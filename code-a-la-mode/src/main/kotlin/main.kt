import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// https://www.codingame.com/ide/puzzle/code-a-la-mode
/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main() {
    val input = Input(Scanner(System.`in`))
    val game = input.nextGame()

    // game loop
    while (true) {
        val gameState = input.nextGameState(game)

        val bestActionResolver = BestActionResolver(PossibleActionResolverV2(gameState))
        val action = bestActionResolver.resolveBestActionFrom(gameState)

        println(action)
    }
}

class Game(val kitchen: Kitchen)

data class GameState(
    private val game: Game,
    val player: Chef,
    val partner: Chef,
    val tablesWithItem: List<Table>,
    val customers: List<Customer>,
    val playerScore: Int = 0
) {
    fun findTableWith(item: Item): Table? {
        return tablesWithItem.firstOrNull { table -> table.item == item }
    }

    fun getTableAt(position: Position): Table? {
        return tablesWithItem.firstOrNull() { table -> table.position == position }
    }

    fun isEmpty(position: Position): Boolean {
        return position != player.position && position != partner.position && kitchen.isEmpty(position)
    }

    val kitchen: Kitchen
        get() = game.kitchen
}

class CustomerActionsWithAward(val customer: Customer, val award: Int)

fun debug(message: String) {
    System.err.println(message)
}

@JvmInline
value class Input(private val input: Scanner) {

    private fun nextPosition(): Position {
        return Position(input.nextInt(), input.nextInt())
    }

    private fun nextItem(): Item {
        return Item(input.next())
    }

    private fun nextChef(): Chef {
        return Chef(nextPosition(), nextItem())
    }

    private fun nextTable(): Table {
        return Table(nextPosition(), nextItem())
    }

    private fun nextCustomer(): Customer {
        return Customer(nextItem(), input.nextInt())
    }

    private fun readTablesWithItem(): List<Table> {
        val tables = mutableListOf<Table>()
        val numTablesWithItems = input.nextInt() // the number of tables in the kitchen that currently hold an item
        for (i in 0 until numTablesWithItems) {
            val table = nextTable()
            System.err.println("table : $table")
            tables.add(table)
        }
        return tables.toList()
    }

    private fun nextCustomers(): List<Customer> {
        val customers = mutableListOf<Customer>()
        val numCustomers = input.nextInt() // the number of customers currently waiting for food
        for (i in 0 until numCustomers) {
            val customer = nextCustomer()
            System.err.println("customer : $customer")
            customers.add(customer)
        }
        return customers.toList()
    }

    private fun nextKitchen(): Kitchen {
        val equipmentReader = EquipmentReader()

        val emptyPositions = mutableSetOf<Position>()
        val equipmentPositions = mutableMapOf<Equipment, Position>()
        for (y in 0 until Position.MAX_Y + 1) {
            val kitchenLine = input.nextLine()
            kitchenLine.forEachIndexed { x, char ->
                val position = Position(x, y)

                if (char == '.') {
                    emptyPositions += position
                }

                val equipment = equipmentReader.read(char)
                if (equipment != null) {
                    equipmentPositions[equipment] = position
                }
            }
        }
        return Kitchen(emptyPositions, equipmentPositions)
    }

    fun nextGame(): Game {
        val numAllCustomers = input.nextInt()
        for (i in 0 until numAllCustomers) {
            @Suppress("UNUSED_VARIABLE") val customerItem = input.next() // the food the customer is waiting for
            @Suppress("UNUSED_VARIABLE") val customerAward =
                input.nextInt() // the number of points awarded for delivering the food
        }
        input.nextLine()
        val kitchen = nextKitchen()
        return Game(kitchen)
    }

    fun nextGameState(game: Game): GameState {
        @Suppress("UNUSED_VARIABLE") val turnsRemaining = input.nextInt()
        val player = nextChef()
        val partner = nextChef()
        val tablesWithItem = readTablesWithItem()
        @Suppress("UNUSED_VARIABLE") val ovenContents = input.next() // ignore until wood 1 league
        @Suppress("UNUSED_VARIABLE") val ovenTimer = input.nextInt()
        val customers = nextCustomers()
        return GameState(game, player, partner, tablesWithItem, customers)
    }

    internal fun nextAction(): Action {
        val actionName = input.next()
        if (actionName == "USE") {
            return Action.Use(nextPosition())
        }
        TODO("nextAction for $actionName")
    }
}

