package util

import bot.ZeldaBot
import co.touchlab.kermit.Logger

object LoggerOverride {
    val log: Boolean
        get() = ZeldaBot.log
}

inline fun v(message: () -> String) = log { Logger.v(message()) }

inline fun d(t: Throwable? = null, message: () -> String) = log { Logger.d( message()) }

inline fun i(t: Throwable? = null, message: () -> String) = log { Logger.i(message()) }

inline fun w(t: Throwable? = null, message: () -> String) = log { Logger.w(message()) }

inline fun e(t: Throwable? = null, message: () -> String) = log { Logger.e( message()) }

/** @suppress */
@PublishedApi
internal inline fun log(block: () -> Unit) {
    if (LoggerOverride.log) {
        block()
    }
}
