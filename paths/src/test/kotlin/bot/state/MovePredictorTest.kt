package bot.state

import bot.ZeldaBot
import bot.state.map.Direction
import bot.state.movement.MovePredictor
import bot.state.movement.toObjInputDir
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MovePredictorIntegrationTest {
    /** Mocks the API so predict() reads controlled values and runs the full pipeline. */
    @Test
    fun `predict MoveRight with gridOffset 0 moves pixels`() {
        val api = mock<nintaco.api.API>()
        whenever(api.readCPU(Addresses.subTile)).thenReturn(0)       // signed → gridOffset = 0
        whenever(api.readCPU(Addresses.subPixel)).thenReturn(0)      // objPosFrac = 0
        whenever(api.readCPU(Addresses.objQSpeedFrac)).thenReturn(0x60) // speed = 96
        whenever(api.readCPU(Addresses.objInputDir)).thenReturn(0)   // overridden by gamePad
        whenever(api.readCPU(Addresses.linkDir)).thenReturn(1)       // objDir = Right

        val result = MovePredictor(api).predict(GamePad.MoveRight)

        result.direction shouldBe Direction.Right
        // speed=96: iters accumulate 96→192→288(carry)→128 — one carry in 4 iters
        result.pixels shouldBe 1
    }
}

class GamePadToObjInputDirTest {
    @Test fun `MoveRight maps to bitmask 1`()  { GamePad.MoveRight.toObjInputDir() shouldBe 1 }
    @Test fun `MoveLeft maps to bitmask 2`()   { GamePad.MoveLeft.toObjInputDir()  shouldBe 2 }
    @Test fun `MoveDown maps to bitmask 4`()   { GamePad.MoveDown.toObjInputDir()  shouldBe 4 }
    @Test fun `MoveUp maps to bitmask 8`()     { GamePad.MoveUp.toObjInputDir()    shouldBe 8 }
    @Test fun `None maps to bitmask 0`()       { GamePad.None.toObjInputDir()      shouldBe 0 }
}

class MovePredictorTest {

    /**
     * Runs predictLinkMovement for 12 consecutive frames, feeding each frame's
     * newObjPosFrac and newObjGridOffset back as the next frame's input.
     *
     * With objQSpeedFrac=0x80 and 4 iterations per frame, the NES adds 0x80 twice
     * per frame (iters 1 and 3 carry, iters 2 and 4 do not), producing exactly 2 pixels
     * per frame while gridOffset is not clamped.  Once gridOffset reaches -8 (negativeLimit),
     * no further movement occurs — but posFrac must keep updating unconditionally so that
     * the internal state does not drift and produce a spurious 6-frame cycle when the limit
     * is later released.
     */
    @Test
    fun `predictLinkMovement 12 frames UP no 6-frame repeating error`() {
        val api = mock<nintaco.api.API>()
        val predictor = MovePredictor(api)

        var posFrac = 0x00
        var gridOffset = 0
        val objQSpeedFrac = 0x80
        val pixelsPerFrame = mutableListOf<Int>()

        repeat(12) { frameIndex ->
            val state = MovePredictor.LinkState(
                objQSpeedFrac = objQSpeedFrac,
                objPosFrac = posFrac,
                objGridOffset = gridOffset,
                objInputDir = 0x08,   // UP bitmask — direction derived inside predictLinkMovement
                objDir = 0x08,
            )
            val result = predictor.predictLinkMovement(state)
            println(
                "Frame ${frameIndex + 1}: pixels=${result.pixels}" +
                "  posFrac=0x${result.newObjPosFrac.toString(16).uppercase()}" +
                "  gridOffset=${result.newObjGridOffset}"
            )
            pixelsPerFrame.add(result.pixels)
            posFrac = result.newObjPosFrac
            gridOffset = result.newObjGridOffset
        }

        // With 0x80 speed, each frame produces exactly 2 carry events (iters 1 and 3)
        // until gridOffset is clamped at -8.  Frames 1-4 should each move 2 pixels.
        pixelsPerFrame[0] shouldBe 2
        pixelsPerFrame[1] shouldBe 2
        pixelsPerFrame[2] shouldBe 2
        pixelsPerFrame[3] shouldBe 2

        // Frame 4 brings gridOffset to -8 (negativeLimit).  From frame 5 onward the
        // clamping branch suppresses gridOffset / pixel updates.  If posFrac were NOT
        // updated unconditionally, the fractional accumulator would freeze and later
        // produce a wrong carry pattern on a 6-frame cycle.  With the correct
        // implementation posFrac continues cycling 0→128→0→… and every frame returns 0.
        for (frame in 4..11) {
            pixelsPerFrame[frame] shouldBe 0
        }

        // posFrac must settle back to 0x00 after each frame (0x80*4 iters = 2 full wraps).
        posFrac shouldBe 0x00

        // gridOffset must remain at the negative limit.
        gridOffset shouldBe -8
    }

