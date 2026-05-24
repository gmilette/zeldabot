package bot.state

import bot.state.SecretTriggeredDetector.SecretTrigger.entries
import nintaco.api.API
import util.d

class SecretTriggeredDetector(private val api: API, private val mapLoc: MapLoc) {
    val pushBlockTypeValue = 0x68

    /**
     * gets updated with its current position after pushing
     */
    val pushBlockPoint by lazy {
        FramePoint(api.readCPU(Addresses.pushBlockX), api.readCPU(Addresses.pushBlockY))
    }
    val pushBlockType by lazy { api.readCPU(Addresses.More.pushBlockType) and 0xFF }
    fun hasPushBlock() = pushBlockType == pushBlockTypeValue

    // !! this is probably the only one the code needs to check
    // Trigger 5: pushable block fully pushed — spawns staircase
    // BlockPushComplete = 1 means pushed but staircase not yet spawned
    // BlockPushComplete = 2 means staircase already spawned
//    fun checkBlockStairs(): Boolean {
//        return secretTrigger == SecretTrigger.BlockStairs && (blockPushed == 1 || blockPushed == 2)
//    }
//    // Trigger 4: pushable block fully pushed — opens shutter doors
//    fun checkBlockDoor(): Boolean {
//        return secretTrigger == SecretTrigger.BlockDoor && blockPushed != 0
//    }
    fun pushedDoorAndRevealedStairs(): Boolean {
        return hasPushBlock() && (blockPushed == 1 || blockPushed == 2)
    }
    // Trigger 4: pushable block fully pushed — opens shutter doors
    fun pushedDoor(): Boolean {
        return hasPushBlock() && blockPushed != 0
    }

    fun hasBeenPushed(): Boolean = pushedDoor() || pushedDoorAndRevealedStairs()
    fun log() = d { "secret trigger $secretTrigger checkBlockDoorOrStairs: ${hasBeenPushed()} has pushed ${hasPushBlock()} block pushed $blockPushed check door ${pushedDoor()} check stair ${pushedDoorAndRevealedStairs()} pt ${pushBlockPoint}" }

    /**
     * tell you what kind of room trigger there is
     */
    val secretTrigger: SecretTrigger by lazy {
        // 0x04CD
        val raw = api.readCPU(Addresses.More.secretTrigger) and 0x07
        entries.first { it.value == raw }
    }

    val blockPushed by lazy {
        // 0x04CF
        api.readCPU(Addresses.More.blockPushed) and 0xFF
    }

    // not strue
    val canTriggerSecret = secretTrigger != SecretTrigger.None
    val secretTriggered by lazy {
        // 0x04CE
        api.readCPU(Addresses.More.secretTriggered) and 0xFF != 0
    }


    fun checkTriggered(): Boolean =
        when (secretTrigger) {
            SecretTrigger.None           -> false
            SecretTrigger.AllDead        -> secretTriggered
            SecretTrigger.Ringleader     -> secretTriggered
            SecretTrigger.LastBoss       -> secretTriggered
            SecretTrigger.BlockDoor      -> pushedDoor()
            SecretTrigger.BlockStairs    -> pushedDoorAndRevealedStairs()
            SecretTrigger.MoneyOrLife    -> secretTriggered
            SecretTrigger.AllDeadForItem -> secretTriggered
        }

    /**
     * Tells you what
     * 0 — None
     * No secret in this room. Nothing happens.
     * 1 — All Dead
     * Triggers when RoomAllDead is set (all non-Bubble enemies are gone). Opens the shutter doors. Used for standard rooms where you must clear all enemies to proceed.
     * 2 — Ringleader
     * Slot 1 enemy is the leader. When it dies, all other enemies instantly die. Then opens shutter doors. Used for rooms where one specific enemy is the "boss" of the group.
     * 3 — Last Boss
     * Checks LastBossDefeated flag. Triggers when the dungeon boss (Aquamentus, Dodongo, etc.) is killed. Opens shutter doors to the Triforce room.
     * 4 — Block Door
     * Checks BlockPushComplete. Triggers when Link pushes the moveable block all the way. Opens shutter doors. The classic "push the block to open the door" puzzle.
     * 5 — Block Stairs
     * Same block push check as 4, but instead of opening doors it spawns a staircase tile at position ($D0, $60) in the room. Used for secret staircases hidden under pushable blocks.
     * 6 — Money or Life
     * Triggers when the old man in slot 1 is gone (ObjType+1 == 0). This is the room where the old man demands rupees or takes a heart container. Once he leaves, the shutters open.
     * 7 — All Dead for Item
     * Same as 1 (all dead), but additionally activates a hidden room item once triggered. Used for rooms where clearing enemies reveals a specific item.
     */
    enum class SecretTrigger(val value: Int) {
        None          (0),
        AllDead       (1),
        Ringleader    (2),
        LastBoss      (3),
        BlockDoor     (4),
        BlockStairs   (5),
        MoneyOrLife   (6),
        AllDeadForItem(7);
    }

    // candle burns, bombs, and push block staircases
    private val isOverworldSecretFound: Boolean by lazy {
        calculateIsOverworldSecretFound()
    }

    private fun calculateIsOverworldSecretFound(): Boolean {
        val worldFlagsAddrLo = api.readCPU(Addresses.worldFlagsAddrLo) and 0xFF
        val worldFlagsAddrHi = api.readCPU(Addresses.worldFlagsAddrHi) and 0xFF
        val worldFlagsAddr   = worldFlagsAddrLo or (worldFlagsAddrHi shl 8)
        val roomFlags        = api.readCPU(worldFlagsAddr + mapLoc) and 0xFF
        return (roomFlags and 0x80) != 0
    }
}