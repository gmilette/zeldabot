package bot.state.movement

import bot.state.Addresses
import bot.state.GamePad
import bot.state.map.Direction
import bot.state.map.opposite
import bot.state.readSigned
import nintaco.api.API
import util.d
import kotlin.math.abs

fun MovePredictor.LinkState.lookupPrediction(): MovePrediction =
    MovePredictorData.lookup(
        action     = gamePad.toObjInputDir(),
        gridOffset = objGridOffset,
        inputDir   = objInputDir,
        objDir     = objDir,
        posFrac    = objPosFrac,
    )

fun GamePad.toObjInputDir(): Int = when (this) {
    GamePad.MoveRight -> 1
    GamePad.MoveLeft  -> 2
    GamePad.MoveDown  -> 4
    GamePad.MoveUp    -> 8
    else              -> 0
}

class MovePredictor(private val api: API) {
    fun makeState(gamePad: GamePad): LinkState {
        return LinkState(
            gamePad = gamePad,
            shoved = api.readCPU(Addresses.shoveDir) != 0,
            objGridOffset = api.readSigned(Addresses.subTile),
            objPosFrac = api.readCPU(Addresses.subPixel),
            objQSpeedFrac = api.readCPU(Addresses.objQSpeedFrac),
            objInputDir = gamePad.toObjInputDir(),
            objDir = api.readCPU(Addresses.linkDir),
            linkGoStraight = api.readCPU(Addresses.linkGoStraight),
        )
    }

    fun predict(gamePad: GamePad): MovementPrediction {
        val linkState = makeState(gamePad)
        val p = linkState.lookupPrediction()
//        val p: MovePrediction = MovePredictorData.lookup(action = linkState.gamePad.toObjInputDir(),
//            gridOffset = linkState.objGridOffset,
//            inputDir  = gamePad.toObjInputDir(),
//            objDir = linkState.objInputDir,
//            posFrac = linkState.objQSpeedFrac
//        )
        return MovementPrediction(
            linkState,
            pixels = p.dist,
            newObjGridOffset = p.newObjGridOffset,
            newObjPosFrac = p.newObjPosFrac,
        )
//        return predictLinkMovement(linkState)
    }

    data class LinkState(
        val gamePad: GamePad = GamePad.None,
        val shoved: Boolean = false,
        val objGridOffset: Int = 0,    // $0394 // subTile
        val objInputDir: Int = 0,      // $03F8 // objInputDir
        val objDir: Int = 0,           // $0098 // linkDir (NES bitmask)
        val objPosFrac: Int = 0,       // $03A8 // subPixel
        val objQSpeedFrac: Int = 0,    // $03BC // objQSpeedFrac
        val linkGoStraight: Int = 0,   // $0057 // Link_GoStraight flag
    )

    data class MovementPrediction(
        val input: LinkState = LinkState(),
        val direction: Direction = Direction.None,
        val pixels: Int = -1,
        val newObjPosFrac: Int = 0,
        val newObjGridOffset: Int = 0
    )