data class Kitchen(
    private val emptyPositions: Set<Position> = emptySet(),
    private val equipmentPositions: Map<Equipment, Position> = emptyMap()
) {
    private val itemProviders = mutableMapOf<Item, ItemProvider?>()

    fun getPositionOf(equipment: Equipment): Position {
        if (!equipmentPositions.containsKey(equipment)) {
            throw EquipmentNotFoundException(equipment)
        }
        return equipmentPositions[equipment]!!
    }

    fun getEquipmentThatProvides(item: Item): ItemProvider {
        return itemProviders.computeIfAbsent(item) {
            equipmentPositions.keys
                .filterIsInstance<ItemProvider>()
                .firstOrNull { itemProvider -> itemProvider.providedItem == item }
        } ?: throw ItemProviderNotFoundException(item)
    }

    fun isEmpty(position: Position): Boolean {
        return emptyPositions.contains(position)
    }

    fun getEquipmentAt(position: Position): Equipment? {
        val entry = equipmentPositions.entries.firstOrNull { entry -> entry.value == position }
        return entry?.key
    }

}

class EquipmentNotFoundException(equipment: Equipment) : Exception("${equipment.name} not found in the kitchen")

data class Position(val x: Int, val y: Int) {
    override fun toString(): String {
        return "$x $y"
    }

    fun isNextTo(position: Position): Boolean {
        return abs(this.x - position.x) <= 1 && abs(this.y - position.y) <= 1
    }

    fun distanceWith(position: Position): Double {
        return sqrt(abs(position.x - x).toDouble().pow(2) + abs(position.y - y).toDouble().pow(2))
    }

    companion object {
        const val MAX_Y = 6
    }
}

data class Item(val name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Item

        if (isBase) {
            if (!other.isBase) {
                return false
            }
            return name == other.name
        }
        if (other.isBase) {
            return false
        }

        val sortedBaseItems = baseItems.sortedBy { item -> item.name }
        val otherSortedBaseItems = other.baseItems.sortedBy { item -> item.name }
        return sortedBaseItems == otherSortedBaseItems
    }

    override fun hashCode(): Int {
        if (isBase) {
            return name.hashCode()
        }
        val sortedBaseItems = baseItems.sortedBy { item -> item.name }
        return sortedBaseItems.hashCode()
    }

    fun contains(item: Item): Boolean {
        return name.startsWith(item.name)
    }

    fun with(otherItem: Item): Item {
        return if (this == otherItem || baseItems.contains(otherItem)) {
            this
        } else {
            copy(
                name = "$name-${otherItem.name}"
            )
        }
    }

    val baseItems: Items
        get() = Items(
            name.split("-")
                .map { namePart -> if (namePart == name) this else Item(namePart) }
                .toList()
        )
    val isNone get() = this == NONE
    val withoutLastBaseItem: Item
        get() {
            val newName = name.substringBeforeLast("-")
            return if (newName.isEmpty()) {
                NONE
            } else {
                Item(newName)
            }
        }
    val isBase get() = !name.contains("-")

    companion object {
        val NONE = Item("NONE")
        val DISH = Item("DISH")
        val ICE_CREAM = Item("ICE_CREAM")
        val BLUEBERRIES = Item("BLUEBERRIES")
    }

}

class Items(private val value: List<Item>) : List<Item> by value

data class Chef(override var position: Position, val item: Item = Item.NONE) : Positioned {

