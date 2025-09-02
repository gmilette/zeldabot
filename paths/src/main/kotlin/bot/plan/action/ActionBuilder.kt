package bot.plan.action

class ActionBuilder {
    // conditions
    val onlyIf = mutableListOf<String>()

    fun buildOne() {
        val action = KillAll()

    }
}

/***
 * ACTION if preconditions
 * THEN ACTION
 * doUntil (conditions)
 * {
 *  ACTION completeif (conditions)
 *  ACTION with (modifiers)
 *  ACTION with timeout(30)
 *  ACTION with once
 *  EITHER ACTION if (conditions) or ACTION2 if (conditions)
 * }
 * ActionSequenceBuilder.build {
 *  KillAll if hasBombs and wait20
 *  InsideNav then Wait20 then Wait20 if needBombs
 */

class ActionSequenceBuilder() {
    val actions = mutableListOf<Action>()

    fun then(action: Action): ActionSequenceBuilder {
        return this
    }
}