    /**
     * Simulates Link_HandleInput (Z_05.asm) to produce the ObjDir and ObjGridOffset
     * that Walker_Move will see.
     *
     * Path 1 — Link_ModifyDirAtGridPoint (ObjGridOffset == 0):
     *   Follow ObjInputDir if present; otherwise keep ObjDir if ObjPosFrac != 0
     *   (fractional momentum still in progress), else stop.
     *
     * Path 2 — Link_ModifyDirOnGridLine (ObjGridOffset != 0):
     *   • Same direction as ObjDir   → keep going
     *   • Opposite to ObjDir         → reverse immediately; ObjGridOffset unchanged
     *   • Perpendicular to ObjDir    → canTurn check; if allowed, ObjDir reverses and
     *                                  ObjGridOffset is mirrored: -3→5, 3→-5, etc.
     */
    private fun computeEffectiveDirection(state: LinkState): Pair<Direction, Int> {
        val inputDir = Direction.fromBitmask(state.objInputDir)
        val currentDir = Direction.fromBitmask(state.objDir)

        if (state.objGridOffset == 0) {
            // Path 1: on a grid boundary
            val result = when {
                inputDir != Direction.None -> Pair(inputDir, 0)
                state.objPosFrac != 0     -> Pair(currentDir, 0)  // fractional momentum in progress
                else                      -> Pair(Direction.None, 0)
            }
            d { "computeDir path1: input=$inputDir current=$currentDir posFrac=${state.objPosFrac} -> ${result.first}" }
            return result
        }

        // Path 2: mid-cell
        if (inputDir == Direction.None) {
            d { "computeDir path2: no input, stopping" }
            return Pair(Direction.None, state.objGridOffset)
        }

        return when (inputDir) {
            currentDir -> {
                // Same direction: keep going
                d { "computeDir path2: same dir=$currentDir gridOffset=${state.objGridOffset}" }
                Pair(currentDir, state.objGridOffset)
            }
            currentDir.opposite() -> {
                // Opposite: reverse immediately, ObjGridOffset unchanged
                d { "computeDir path2: opposite $currentDir -> $inputDir gridOffset=${state.objGridOffset}" }
                Pair(inputDir, state.objGridOffset)
            }
            else -> {
                // Perpendicular: NES canTurn check
                // Left ($02) and Up ($08) have bit 0x0A set — they produce negative offsets
                val offsetSignMatchesDir = if ((state.objDir and 0x0A) != 0) {
                    state.objGridOffset < 0
                } else {
                    state.objGridOffset > 0
                }
                val canTurn = state.linkGoStraight == 0
                        && abs(state.objGridOffset) < 4
                        && offsetSignMatchesDir

                if (canTurn) {
                    // ObjDir reverses; ObjGridOffset is mirrored toward the previous grid point
                    val newGridOffset = if (state.objGridOffset < 0) {
                        8 + state.objGridOffset   // e.g. -3 → 5
                    } else {
                        -8 + state.objGridOffset  // e.g.  3 → -5
                    }
                    d { "computeDir path2: perpendicular canTurn $currentDir -> ${currentDir.opposite()} gridOffset ${state.objGridOffset} -> $newGridOffset" }
                    Pair(currentDir.opposite(), newGridOffset)
                } else {
                    d { "computeDir path2: perpendicular cannotTurn, keep $currentDir (straight=${state.linkGoStraight} absOffset=${abs(state.objGridOffset)} signMatch=$offsetSignMatchesDir)" }
                    Pair(currentDir, state.objGridOffset)
                }
            }
        }
    }

    fun predictLinkMovement(state: LinkState): MovementPrediction {
        val positiveLimit = 8
        val negativeLimit = -8

        d { "predictLinkMovement IN: pad=${state.gamePad} gridOffset=${state.objGridOffset} posFrac=${state.objPosFrac} objDir=${state.objDir} inputDir=${state.objInputDir} speed=${state.objQSpeedFrac} straight=${state.linkGoStraight}" }

        val (direction, startingGridOffset) = computeEffectiveDirection(state)

        d { "predictLinkMovement: pad=${state.gamePad} gridOffset=${state.objGridOffset} objDir=${state.objDir} -> dir=$direction startOffset=$startingGridOffset" }

        if (direction == Direction.None) {
            return MovementPrediction(state, Direction.None, 0, state.objPosFrac, state.objGridOffset)
        }

        val isPositive = direction == Direction.Right || direction == Direction.Down

        var posFrac = state.objPosFrac
        var gridOffset = startingGridOffset
        var pixels = 0

        repeat(4) {
            if (isPositive) {
                val sum = posFrac + state.objQSpeedFrac
                posFrac = sum and 0xFF
                val carry = if (sum > 0xFF) 1 else 0
                d { "predictLinkMovement: sum=$sum carry=$carry posFrac=$posFrac" }
                if (gridOffset != positiveLimit && gridOffset != negativeLimit) {
                    gridOffset += carry
                    pixels += carry
                }
            } else {
                val diff = posFrac - state.objQSpeedFrac
                posFrac = diff and 0xFF
                val borrow = if (diff < 0) 1 else 0
                d { "predictLinkMovement: diff=$diff borrow=$borrow posFrac=$posFrac" }
                if (gridOffset != positiveLimit && gridOffset != negativeLimit) {
                    gridOffset -= borrow
                    pixels += borrow
                }
            }
        }

        d { "predictLinkMovement OUT: dir=$direction pixels=$pixels posFrac=$posFrac gridOffset=$gridOffset" }
        return MovementPrediction(state, direction, pixels, posFrac, gridOffset)
    }
}