    fun needsToDropItemToPrepare(itemToPrepare: Item): Boolean {
        return !item.isNone && !itemToPrepare.contains(this.item)
    }

}

data class Table(override val position: Position, val item: Item = Item.NONE) : Positioned

class Customer(
    val item: Item,
    /** Award intrinsèque + nombre de tours restants */
    val award: Int
) {
    override fun toString(): String {
        return "Customer(item = $item, award = $award)"
    }
}

interface Positioned {
    val position: Position

    fun isNextTo(position: Position): Boolean {
        return this.position.isNextTo(position)
    }
}

abstract class Action(val name: String, val comment: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Action
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    class Move(val position: Position, comment: String? = null) : Action("MOVE", comment) {
        override fun toString(): String {
            return if (comment != null) {
                "$name $position; $comment"
            } else {
                "$name $position"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (!super.equals(other)) return false
            if (this === other) return true
            if (other.javaClass != javaClass) return false
            other as Move
            return position == other.position
        }

        override fun hashCode(): Int {
            return "$name $position".hashCode()
        }
    }

    class Use(val position: Position, comment: String? = null) : Action("USE", comment) {
        override fun toString(): String {
            return if (comment != null) {
                "$name $position; $comment"
            } else {
                "$name $position"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (!super.equals(other)) return false
            if (this === other) return true
            if (other.javaClass != javaClass) return false
            other as Use
            return position == other.position
        }

        override fun hashCode(): Int {
            return "$name $position".hashCode()
        }
    }

    class Wait(comment: String? = null) : Action("WAIT", comment)

    override fun toString(): String {
        return if (comment != null) {
            "$name; $comment"
        } else {
            name
        }
    }

}

class EquipmentReader {
    fun read(char: Char): Equipment? {
        return when (char) {
            'D' -> Equipment.DISHWASHER
            'W' -> Equipment.WINDOW
            'B' -> ItemProvider("BLUEBERRIES_CRATE", Item.BLUEBERRIES)
            'I' -> ItemProvider("ICE_CREAM_CRATE", Item.ICE_CREAM)
            '#', '.' -> null
            else -> {
                debug("Unknown equipment char : $char")
                null
            }
        }
    }
}

open class Equipment(val name: String) {

    companion object {
        val DISHWASHER = ItemProvider("DISHWASHER", Item.DISH)
        val WINDOW = Equipment("WINDOW")
    }
}

class ItemProvider(name: String, val providedItem: Item) : Equipment(name)

class ItemProviderNotFoundException(val item: Item) : Exception("Cannot find provider for $item")

abstract class PossibleActionResolver(val gameState: GameState) {
    abstract fun computeNextPossibleActions(): Set<Action>
}

class PossibleActionResolverV1(gameState: GameState) : PossibleActionResolver(gameState) {
    override fun computeNextPossibleActions(): Set<Action> {
        return setOf(
            try {
                val kitchen = gameState.kitchen
                val player = gameState.player
                val tables = gameState.tablesWithItem
                val customers = gameState.customers

                val useWindow = Action.Use(kitchen.getPositionOf(Equipment.WINDOW))

                fun get(item: Item): Action {
                    val tableWithItem = gameState.findTableWith(item)
                    if (tableWithItem != null) {
                        return Action.Use(tableWithItem.position, "Got some ${item.name} on table $tableWithItem")
                    }
                    val equipment = kitchen.getEquipmentThatProvides(item)
                    val equipmentPosition = kitchen.getPositionOf(equipment)
                    return Action.Use(equipmentPosition, equipment.name)
                }

                fun dropPlayerItem(): Action {
                    return Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER), "Drop player item")
                }

                fun prepare(item: Item): List<Action> {

                    if (item.isNone || player.item == item) {
                        return emptyList()
                    }

                    val actions = mutableListOf<Action>()

                    if (player.needsToDropItemToPrepare(item)) {
                        actions += dropPlayerItem()
                    }

                    if (item.isBase) {
                        actions += get(item)
                    } else if (player.item != item) {
                        actions += prepare(item.withoutLastBaseItem)
                        actions += get(item.baseItems.last())
                    }

                    return actions.toList()
                }

                fun actionsToServe(customer: Customer): List<Action> {

                    val actions = mutableListOf<Action>()

                    if (player.item != customer.item) {

                        // Le plat existe déjà sur une table ?
                        val tableWithItem = tables.find { table -> table.item == customer.item }
                        if (tableWithItem != null) {
                            actions += Action.Use(tableWithItem.position, tableWithItem.toString())
                        } else {
                            actions += prepare(customer.item)
                        }

                    }

                    actions += useWindow

                    return actions.toList()
                }

                val actionsByCustomer = mutableMapOf<Customer, List<Action>>()
                var firstException: Exception? = null
                for (customer in customers) {
                    try {
                        val actions = actionsToServe(customer)
                        actionsByCustomer[customer] = actions
                    } catch (e: Exception) {
                        debug("Cannot serve $customer because of error : ${e.message}")
                        firstException = e
                    }
                }

                if (actionsByCustomer.isEmpty() && firstException != null) {
                    throw firstException
                }

                fun costOf(action: Action): Int {
                    if (action is Action.Use) {
                        return if (player.isNextTo(action.position)) {
                            1
                        } else {
                            2 // TODO compute cost to go from player position to action.position
                        }
                    }
                    return 1
                }

                fun costOf(actions: List<Action>): Int {
                    return actions.sumOf { action -> costOf(action) }
                }

                fun chooseCustomerWithMaxAward(actionsByCustomer: MutableMap<Customer, List<Action>>): Customer {
                    return actionsByCustomer
                        .map { (customer, actions) ->
                            val actionsAward = customer.award - costOf(actions)
                            CustomerActionsWithAward(customer, actionsAward)
                        }
                        .maxByOrNull(CustomerActionsWithAward::award)!!
                        .customer
                }


                if (actionsByCustomer.isEmpty()) {
                    Action.Wait("Cannot serve any customer")
                } else {
                    val bestValuableCustomer = chooseCustomerWithMaxAward(actionsByCustomer)
                    val actions = actionsByCustomer[bestValuableCustomer]!!

                    actions.firstOrNull() ?: Action.Wait("No action to perform")
                }

            } catch (e: ItemProviderNotFoundException) {
                Action.Wait(comment = e.item.name + "?!")
            } catch (e: Exception) {
                debug("ERROR : ${e.message}")
                Action.Wait(comment = e.message)
            }
        )
    }
}

