package bot

import bot.state.Addresses
import nintaco.api.API
import nintaco.api.ApiSource
import nintaco.api.Colors
import util.d

class ZeldahelperBot {
    private val api: API = ApiSource.getAPI()

    fun launch() {
        api.addFrameListener { renderFinished() }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener { apiEnabled() }
        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()
    }

    private fun apiEnabled() {
        d { "Let's begin apiEnabled" }
        val sprite = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        for (y in 0 until SPRITE_SIZE) {
            val Y = y - SPRITE_SIZE / 2 + 0.5
            for (x in 0 until SPRITE_SIZE) {
                val X = x - SPRITE_SIZE / 2 + 0.5
                sprite[SPRITE_SIZE * y + x] = if (X * X + Y * Y
                    <= SPRITE_SIZE * SPRITE_SIZE / 4
                ) Colors.ORANGE else -1
            }
        }
        api.createSprite(
            SPRITE_ID,
            SPRITE_SIZE,
            SPRITE_SIZE, sprite)
        strWidth = api.getStringWidth(STRING, false)
        strX = 0 //(256 - strWidth) / 2
        strY = (240 - 8) - 40  /// 2 // interesting that's size
    }

    private fun apiDisabled() {
        println("API disabled")
    }

    private fun dispose() {
        println("API stopped")
    }

    private fun statusChanged(message: String) {
        System.out.format("Status message: %s%n", message)
    }

    private var strWidth = 0
    private var strX = 0
    private var strY = 200

    private fun drawIt(text: String) {
        api.drawString(text, strX, strY, false)
    }

    private val start = System.currentTimeMillis()

    private fun renderFinished() {
        val minutes = (System.currentTimeMillis() - start) / 1000 / 60
        val tenth = api.readCPU(Addresses.tenthEnemyCount)
        val message = "${minutes}m : ${tenth}b"
        println(message)
        drawIt(message)
    }

    companion object {
        private const val STRING = "####"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 2

        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            ZeldahelperBot().launch()
        }
    }
}