    /**
     * Verifies that objGridOffset is treated as a signed byte.
     *
     * NES memory value 249 (0xF9) represents -7 in two's-complement.  With the unsigned
     * bug the value 249 would not be near either limit, so both iteration 1 and iteration 3
     * would produce a borrow and the result would be 2 pixels.  With the correct signed
     * read, -7 is one step from the -8 clamp: only iteration 1 fires before the clamp is
     * hit, so exactly 1 pixel is returned.
     */
    @Test
    fun `predictLinkMovement with objGridOffset minus7 produces 1 pixel not 2`() {
        ZeldaBot.log = true
        val api = mock<nintaco.api.API>()
        val predictor = MovePredictor(api)

        val state = MovePredictor.LinkState(
            gamePad = GamePad.MoveRight,
            objGridOffset = -7,      // signed: one step from the -8 negative limit
            objPosFrac = 0,
            objQSpeedFrac = 96,      // 0x60
            objInputDir = 0x08,      // UP bitmask
            objDir = 0x08,
        )
        val result = predictor.predictLinkMovement(state)

        // Only iter 1 produces a borrow (0 - 96 < 0) and gridOffset moves -7 → -8.
        // Iters 2-4 are clamped.  Total: 1 pixel.
        result.pixels shouldBe 1
        result.newObjGridOffset shouldBe -8
    }

    /**
     * Perpendicular input mid-cell with canTurn true: ObjDir reverses and
     * ObjGridOffset is mirrored.  Taken from a real observed failure where the
     * predictor incorrectly predicted 2 pixels Up instead of 1 pixel Down.
     *
     * Setup: moving Up (objDir=8), gridOffset=-3, press MoveLeft (perpendicular).
     * canTurn: abs(-3)=3 < 4 ✓, objDir=8 has bit 0x0A set so need offset<0 ✓, linkGoStraight=0 ✓
     * Result: ObjDir→Down, gridOffset = 8+(-3) = 5.
     * Walker_Move (Down, positive, speed=96, posFrac=0, gridOffset=5):
     *   96 → 192 → 288 carry (gridOffset=6, pixels=1) → 32
     */
    @Test
    fun `perpendicular input canTurn reverses direction and mirrors gridOffset`() {
        val api = mock<nintaco.api.API>()
        val predictor = MovePredictor(api)

        val state = MovePredictor.LinkState(
            gamePad = GamePad.MoveLeft,
            objGridOffset = -3,
            objInputDir = 2,          // Left bitmask
            objDir = 8,               // Up bitmask
            objPosFrac = 0,
            objQSpeedFrac = 96,
            linkGoStraight = 0,
        )
        val result = predictor.predictLinkMovement(state)

        result.direction shouldBe Direction.Down
        result.pixels shouldBe 1
        result.newObjGridOffset shouldBe 6
        result.newObjPosFrac shouldBe 128
    }

    /**
     * When objGridOffset != 0 and objInputDir == 0, direction must be NONE
     * regardless of objDir — so no movement occurs.
     */
    @Test
    fun `predictLinkMovement gridOffset nonzero inputDir zero yields no movement`() {
        val api = mock<nintaco.api.API>()
        val predictor = MovePredictor(api)

        val state = MovePredictor.LinkState(
            objGridOffset = 3,
            objInputDir = 0,
            objDir = 0x01,   // Right bitmask — must be ignored because inputDir == 0
            objPosFrac = 0,
            objQSpeedFrac = 96,
        )
        val result = predictor.predictLinkMovement(state)

        result.pixels shouldBe 0
        result.direction shouldBe Direction.None
    }

    /**
     * When objGridOffset == 0 and objInputDir == 0, direction must be NONE
     * and no movement occurs.
     */
    @Test
    fun `predictLinkMovement gridOffset zero inputDir zero yields no movement`() {
        val api = mock<nintaco.api.API>()
        val predictor = MovePredictor(api)

        val state = MovePredictor.LinkState(
            objGridOffset = 0,
            objInputDir = 0,
            objDir = 0x01,   // Right bitmask — must be ignored because inputDir == 0
            objPosFrac = 0,
            objQSpeedFrac = 96,
        )
        val result = predictor.predictLinkMovement(state)

        result.pixels shouldBe 0
        result.direction shouldBe Direction.None
    }
}
