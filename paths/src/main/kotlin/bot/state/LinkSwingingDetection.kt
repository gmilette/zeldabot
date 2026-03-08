package bot.state

import nintaco.api.API

object LinkSwingingDetection {
    fun attacking(api: API): Boolean {
        val attacking = api.readCPU(Addresses.linkDoingAnAttack)
        val sword = api.readCPU(Addresses.More.swordState)
//        d { " attacking $attacking $sword" }
        return !(attacking == 0 && sword == 0)
    }

    fun notAttacking(api: API): Boolean {
        val attacking = api.readCPU(Addresses.linkDoingAnAttack)
        val sword = api.readCPU(Addresses.More.swordState)
        return (attacking == 0 && sword == 0)
    }
}