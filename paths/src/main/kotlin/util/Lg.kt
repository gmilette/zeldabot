package util

import bot.ZeldaBot
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag

object LoggerOverride {
    val log: Boolean
        get() = ZeldaBot.log

    private val allowed = mutableListOf<String>("")
    private val denied = mutableListOf<String>("")

    /** Only show logs from these packages (prefix match). If empty, all packages are shown. */
    fun allow(vararg packages: String) = apply { allowed.addAll(packages) }

    /** Hide logs from these packages (prefix match). */
    fun deny(vararg packages: String) = apply { denied.addAll(packages) }

    fun init() {
        Logger.setLogWriters(FilteringLogWriter(allowed, denied, CommonWriter(ZFormatter())))
    }
}

private fun callerPackage(): String =
    Thread.currentThread().stackTrace
        .map { it.className }
        .firstOrNull { cls ->
            !cls.startsWith("co.touchlab.kermit") &&
            !cls.startsWith("util.") &&
            !cls.startsWith("java.") &&
            !cls.startsWith("sun.") &&
            !cls.startsWith("kotlin.")
        }
        ?.substringBefore('$')
        ?: ""

private class FilteringLogWriter(
    private val allowed: List<String>,
    private val denied: List<String>,
    private val delegate: LogWriter,
) : LogWriter() {
    override fun isLoggable(tag: String, severity: Severity): Boolean {
        val pkg = callerPackage()
        if (denied.isNotEmpty() && denied.any { pkg.startsWith(it) }) return false
        if (allowed.isNotEmpty() && allowed.none { pkg.startsWith(it) }) return false
        return true
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) =
        delegate.log(severity, message, tag, throwable)
}

private class ZFormatter : MessageStringFormatter {
    override fun formatMessage(severity: Severity?, tag: Tag?, message: Message): String {
        val pkg = callerPackage()
        val derivedTag = if (pkg.isNotEmpty()) Tag(pkg) else tag
        return super.formatMessage(severity, derivedTag, message)
    }

    override fun formatSeverity(severity: Severity): String = ""
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
