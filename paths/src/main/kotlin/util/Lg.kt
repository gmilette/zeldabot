package util

import bot.ZeldaBot
import co.touchlab.kermit.Logger

object LoggerOverride {
    val log: Boolean
        get() = ZeldaBot.log
}

inline fun v(message: () -> String) = log { Logger.v(message()) }

inline fun d(t: Throwable? = null, message: () -> String) = log { Logger.d( message(), t) }

inline fun i(t: Throwable? = null, message: () -> String) = log { Logger.i(message(), t) }

inline fun w(t: Throwable? = null, message: () -> String) = log { Logger.w(message(), t) }

inline fun e(t: Throwable? = null, message: () -> String) = log { Logger.e( message(), t) }

/** @suppress */
@PublishedApi
internal inline fun log(block: () -> Unit) {
    if (LoggerOverride.log) {
        block()
    }
}