class PossibleActionResolverV2(gameState: GameState) : PossibleActionResolver(gameState) {

    private val kitchen get() = gameState.kitchen
    private val player get() = gameState.player
    private val tablesWithItem get() = gameState.tablesWithItem

    override fun computeNextPossibleActions(): Set<Action> {
        return gameState.customers.flatMap { customer -> serve(customer) }.toSet()
    }

    private fun serve(customer: Customer): Set<Action> {
        return try {
            if (player.item == customer.item) {
                setOf(Action.Use(kitchen.getPositionOf(Equipment.WINDOW)))
            } else {
                val tableWithItem = tablesWithItem.find { table -> table.item == customer.item }
                if (tableWithItem != null) {
                    setOf(Action.Use(tableWithItem.position, tableWithItem.toString()))
                } else {
                    prepare(customer.item)
                }
            }
        } catch (e: Exception) {
            setOf(Action.Wait(e.message))
        }
    }

    private fun prepare(item: Item): Set<Action> {
        return if (item.isNone || player.item == item) {
            emptySet()
        } else if (item.isBase) {
            get(item)
        } else if (player.needsToDropItemToPrepare(item)) {
            dropPlayerItem()
        } else {
            val playerBaseItems = player.item.baseItems
            (item.baseItems - playerBaseItems)
                .flatMap { baseItem -> prepare(baseItem) }
                .toSet()
        }
    }

