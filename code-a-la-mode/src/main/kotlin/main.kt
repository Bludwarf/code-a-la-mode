import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val PRINT_GAME_STATE = false

// https://www.codingame.com/ide/puzzle/code-a-la-mode
/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main() {
    debug(System.currentTimeMillis())
    val input = Input(Scanner(System.`in`))
    val game = input.nextGame()
    val bestActionResolver = BestActionResolver()
    val writer = Writer(System.err)

    // game loop
    while (true) {
        val gameState = input.nextGameState(game)
        val timer = Timer()

        if (PRINT_GAME_STATE) {
            System.err.print("GameState : ")
            writer.write(gameState)
            writer.flush()
            System.err.println()
        }

        val action = try {
            bestActionResolver.resolveBestActionFrom(gameState)
        } catch (e: Throwable) {
            Action.Wait(e.message)
        }

        debug("Δt = $timer")
        println(action)
    }
}

data class Timer(var t0: Long = System.currentTimeMillis()) {
    override fun toString(): String {
        val t1 = System.currentTimeMillis()
        val interval = t1 - t0
        t0 = System.currentTimeMillis()
        return interval.toString()
    }
}

class Game(val kitchen: Kitchen)

data class GameState(
    private val game: Game,
    val turnsRemaining: Int,
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
        return tablesWithItem.firstOrNull { table -> table.position == position }
    }

    fun isEmpty(position: Position): Boolean {
        return position != player.position && position != partner.position && kitchen.isEmpty(position)
    }

    val kitchen: Kitchen
        get() = game.kitchen

    fun contains(item: Item): Boolean {
        return findTableWith(item) != null || kitchen.getEquipmentThatProvides(item) != null
    }

}

fun debug(message: Any) {
    System.err.println(message.toString())
}

