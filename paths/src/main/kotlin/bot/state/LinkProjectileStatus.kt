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
    val wandProjectileInFlight = weaponState and 0xF0 == 0x30
    val weaponInFlight = weaponState != 0
    val weaponReady = !linkBusy && weaponState == 0

    /**
     * $00BA = 0x00  No projectile active
     * $00BA = 0x10  Sword beam active and flying
     * $00BA = 0x01  Sword beam spreading out on impact
     * $00BA = 0x80  Magic rod shot active and flying
     */
    private val beamState by lazy { api.readCPU(Addresses.More.beamState) and 0xFF }
    val swordBeamActive = beamState == 0x10 || beamState == 0x01
    val magicShotActive = beamState and 0x80 != 0
    val beamInUse = beamState != 0
    val swordIsFlying = beamInUse

    fun status(): String = "beam: $beamInUse boomerang: $boomerangReady, weapon: $weaponReady arrow: $arrowInFlight, wand: $wandProjectileInFlight"
}