    private fun dropPlayerItem(): Set<Action> {
        // TODO poser l'item si possible pour gagner du temps
        return setOf(Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER), "Drop player item"))
    }

    private fun get(item: Item): Set<Action> {

        val possibleActions = mutableSetOf<Action>()

        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            possibleActions += Action.Use(tableWithItem.position, "Got some ${item.name} on table $tableWithItem")
        }

        try {
            val equipment = kitchen.getEquipmentThatProvides(item)
            val equipmentPosition = kitchen.getPositionOf(equipment)
            possibleActions += Action.Use(equipmentPosition, equipment.name)
        } catch (e: ItemProviderNotFoundException) {
            possibleActions += setOf(Action.Wait(e.message)) // on attend que quelqu'un d'autre prépare le baseItem
        }

        return possibleActions.toSet()

    }
}

class BestActionResolver(private val possibleActionResolver: PossibleActionResolver) {
    private val simulator = Simulator()

    fun resolveBestActionFrom(gameState: GameState): Action {
        val nextPossibleActions = possibleActionResolver.computeNextPossibleActions()
        val defaultAction = Action.Wait()

        return if (nextPossibleActions.isEmpty()) {
            defaultAction
        } else if (nextPossibleActions.size == 1) {
            nextPossibleActions.first()
        } else {
            nextPossibleActions.maxByOrNull { action -> computeAwardOf(action, gameState) } ?: defaultAction
        }
    }

    private fun computeAwardOf(action: Action, gameStateBeforeAction: GameState): Int {
        val gameStateAfterAction = simulator.simulate(gameStateBeforeAction, action)
        return gameStateAfterAction.playerScore - gameStateBeforeAction.playerScore
    }

}

class Simulator {
    fun simulate(gameState: GameState, action: Action): GameState {
        return when (action) {
            is Action.Use -> {
                simulate(gameState, action)
            }

            is Action.Move -> {
                simulate(gameState, action)
            }

            else -> {
                gameState
            }
        }
    }

    private fun simulate(gameState: GameState, action: Action.Use): GameState {
        val position = action.position
        if (gameState.player.position.isNextTo(position)) {

            val table = gameState.getTableAt(position)
            if (table != null) {
                return simulateUse(table, gameState)
            }

            val equipment = gameState.kitchen.getEquipmentAt(position)
            if (equipment != null) {
                if (equipment == Equipment.DISHWASHER) {
                    return simulateUseDishwasher(gameState)
                } else if (equipment == Equipment.WINDOW) {
                    return simulateUseWindow(gameState)
                } else if (equipment is ItemProvider) {
                    return simulateUse(equipment, gameState)
                }
            }

            debug("TODO simulate $action")
            TODO("simulate $action")
        } else {
            return simulate(gameState, Action.Move(position, action.comment), stopNextToPosition = true)
        }
    }

    private fun simulateUse(table: Table, gameState: GameState): GameState {
        val player = gameState.player
        return gameState.copy(
            tablesWithItem = gameState.tablesWithItem - table,
            player = if (table.item.isNone) player else player.copy(
                item = player.item.with(table.item)
            )
        )
    }

    private fun simulate(
        gameState: GameState,
        action: Action.Move,
        stopNextToPosition: Boolean = false
    ): GameState {
        val stopCondition =
            if (stopNextToPosition) gameState.player.position.isNextTo(action.position) else gameState.player.position == action.position
        return if (stopCondition) {
            gameState
        } else {
            val player = gameState.player
            if (player.position == action.position) {
                gameState
            } else if (player.position.isNextTo(action.position)) {
                gameState.copy(
                    player = player.copy(
                        position = action.position
                    )
                )
            } else {
                val pathFinder = PathFinder(gameState)
                val path = pathFinder.findPath(player.position, action.position)
                if (path == null) {
                    gameState
                } else {
                    val nextPlayerPosition = path.subPath(4).lastOrNextOf(action.position)
                    gameState.copy(
                        player = player.copy(
                            position = nextPlayerPosition
                        )
                    )
                }
            }
        }
    }

