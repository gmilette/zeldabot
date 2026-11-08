import bot.plan.action.IsGoal
import bot.plan.action.RouteTo
import bot.plan.action.routeto.PointMoveAction
import bot.plan.action.routeto.RouteExecution
import bot.plan.action.routeto.RouteToGetInFrontOf
import bot.state.Addresses
import bot.state.Agent
import bot.state.EnemyState
import bot.state.FramePoint
import bot.state.FrameState
import bot.state.Inventory
import bot.state.MapLocationState
import bot.state.map.Direction
import bot.state.map.Hyrule
import bot.state.oam.swordDir
import nintaco.api.API
import java.lang.reflect.Proxy

// nintaco.api.API has 166 methods and mockito is test only, so the stub is a proxy. Reads default
// to zero, which is what the goal chain needs: swordUseCountdown and swordBlocked zero make
// canUseSword true, and linkDoingAnAttack zero makes link not mid-swing. Zero is NOT neutral for
// hearts though - it reads as one container, one filled, so link is at full health and the sword
// beam is always available, which makes every square on the enemy's row a goal.
private fun stubApi(reads: Map<Int, Int> = emptyMap()): API =
    Proxy.newProxyInstance(API::class.java.classLoader, arrayOf(API::class.java)) { _, method, args ->
        val address = args?.firstOrNull() as? Int
        val canned = address?.let { reads[it] }
        if (canned != null && method.returnType != Boolean::class.javaPrimitiveType) canned
        else if (canned != null) canned != 0
        else when (method.returnType) {
            Boolean::class.javaPrimitiveType, java.lang.Boolean::class.java -> false
            Int::class.javaPrimitiveType, Integer::class.java -> 0
            Long::class.javaPrimitiveType, java.lang.Long::class.java -> 0L
            Short::class.javaPrimitiveType, java.lang.Short::class.java -> 0.toShort()
            Byte::class.javaPrimitiveType, java.lang.Byte::class.java -> 0.toByte()
            Float::class.javaPrimitiveType, java.lang.Float::class.java -> 0f
            Double::class.javaPrimitiveType, java.lang.Double::class.java -> 0.0
            String::class.java -> ""
            IntArray::class.java -> IntArray(0)
            else -> null
        }
    } as API

object RoutingTestGoal {
    // reads mapTile5.csv and builds every overworld cell, far too slow to redo per search
    private val hyrule: Hyrule by lazy { Hyrule() }

    // three containers with one filled: link is alive but not at full health, so the sword beam is
    // off. The UI's ableToLongAttack turns it back on by leaving the byte at zero (full health).
    private const val notFullHealth = 0x21

    // a sword enemy's FACING is its tile, not its dir field, so swordDir.dirFront has to agree with
    // where the enemy was placed. 0xbe and 0xb8 are the direction-unambiguous picks: 0xba and 0xbc
    // appear in both left and right, and dirFront checks left first.
    private val swordTile = mapOf(
        Direction.Up to 0xb4,
        Direction.Down to 0xac,
        Direction.Left to 0xbe,
        Direction.Right to 0xb8
    )

    // has sword carriers and is not in levelsWithNotSword, so AttackableDecider applies the strict
    // "absolutely don't attack front" rule rather than the looser wizzrobe one
    const val swordLevel = 8

    fun syntheticState(
        start: FramePoint,
        startDirection: Direction,
        enemies: List<FramePoint>,
        isLevel: Boolean,
        mapLoc: Int,
        fullHealth: Boolean = false,
        swordEnemies: Boolean = false
    ): MapLocationState {
        val api = stubApi(
            if (fullHealth) emptyMap() else mapOf(Addresses.heartContainers to notFullHealth)
        )
        val agents = enemies.mapIndexed { index, enemy ->
            // Direction.None is a real enum value, not null, so it needs a real fallback here: a
            // sword enemy has to face somewhere or swordDir cannot classify its tile at all
            val placed = enemy.direction ?: Direction.None
            val tile = if (swordEnemies) swordTile[placed] ?: swordTile.getValue(Direction.Down)
            else 0
            Agent(
                index = index + 1,
                point = FramePoint(enemy.x, enemy.y),
                // for a sword enemy the tile is the facing, so take dir back off it and the two
                // can never disagree
                dir = if (swordEnemies) swordDir.dirFront(tile) ?: placed
                else enemy.direction ?: Direction.None,
                tile = tile,
                // AttackableDecider works off state.aliveEnemies, which filters on exactly this
                state = EnemyState.Alive,
                hp = 1,
                maxHp = 1
            )
        }
        val link = Agent(index = 0, point = FramePoint(start.x, start.y), dir = startDirection)
        return MapLocationState(
            hyrule = hyrule,
            frameState = FrameState(
                api = api,
                currentFrame = 0,
                enemies = agents,
                enemiesUncombined = emptyList(),
                enemiesRaw = emptyList(),
                level = if (swordEnemies) swordLevel else if (isLevel) 1 else 0,
                mapLoc = mapLoc,
                link = link,
                ladder = null,
                inventory = Inventory(api)
            )
        )
    }

    fun productionGoal(
        start: FramePoint,
        startDirection: Direction,
        enemies: List<FramePoint>,
        isLevel: Boolean,
        mapLoc: Int,
        fullHealth: Boolean = false,
        swordEnemies: Boolean = false,
        // a zero selectedItem reads as the boomerang, so ranged attacks are on unless this is off
        // and every square on an enemy's line would count as a goal
        param: RouteTo.RouteParam = RouteTo.RouteParam(allowRangedAttack = fullHealth)
    ): ProductionGoal {
        val state = syntheticState(
            start, startDirection, enemies, isLevel, mapLoc, fullHealth, swordEnemies
        )
        return ProductionGoal(state, param, RouteExecution())
    }
}

class ProductionGoal(
    val state: MapLocationState,
    param: RouteTo.RouteParam,
    exec: RouteExecution
) {
    // attackableSpec must stay EMPTY: a non-empty list makes RoutePreparation mark every enemy
    // attackable from every direction, which skips AttackableDecider - the only place
    // canAttackFront is enforced - and the goal then happily picks a square in front of a sword
    private val isGoal = IsGoal(exec, state, param)

    fun isGoal(point: FramePoint, isSafe: Boolean): Boolean = isGoal.isGoal(point, isSafe)

    fun actionAt(point: FramePoint, isSafe: Boolean): PointMoveAction =
        isGoal.getGoalAction(point, isSafe)

    fun describe(point: FramePoint, isSafe: Boolean): String =
        actionAt(point, isSafe).let { it::class.simpleName ?: it.toString() }

    fun inFrontOfEnemies(): List<FramePoint> =
        RouteToGetInFrontOf.getInFrontOfGridsSword(state)
}
