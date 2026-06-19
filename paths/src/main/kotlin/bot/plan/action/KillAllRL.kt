package bot.plan.action

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import bot.training.KillAllStateEncoder
import bot.state.GamePad
import bot.state.MapLocationState
import util.d
import java.nio.FloatBuffer

/**
 * RL-based KillAll action using a trained ONNX policy.
 * Implements the same [Action] interface as [KillAll].
 *
 * The ONNX model is loaded lazily on first use.
 * Until a model is trained, this acts as a random policy fallback.
 */
class KillAllRL(private val modelPath: String = "killall_policy.onnx") : Action {

    private var episodeFrameCount = 0

    private val ACTION_MAP = arrayOf(
        GamePad.None,
        GamePad.MoveRight,
        GamePad.MoveLeft,
        GamePad.MoveDown,
        GamePad.MoveUp,
        GamePad.A,
        GamePad.B
    )

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    private val session: OrtSession? by lazy {
        try {
            env.createSession(modelPath)
        } catch (e: Exception) {
            d { "KillAllRL: failed to load ONNX model from $modelPath — ${e.message}" }
            null
        }
    }

    override fun reset() {
        super.reset()
        episodeFrameCount = 0
    }

    override fun complete(state: MapLocationState): Boolean = state.cleared

    override fun nextStep(state: MapLocationState): GamePad {
        d { "KillAllRL nextStep" }
        episodeFrameCount++
        val obs = KillAllStateEncoder.encode(state, episodeFrameCount)
        val sess = session
        if (sess == null) {
            // No model loaded — random policy for exploration
            return ACTION_MAP.random()
        }

        return try {
            val inputName = sess.inputNames.first()
            val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(obs), longArrayOf(1, obs.size.toLong()))
            val result = sess.run(mapOf(inputName to tensor))
            val output = result[0].value

            val actionIdx = when (output) {
                is Array<*> -> (output[0] as LongArray)[0].toInt()
                is LongArray -> output[0].toInt()
                else -> 0
            }

            tensor.close()
            result.close()

            ACTION_MAP.getOrElse(actionIdx) { GamePad.None }
        } catch (e: Exception) {
            d { "KillAllRL: inference error — ${e.message}" }
            GamePad.None
        }
    }

    override val name: String
        get() = "KillAllRL(ep=$episodeFrameCount)"
}
