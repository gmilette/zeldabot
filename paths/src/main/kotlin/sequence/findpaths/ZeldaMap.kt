package sequence.findpaths

sealed class ZeldaMapCode(val code: String) {
    object Ground: ZeldaMapCode("02")
    object Cave: ZeldaMapCode("12")
    object Bridge: ZeldaMapCode("91")
    object Dock: ZeldaMapCode("97")
    object PathBridge: ZeldaMapCode("1a")
    object PathGraveyard: ZeldaMapCode("0e")
    object PathBridgeGraveyard: ZeldaMapCode("14")
    object PathBridgeGraveyardGrave: ZeldaMapCode("40")
    object GraveYardBridge: ZeldaMapCode("20")
}