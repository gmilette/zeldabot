package bot.state

import nintaco.api.API

data class LinkProjectileStatus(val api: API) {
    val linkState by lazy { (api.readCPU(Addresses.linkDoingAnAttack) and 0xFF) and 0xF0 }
    val linkBusy = linkState == 0x10 || linkState == 0x20

    private val boomerangState by lazy { api.readCPU(Addresses.More.boomerangState) and 0xFF }
    val boomerangReady = !linkBusy && boomerangState == 0
    val boomerangInFlight = boomerangState != 0
    val boomerangReturning = boomerangState and 0xF0 in setOf(0x20, 0x30, 0x40, 0x50)

    private val weaponState by lazy { api.readCPU(Addresses.More.weaponState) and 0xFF }
    val arrowInFlight = weaponState and 0xF0 in setOf(0x10, 0x20)
    val wandProjectileActive = weaponState and 0xF0 == 0x30
    val weaponReady = !linkBusy && weaponState == 0

    fun status(): String = "boomerang: $boomerangReady, weapon: $weaponReady arrow: $arrowInFlight, wand: $wandProjectileActive"
}