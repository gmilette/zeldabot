package bot.state

import nintaco.api.API

/** Reads a single CPU byte and interprets it as a signed value (-128..127). */
fun API.readSigned(address: Int): Int {
    val raw = readCPU(address)
    return if (raw > 127) raw - 256 else raw
}
