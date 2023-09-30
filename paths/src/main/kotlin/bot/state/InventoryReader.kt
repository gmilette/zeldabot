package bot.state

import nintaco.api.API
import bot.state.map.destination.ZeldaItem
import util.d

class InventoryItems(api: API) {
    val hasRing by lazy { api.readCPU(api.readCPU(Addresses.hasRing)) }
}

object InventoryReader {
//    val sword by lazy { api.readCPU(Addresses.hasSword) }

    fun readInventory(api: API): Set<ZeldaItem> {
        val items = mutableSetOf<ZeldaItem>()
        when (api.readCPU(Addresses.hasSword)) {
            1 -> items.add(ZeldaItem.WoodenSword)
            2 -> items.add(ZeldaItem.WhiteSword)
            3 -> items.add(ZeldaItem.MagicSword)
            else -> {}
        }
        when (api.readCPU(Addresses.hasCandle)) {
            1 -> items.add(ZeldaItem.RedCandle)
            2 -> items.add(ZeldaItem.BlueCandle)
        }
        if (api.readCpuB(Addresses.hasBow)) items.add(ZeldaItem.Bow)
        if (api.readCpuB(Addresses.hasBook)) items.add(ZeldaItem.BookOfMagic)
        if (api.readCpuB(Addresses.hasBoomerang)) items.add(ZeldaItem.Boomerang)
        if (api.readCpuB(Addresses.hasFood)) items.add(ZeldaItem.Food)
        if (api.readCpuB(Addresses.hasBracelet)) items.add(ZeldaItem.PowerBracelet)
        if (api.readCpuB(Addresses.hasLadder)) items.add(ZeldaItem.Ladder)
        if (api.readCpuB(Addresses.hasLetter)) items.add(ZeldaItem.Letter)
        if (api.readCpuB(Addresses.hasMagicBoomerang)) items.add(ZeldaItem.MagicalBoomerang)
        when (api.readCPU(Addresses.hasArrow)) {
            1 -> items.add(ZeldaItem.Arrow)
            2 -> items.add(ZeldaItem.SilverArrow)
            else -> {}
        }
        when (api.readCPU(Addresses.hasPotion)) {
            1 -> items.add(ZeldaItem.Potion)
            2 -> items.add(ZeldaItem.SecondPotion)
            else -> {}
        }
        if (api.readCpuB(Addresses.hasPotion)) items.add(ZeldaItem.Potion)
        if (api.readCpuB(Addresses.hasRaft)) items.add(ZeldaItem.Raft)
        when (api.readCPU(Addresses.hasRing)) {
            1 -> items.add(ZeldaItem.BlueRing)
            2 -> items.add(ZeldaItem.RedRing)
            else -> {}
        } // todo
        if (api.readCpuB(Addresses.hasShield)) items.add(ZeldaItem.MagicShield)
        if (api.readCpuB(Addresses.hasWhistle)) items.add(ZeldaItem.Whistle)
        if (api.readCpuB(Addresses.hasRod)) items.add(ZeldaItem.Wand)
        d { " items in inventory: $items"}
        return items
    }

    private fun API.readCpuB(address: Int): Boolean =
        readCPU(address) != 0
}