    private fun simulateUseDishwasher(gameState: GameState): GameState {
        val player = gameState.player
        val playerHasDish = player.item == Item.DISH
        return if (playerHasDish) {
            dropDishToDishwasher(gameState)
        } else {
            grabDishFromDishwasher(gameState)
        }
    }

    private fun simulateUseWindow(gameState: GameState): GameState {
        val player = gameState.player
        val customerThatWantPlayerItem =
            gameState.customers.firstOrNull { customer -> customer.item == player.item }
        return if (customerThatWantPlayerItem != null) {
            gameState.copy(
                player = player.copy(
                    item = Item.NONE
                ),
                customers = gameState.customers - customerThatWantPlayerItem,
                playerScore = gameState.playerScore + customerThatWantPlayerItem.award,
            )
        } else {
            gameState
        }
    }

    private fun simulateUse(equipment: ItemProvider, gameState: GameState): GameState {
        return gameState.copy(
            player = gameState.player.copy(
                item = gameState.player.item.with(equipment.providedItem)
            )
        )
    }

    private fun dropDishToDishwasher(gameState: GameState): GameState {
        // TODO inc dish count
        return gameState.copy(
            player = gameState.player.copy(
                item = Item.NONE
            )
        )
    }

    private fun grabDishFromDishwasher(gameState: GameState): GameState {
        // TODO dec dish count
        return gameState.copy(
            player = gameState.player.copy(
                item = Item.DISH
            )
        )
    }

}

class PathFinder(private val gameState: GameState) {

    internal fun findPath(position: Position, target: Position): Path? {
        return possiblePaths(position, target)
            .sortedWith(
                Comparator.comparing<Path?, Double?> { path -> path.minDistanceWith(target) }
                    .thenComparing { path -> path.size }
            )
            .firstOrNull()
    }

    internal fun possiblePaths(
        position: Position,
        target: Position,
        path: Path = Path(listOf(gameState.player.position))
    ): List<Path> {
        if (position == target) {
            return listOf(path)
        }
        val nextPositions = nextEmptyPositions(position) - path.positions.toSet()
        return if (nextPositions.isEmpty()) {
            listOf(path)
        } else {
            nextPositions
                .map { nextPosition -> path.copy(positions = path.positions + nextPosition) }
                .flatMap { nextPath -> possiblePaths(nextPath.end, target, nextPath) }
        }
    }

    private fun nextEmptyPositions(position: Position): Set<Position> {
        val x = position.x
        val y = position.y

        val adjacentPositions = listOf(
            Position(x, y - 1),
            Position(x - 1, y),
            Position(x + 1, y),
            Position(x, y + 1),
        )

        val nextEmptyPositions = mutableSetOf<Position>()
        for (adjacentPosition in adjacentPositions) {
            // TODO max x
            if (x >= 0 && y >= 0 && y <= Position.MAX_Y && gameState.isEmpty(adjacentPosition)) {
                nextEmptyPositions += adjacentPosition
            }
        }
        return nextEmptyPositions.toSet()
    }
}

data class Path(val positions: List<Position>) : List<Position> by positions {
    fun minDistanceWith(target: Position): Double {
        return positions.minOf { position -> position.distanceWith(target) }
    }

    val end: Position get() = positions.last()

    fun subPath(maxDistance: Int): Path {
        return copy(
            positions = positions.subList(0, positions.size.coerceAtMost(maxDistance + 1))
        )
    }

    fun lastOrNextOf(targetPosition: Position): Position {
        return positions.firstOrNull { position -> position.isNextTo(targetPosition) } ?: positions.last()
    }
}