@JvmInline
value class Input(private val input: Scanner) {

    private fun nextPosition(): Position {
        return Position(input.nextInt(), input.nextInt())
    }

    private fun nextItem(): Item? {
        val name = input.next()
        return if (name == Item.NONE.name) {
            null
        } else {
            Item(name)
        }
    }

    private fun nextChef(): Chef {
        return Chef(nextPosition(), nextItem())
    }

    private fun nextTable(): Table {
        return Table(nextPosition(), nextItem()!!)
    }

    private fun nextCustomer(): Customer {
        return Customer(nextItem()!!, input.nextInt())
    }

    private fun readTablesWithItem(): List<Table> {
        val tables = mutableListOf<Table>()
        val numTablesWithItems = input.nextInt() // the number of tables in the kitchen that currently hold an item
        for (i in 0 until numTablesWithItems) {
            val table = nextTable()
            tables.add(table)
        }
        return tables.toList()
    }

    private fun nextCustomers(): List<Customer> {
        val customers = mutableListOf<Customer>()
        val numCustomers = input.nextInt() // the number of customers currently waiting for food
        for (i in 0 until numCustomers) {
            val customer = nextCustomer()
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
        val turnsRemaining = input.nextInt()
        val player = nextChef()
        val partner = nextChef()
        val tablesWithItem = readTablesWithItem()
        @Suppress("UNUSED_VARIABLE") val ovenContents = input.next() // ignore until wood 1 league
        @Suppress("UNUSED_VARIABLE") val ovenTimer = input.nextInt()
        val customers = nextCustomers()
        return GameState(game, turnsRemaining, player, partner, tablesWithItem, customers)
    }

    internal fun nextAction(): Action {
        return when (val actionName = input.next()) {
            "USE" -> Action.Use(nextPosition())
            "MOVE" -> Action.Move(nextPosition())
            "WAIT" -> Action.Wait()
            else -> throw NotImplementedError("Unknown action $actionName")
        }
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

    fun requireEquipmentThatProvides(item: Item): ItemProvider {
        return itemProviders.computeIfAbsent(item) {
            equipmentPositions.keys
                .filterIsInstance<ItemProvider>()
                .firstOrNull { itemProvider -> itemProvider.providedItem == item }
        } ?: throw ItemProviderNotFoundException(item)
    }

    fun getEquipmentThatProvides(item: Item): ItemProvider? {
        return itemProviders.computeIfAbsent(item) {
            equipmentPositions.keys
                .filterIsInstance<ItemProvider>()
                .firstOrNull { itemProvider -> itemProvider.providedItem == item }
        }
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
        return baseItems.containsAll(item.baseItems)
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
    val isBase get() = !name.contains("-")

    companion object {
        val NONE = Item("NONE")
        val DISH = Item("DISH")
        val ICE_CREAM = Item("ICE_CREAM")
        val BLUEBERRIES = Item("BLUEBERRIES")
        val STRAWBERRIES = Item("STRAWBERRIES")
        val CHOPPED_STRAWBERRIES = Item("CHOPPED_" + STRAWBERRIES.name)
    }

}

class Items(private val value: List<Item>) : List<Item> by value

data class Chef(override var position: Position, val item: Item?) : Positioned

data class Table(override val position: Position, val item: Item) : Positioned

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
            'C' -> Equipment.CHOPPING_BOARD
            'W' -> Equipment.WINDOW
            'B' -> ItemProvider(Item.BLUEBERRIES)
            'I' -> ItemProvider(Item.ICE_CREAM)
            'S' -> ItemProvider(Item.STRAWBERRIES)
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
        val DISHWASHER = ItemProvider(Item.DISH, "DISHWASHER")
        val WINDOW = Equipment("WINDOW")
        val CHOPPING_BOARD = Equipment("CHOPPING_BOARD")
    }
}

class ItemProvider(val providedItem: Item, name: String = providedItem.name + "_CRATE") : Equipment(name)

class ItemProviderNotFoundException(item: Item) : Exception("Cannot find provider for $item")

class BestActionResolver {

    fun resolveBestActionFrom(gameState: GameState): Action {
        val actionsResolver = ActionsResolver(gameState)
        return actionsResolver.nextActionsFrom(gameState).first()
    }

    private class ActionsResolver(val gameState: GameState) {
        private val kitchen get() = gameState.kitchen
        private val player get() = gameState.player
        private val tablesWithItem get() = gameState.tablesWithItem
        private val customers get() = gameState.customers
        private val useWindow = use(Equipment.WINDOW)

        fun nextActionsFrom(gameState: GameState): List<Action> {
            return when (val playerItem = gameState.player.item) {
                null -> {
                    actionsToServeBestCustomer(customers, Action.Wait("No customers"))
                }

                Item.STRAWBERRIES -> {
                    listOf(use(Equipment.CHOPPING_BOARD))
                }

                else -> {
                    val compatibleCustomers = customers.filter { customer -> customer.item.contains(playerItem) }
                    actionsToServeBestCustomer(compatibleCustomers, dropPlayerItem())
                }
            }
        }

        private fun actionsToServeBestCustomer(customers: List<Customer>, fallbackAction: Action): List<Action> {
            val actionsToServerBestCustomer = customers
                .mapNotNull { customer ->
                    try {
                        ActionsToServeCustomer(serve(customer), customer)
                    } catch (e: Throwable) {
                        null
                    }
                }
                .maxByOrNull(::estimateAward)
                ?: return listOf(fallbackAction)
            return actionsToServerBestCustomer.actions
        }

        private data class ActionsToServeCustomer(val actions: List<Action>, val customer: Customer)

        fun estimateAward(actionsToServeCustomer: ActionsToServeCustomer): Int {
            // TODO
            return actionsToServeCustomer.customer.award
        }

        fun serve(customer: Customer): List<Action> {

            val actions = mutableListOf<Action>()

            if (player.item != customer.item) {

                // Le plat existe déjà sur une table ?
                val tableWithItem = tablesWithItem.find { table -> table.item == customer.item }
                if (tableWithItem != null) {
                    actions += Action.Use(tableWithItem.position, tableWithItem.toString())
                } else {
                    actions += assemble(customer.item)
                }

            }

            actions += useWindow

            return actions.toList()
        }

        fun assemble(item: Item): List<Action> {
            debug("player.item.name : ${player.item?.name}")
            debug("item to prepare  : $item")

            if (player.item == item) {
                return emptyList()
            }

            val playerBaseItems = player.item?.baseItems ?: emptyList()

            val missingBaseItems = (item.baseItems - playerBaseItems.toSet())
            val getActions = missingBaseItems.flatMap { baseItem -> get(baseItem) }

            val missingBaseItemsToPrepare = missingBaseItems.filter { baseItem -> !gameState.contains(baseItem) }
            val prepareActions = missingBaseItemsToPrepare.flatMap { baseItem -> prepare(baseItem) }

            return prepareActions + getActions
        }

        private fun get(item: Item): List<Action> {
            val tableWithItem = gameState.findTableWith(item)
            if (tableWithItem != null) {
                return listOf(Action.Use(tableWithItem.position, "Got some ${item.name} on table $tableWithItem"))
            }

            val equipment = kitchen.getEquipmentThatProvides(item)
            if (equipment != null) {
                val equipmentPosition = kitchen.getPositionOf(equipment)
                return listOf(Action.Use(equipmentPosition, equipment.name))
            }

            return prepare(item)
        }

        fun dropPlayerItem(): Action {
            return Action.Use(kitchen.getPositionOf(Equipment.WINDOW), "dropPlayerItem")
        }

        private fun prepare(item: Item): List<Action> {
            val actions = mutableListOf<Action>()

            if (player.item != null) {
                actions += dropPlayerItem()
            }

            if (item == Item.CHOPPED_STRAWBERRIES) {
                if (gameState.player.item != Item.STRAWBERRIES) actions += get(Item.STRAWBERRIES)
                actions += Action.Use(gameState.kitchen.getPositionOf(Equipment.CHOPPING_BOARD), "Chopping...")
            } else {
                throw DontKnowHowToPrepare(item)
            }

            return actions.toList()
        }

        private fun use(equipment: Equipment): Action {
            return Action.Use(kitchen.getPositionOf(equipment))
        }

    }

}

class DontKnowHowToPrepare(item: Item) : Throwable("Don't know how to prepare $item")

class Simulator {
    fun simulate(gameState: GameState, action: Action): GameState {
        return when (action) {
            is Action.Use -> {
                simulate(gameState, action)
            }

            is Action.Move -> {
                simulate(gameState, action)
            }

            is Action.Wait -> {
                wait(gameState)
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
                } else if (equipment == Equipment.CHOPPING_BOARD) {
                    return simulateUseChoppingBoard(gameState)
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
            turnsRemaining = gameState.turnsRemaining - 1,
            tablesWithItem = gameState.tablesWithItem - table,
            player = if (player.item == null) player else player.copy(
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
                    turnsRemaining = gameState.turnsRemaining - 1,
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
                        turnsRemaining = gameState.turnsRemaining - 1,
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
            gameState
        } else {
            grabDishFromDishwasher(gameState)
        }
    }

    private fun simulateUseChoppingBoard(gameState: GameState): GameState {
        return if (gameState.player.item == Item.STRAWBERRIES) {
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
                player = gameState.player.copy(
                    item = Item.CHOPPED_STRAWBERRIES
                ),
            )
        } else {
            gameState
        }
    }

    private fun simulateUseWindow(gameState: GameState): GameState {
        val player = gameState.player
        val customerThatWantPlayerItem =
            gameState.customers.firstOrNull { customer -> customer.item == player.item }
        return if (customerThatWantPlayerItem != null) {
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
                player = player.copy(
                    item = null
                ),
                customers = gameState.customers - customerThatWantPlayerItem,
                playerScore = gameState.playerScore + customerThatWantPlayerItem.award,
            )
        } else {
            gameState
        }
    }

    private fun simulateUse(equipment: ItemProvider, gameState: GameState): GameState {
        // FIXME on ne peut pas prendre une fraise quand on a une assiette (cf message d'erreur : bludwarf: Cannot take Dish(contents=[ICE_CREAM, BLUEBERRIES]) while holding STRAWBERRIES!)
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = if (gameState.player.item == null) equipment.providedItem else gameState.player.item.with(
                    equipment.providedItem
                )
            )
        )
    }

    private fun grabDishFromDishwasher(gameState: GameState): GameState {
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = Item.DISH
            )
        )
    }

    private fun wait(gameState: GameState): GameState {
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
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

internal class Writer(`out`: OutputStream) : AutoCloseable {
    private val out = PrintWriter(out)
    private var empty = true

    fun write(gameState: GameState) {
        write(gameState.turnsRemaining)
        write(gameState.player)
        write(gameState.partner)

        write(gameState.tablesWithItem.size)
        gameState.tablesWithItem.forEach { write(it) }

        write("NONE") // TODO ovenContents
        write("0") // TODO ovenTimer

        write(gameState.customers.size)
        gameState.customers.forEach { write(it) }
    }

    private fun write(chef: Chef) {
        write(chef.position)
        write(chef.item ?: Item.NONE)
    }

    private fun write(position: Position) {
        write(position.x, position.y)
    }

    private fun write(item: Item) {
        write(item.name)
    }

    private fun write(vararg values: Any) {
        for (value in values) {
            write(value)
        }
    }

    private fun write(table: Table) {
        write(table.position)
        write(table.item)
    }

    private fun write(customer: Customer) {
        write(customer.item)
        write(customer.award)
    }

    private fun write(value: Any) {
        if (!empty) out.write(" ")
        out.write(value.toString())
        if (empty) empty = false
    }

    override fun close() {
        out.close()
    }

    fun flush() {
        out.flush()
    }
}
