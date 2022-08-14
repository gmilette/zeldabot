package bot

import co.touchlab.kermit.Logger
import nintaco.api.*
import util.d

class HelloWorld {
    private val api: API = ApiSource.getAPI()
    private var spriteX = 0
    private var spriteY = 8
    private var spriteVx = 1
    private var spriteVy = 1
    private var strWidth = 0
    private var strX = 0
    private var strY = 0
    fun launch() {
        api.addFrameListener { renderFinished() }
        api.addStatusListener { message: String -> statusChanged(message) }
        api.addActivateListener { apiEnabled() }
        api.addDeactivateListener { apiDisabled() }
        api.addStopListener { dispose() }
        api.run()
    }

    private fun apiEnabled() {
        d { "Hello d"}
        println("API enabled GREG kotlin")
        Logger.d { "Hello from logger"}
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
        api.createSprite(SPRITE_ID, SPRITE_SIZE, SPRITE_SIZE, sprite)
        strWidth = api.getStringWidth(STRING, false)
        strX = (256 - strWidth) / 2
        strY = (240 - 8) / 2
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

    private fun renderFinished() {
        api.drawSprite(SPRITE_ID, spriteX, spriteY)
        if (spriteX + SPRITE_SIZE == 255) {
            spriteVx = -1
        } else if (spriteX == 0) {
            spriteVx = 1
        }
        if (spriteY + SPRITE_SIZE == 231) {
            spriteVy = -1
        } else if (spriteY == 8) {
            spriteVy = 1
        }
        spriteX += spriteVx
        spriteY += spriteVy
        api.setColor(Colors.DARK_BLUE)
        api.fillRect(strX - 1, strY - 1, strWidth + 2, 9)
        api.setColor(Colors.BLUE)
        api.drawRect(strX - 2, strY - 2, strWidth + 3, 10)
        api.setColor(Colors.WHITE)
        api.drawString(STRING, strX, strY, false)
    }

    companion object {
        private const val STRING = "Hello, World! KOTLIN yo"
        private const val SPRITE_ID = 123
        private const val SPRITE_SIZE = 32
        @JvmStatic
        fun main(args: Array<String>) {
            ApiSource.initRemoteAPI("localhost", 9999)
            HelloWorld().launch()
        }
    }
}