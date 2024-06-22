package bot.state

import bot.state.oam.OamStateReasoner
import bot.state.oam.SpriteData
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.kotlin.mock
import util.d


class OamStateReasonerTest {
    @Test
    fun `test keep`() {
        val reasoner = OamStateReasoner(mock(), mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(85, 102), 176, 3),
            SpriteData(178, FramePoint(77, 118), 178, 3)
        )
        reasoner.combine(sprites).apply {
            size shouldBe 2
        }
    }

    @Test
    fun `test some`() {
        val reasoner = OamStateReasoner(mock(), mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(128, 55), 176, 3),
            SpriteData(178, FramePoint(136, 55), 178, 3)
        )
        reasoner.combine(sprites).apply {
            size shouldBe 1
            get(0).point.x shouldBe 128
        }
    }

//    =(136, 39), dir=None, state=Alive, countDown=0, hp=168, projectileState=NotProjectile, droppedId=66)
//    Debug: (Kermit)  enemy Agent(index=170, point=(128, 39)
@Test
fun `test some more`() {
    val reasoner = OamStateReasoner(mock(), mock())
    val sprites = listOf(
        SpriteData(176, FramePoint(136, 39), 176, 3),
        SpriteData(178, FramePoint(128, 39), 178, 3)
    )
    reasoner.combine(sprites).apply {
        size shouldBe 1
        get(0).point.x shouldBe 128
    }
}
    @Test
    fun `test delete skeli`() {
        val reasoner = OamStateReasoner(mock(), mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(56, 90), 168, 3),
            SpriteData(178, FramePoint(48, 90), 170, 3)
        )
        reasoner.combine(sprites).apply {
            size shouldBe 1
            get(0).point.x shouldBe 48
        }
    }

    @Test
    fun `test delete samex`() {
        val reasoner = OamStateReasoner(mock(), mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(141, 64), 168, 3),
            SpriteData(176, FramePoint(149, 64), 168, 3),
            SpriteData(176, FramePoint(141, 32), 168, 3),
            SpriteData(178, FramePoint(149, 32), 170, 3)
        )
        reasoner.combine(sprites).apply {
            size shouldBe 2
        }
    }

    @Test
    fun `test snake guy`() {
//        (Kermit) 0: SpriteData(index=27, point=(200, 123), tile=166, attribute=2)
//        Debug: (Kermit) 1: SpriteData(index=28, point=(168, 66), tile=166, attribute=2)
//        Debug: (Kermit) 2: SpriteData(index=29, point=(159, 96), tile=166, attribute=2)
//        Debug: (Kermit) 3: SpriteData(index=30, point=(155, 64), tile=166, attribute=2)
//        Debug: (Kermit) 4: SpriteData(index=31, point=(130, 96), tile=162, attribute=2)
//        Debug: (Kermit) 5: SpriteData(index=44, point=(160, 66), tile=164, attribute=2)
//        Debug: (Kermit) 6: SpriteData(index=45, point=(192, 123), tile=164, attribute=2)
//        Debug: (Kermit) 7: SpriteData(index=50, point=(147, 64), tile=164, attribute=2)
//        Debug: (Kermit) 8: SpriteData(index=51, point=(151, 96), tile=164, attribute=2)
        val reasoner = OamStateReasoner(mock(), mock())
        val sprites = listOf(
            SpriteData(176, FramePoint(200, 123), 166, 2),
            SpriteData(176, FramePoint(168, 66), 166, 2),
            SpriteData(176, FramePoint(159, 96), 166, 2),
            SpriteData(176, FramePoint(155, 64), 166, 2),
            SpriteData(176, FramePoint(130, 96), 162, 2),
            SpriteData(176, FramePoint(160, 66), 166, 2),
            SpriteData(178, FramePoint(192, 123), 164, 2),
            SpriteData(176, FramePoint(147, 64), 166, 2),
            SpriteData(176, FramePoint(151, 96), 166, 2),
        )
        reasoner.combine(sprites).apply {
            size shouldBe 5
//            get(0).point.x shouldBe 192
            for (spriteData in this) {
                d { " $spriteData"}
            }
        }

//        Debug: (Kermit)  SpriteData(index=178, point=(192, 123), tile=164, attribute=2)
//        Debug: (Kermit)  SpriteData(index=176, point=(130, 96), tile=166, attribute=2)
//        Debug: (Kermit)  SpriteData(index=176, point=(160, 66), tile=166, attribute=2)
//        Debug: (Kermit)  SpriteData(index=176, point=(147, 64), tile=166, attribute=2)
//        Debug: (Kermit)  SpriteData(index=176, point=(151, 96), tile=166, attribute=2)
//        Debug: (Kermit)  enemy: Agent(index=166, point=(200, 123), dir=None, state=Alive, countDown=0, hp=166, projectileState=NotProjectile, droppedId=2)
//        Debug: (Kermit)  enemy: Agent(index=164, point=(160, 66), dir=None, state=Alive, countDown=0, hp=164, projectileState=NotProjectile, droppedId=2)
//        Debug: (Kermit)  enemy: Agent(index=164, point=(192, 123), dir=None, state=Alive, countDown=0, hp=164, projectileState=NotProjectile, droppedId=2)
//        Debug: (Kermit)  enemy: Agent(index=164, point=(147, 64), dir=None, state=Alive, countDown=0, hp=164, projectileState=NotProjectile, droppedId=2)
//        Debug: (Kermit)  enemy: Agent(index=164, point=(151, 96), dir=None, state=Alive, countDown=0, hp=164, projectileState=NotProjectile, droppedId=2)
//
    }

}