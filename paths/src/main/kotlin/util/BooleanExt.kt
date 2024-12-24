package util

fun Boolean.ifTrue(message: String): String = if (this) message else ""

fun Boolean.ifFalse(message: String): String = if (this) message else ""
