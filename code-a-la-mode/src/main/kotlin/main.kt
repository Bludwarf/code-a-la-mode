import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import java.util.function.Supplier
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

private const val PRINT_GAME = true
private const val PRINT_GAME_STATE = true

val equipmentMapper = EquipmentMapper()

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

    if (PRINT_GAME) {
        System.err.print("Game : ")
        writer.write(game)
        writer.flush()
        System.err.println()
    }

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
            Action.Wait("Error : ${e.message}")
            e.printStackTrace()
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

class Game(val kitchen: Kitchen, val customers: List<Customer> = emptyList())

data class GameState(
    private val game: Game,
    val turnsRemaining: Int,
    val player: Chef,
    val partner: Chef,
    val tablesWithItem: Set<Table>,
    val customers: List<Customer>,
    val playerScore: Int = 0,
    val ovenContents: Item? = null,
    val ovenTimer: Int = 0,
) {
    fun findTableWith(item: Item): Table? {
        return tablesWithItem.firstOrNull { table -> table.item == item }
    }

    fun getTableAt(position: Position): Table? {
        return (tablesWithItem + emptyTables).firstOrNull { table -> table.position == position }
    }

    fun isEmpty(position: Position): Boolean {
        return position != player.position && position != partner.position && kitchen.isEmpty(position)
    }

    private val emptyTables: Set<Table> get() = game.kitchen.tables - tablesWithItem

    val kitchen: Kitchen
        get() = game.kitchen

    fun contains(item: Item): Boolean {
        val chefs = setOf(player) // TODO faut-il inclure le partner ?
        return chefs.any { chef -> chef.contains(item) } || findTableWith(item) != null || kitchen.getEquipmentThatProvides(
            item
        ) != null
    }

    fun getOvenThatContains(item: Item?): Oven? {
        return if (ovenContents == item) {
            Equipment.OVEN
        } else {
            null
        }
    }

    fun getEmptyOven(): Oven? {
        return getOvenThatContains(null)
    }

    private fun findEmptyTablesNextTo(position: Position): List<Table> {
        return emptyTables
            .filter { emptyTable -> emptyTable.isNextTo(position) }
    }

    fun findClosestEmptyTablesTo(position: Position, pathFinder: PathFinder): List<Table> {
        val adjacentEmptyTables = findEmptyTablesNextTo(position)
        return adjacentEmptyTables.ifEmpty {
            val possiblePaths = pathFinder.possiblePathsWhile(position, { findEmptyTablesNextTo(it).isEmpty() })
            val minimumPathLength = possiblePaths.minOf { it.length }
            val shorterPaths = possiblePaths.filter { it.length == minimumPathLength }
            shorterPaths.flatMap { findClosestEmptyTablesTo(it.end, pathFinder) }
        }
    }

    fun containsAll(baseItems: Set<Item>): Boolean = baseItems.all { contains(it) }
    fun getPositionOf(item: Item): Position {
        val tableWithItem = findTableWith(item)
        if (tableWithItem != null) return tableWithItem.position

        val ovenThatContains = getOvenThatContains(item)
        if (ovenThatContains != null) return kitchen.getPositionOf(ovenThatContains)

        return kitchen.getPositionOf(item)
    }

    val tablesWithDish by lazy {
        tablesWithItem.filter { table -> table.item!!.baseItems.contains(Item.DISH) }
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

    private fun readTablesWithItem(): Set<Table> {
        val tables = mutableSetOf<Table>()
        val numTablesWithItems = input.nextInt() // the number of tables in the kitchen that currently hold an item
        for (i in 0 until numTablesWithItems) {
            val table = nextTable()
            tables.add(table)
        }
        return tables.toSet()
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

        val emptyPositions = mutableSetOf<Position>()
        val tables = mutableSetOf<Table>()
        val equipmentPositions = mutableMapOf<Equipment, Position>()
        for (y in 0 until Position.MAX_Y + 1) {
            val kitchenLine = input.nextLine()
            kitchenLine.forEachIndexed { x, char ->
                val position = Position(x, y)

                if (char == '#') {
                    tables += Table(position)
                } else {
                    val equipment = equipmentMapper.read(char)
                    if (equipment != null) {
                        equipmentPositions[equipment] = position
                    } else {
                        emptyPositions += position
                    }
                }
            }
        }
        // TODO init chefs spawn positions ("0" | "1")
        return Kitchen(emptyPositions, equipmentPositions, tables)
    }

    fun nextGame(): Game {
        val customers = nextCustomers()
        input.nextLine()
        val kitchen = nextKitchen()
        return Game(kitchen, customers)
    }

    fun nextGameState(game: Game): GameState {
        val turnsRemaining = input.nextInt()
        val player = nextChef()
        val partner = nextChef()
        val tablesWithItem = readTablesWithItem()
        val ovenContents = nextItem()
        val ovenTimer = input.nextInt()
        val customers = nextCustomers()
        return GameState(
            game,
            turnsRemaining,
            player,
            partner,
            tablesWithItem,
            customers,
            ovenContents = ovenContents,
            ovenTimer = ovenTimer
        )
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
    private val equipmentPositions: Map<Equipment, Position> = emptyMap(),
    val tables: Set<Table> = emptySet(),
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

    fun getPositionOf(item: Item): Position {
        val equipmentThatProvides = getEquipmentThatProvides(item)
        if (equipmentThatProvides != null) return getPositionOf(equipmentThatProvides)
        throw Throwable("Cannot find $item in kitchen")
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

    fun straightDistanceWith(position: Position): Double {
        return sqrt(abs(position.x - x).toDouble().pow(2) + abs(position.y - y).toDouble().pow(2))
    }

    val adjacentPositions: Set<Position> by lazy {
        val theoricalAdjacentPositions = listOf(
            Position(x, y - 1),
            Position(x - 1, y),
            Position(x + 1, y),
            Position(x, y + 1),
        )

        val validAdjacentPositions = mutableSetOf<Position>()
        for (adjacentPosition in theoricalAdjacentPositions) {
            if (x >= 0 && y >= 0 && x <= MAX_X && y <= MAX_Y) {
                validAdjacentPositions += adjacentPosition
            }
        }

        validAdjacentPositions.toSet()
    }

    companion object {
        const val MAX_X = 10
        const val MAX_Y = 6
    }
}

data class Item(val name: String) {

    constructor(baseItems: Set<Item>) : this(baseItems.joinToString("-") { it.name })

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

        return baseItems == other.baseItems
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


    fun isCompatibleWith(item: Item): Boolean {
        return item.contains(this)
    }

    fun isNotCompatibleWith(item: Item): Boolean {
        return !isCompatibleWith(item)
    }

    operator fun plus(otherItem: Item): Item = Item(baseItems + otherItem)
    operator fun minus(otherItem: Item): Item = Item(baseItems - otherItem)

    val baseItems: Items
        get() = Items(
            name.split("-")
                .map { namePart -> if (namePart == name) this else Item(namePart) }
                .toSet()
        )
    val isBase get() = !name.contains("-")

    companion object {
        val NONE = Item("NONE")
        val DISH = Item("DISH")
        val BLUEBERRIES = Item("BLUEBERRIES")
        val ICE_CREAM = Item("ICE_CREAM")

        val STRAWBERRIES = Item("STRAWBERRIES")
        val CHOPPED_STRAWBERRIES = Item("CHOPPED_" + STRAWBERRIES.name)

        val DOUGH = Item("DOUGH")
        val CROISSANT = Item("CROISSANT")

        val CHOPPED_DOUGH = Item("CHOPPED_DOUGH")
        val RAW_TART = Item("RAW_TART")
        val TART = Item("TART")
    }

}

class Items(private val value: Set<Item>) : Set<Item> by value {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Items) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

data class Chef(override var position: Position, val item: Item? = null) : Positioned {
    fun has(item: Item): Boolean {
        return this.item == item
    }

    fun contains(item: Item): Boolean {
        return has(item) || this.item != null && this.item.contains(item)
    }
}

data class Table(override val position: Position, var item: Item? = null) : Positioned {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Table) return false

        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int {
        return position.hashCode()
    }
}

class Customer(
    val item: Item,
    /** Award intrinsèque + nombre de tours restants */
    val award: Int,
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

    fun isNextTo(positioned: Positioned): Boolean {
        return this.isNextTo(positioned.position)
    }
}

abstract class Action(val command: String, val comment: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Action) return false
        return command == other.command
    }

    override fun hashCode(): Int {
        return command.hashCode()
    }

    class Move(override val position: Position, comment: String? = null) : Action("MOVE $position", comment), Positioned

    /**
     * Limitations :
     *
     * - On ne peut pas utiliser le *partner*
     */
    class Use(override val position: Position, comment: String? = null) : Action("USE $position", comment), Positioned

    class Wait(comment: String? = null) : Action("WAIT", comment)

    override fun toString(): String {
        return if (comment != null) {
            "$command; $comment"
        } else {
            command
        }
    }

}

abstract class Step {

    abstract fun isDone(gameState: GameState): Boolean

    data class GetSome(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(item)
    }

    data class Transform(val itemToTransform: Item, val equipment: Equipment, val resultingItem: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(resultingItem)
    }

    data class PutInOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.ovenContents == item
    }

    data class WaitForItemInOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.ovenContents == item
    }

    data class GetFromOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(item)
    }
}

class EquipmentMapper {
    fun read(char: Char): Equipment? {
        return when (char) {
            'D' -> Equipment.DISHWASHER
            'C' -> Equipment.CHOPPING_BOARD
            'W' -> Equipment.WINDOW
            'O' -> Equipment.OVEN
            'B' -> ItemProvider(Item.BLUEBERRIES)
            'I' -> ItemProvider(Item.ICE_CREAM)
            'S' -> ItemProvider(Item.STRAWBERRIES)
            'H' -> ItemProvider(Item.DOUGH)
            '#', '.' -> null
            else -> {
                debug("Unknown equipment char : $char")
                null
            }
        }
    }

    fun write(equipment: Equipment): Char? {
        return when (equipment) {
            Equipment.DISHWASHER -> 'D'
            Equipment.CHOPPING_BOARD -> 'C'
            Equipment.WINDOW -> 'W'
            Equipment.OVEN -> 'O'
            ItemProvider(Item.BLUEBERRIES) -> 'B'
            ItemProvider(Item.ICE_CREAM) -> 'I'
            ItemProvider(Item.STRAWBERRIES) -> 'S'
            ItemProvider(Item.DOUGH) -> 'H'
            else -> {
                debug("Unknown equipment : $equipment")
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
        val OVEN = Oven()
    }
}

class Oven : Equipment("OVEN") {

}

class ItemProvider(val providedItem: Item, name: String = providedItem.name + "_CRATE") : Equipment(name) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as ItemProvider
        return providedItem == other.providedItem
    }

    override fun hashCode(): Int {
        return providedItem.hashCode()
    }
}

class ItemProviderNotFoundException(item: Item) : Exception("Cannot find provider for $item")

class BestActionResolver {

    fun resolveBestActionFrom(gameState: GameState): Action {
        val actionsResolver: ActionsResolver = ActionsResolverWithActionToServeCustomer(gameState)
        return actionsResolver.nextAction()
    }

}

abstract class ActionsResolver(protected val gameState: GameState) {
    protected val kitchen = gameState.kitchen
    protected val player = gameState.player
    protected val customers = gameState.customers
    protected val ovenContents = gameState.ovenContents

    protected val baseItemsWantedByCustomers by lazy { customers.flatMap { it.item.baseItems }.toSet() }

    protected val useWindow = Action.Use(kitchen.getPositionOf(Equipment.WINDOW))
    protected val cookBook = CookBook()
    protected val pathFinder = PathFinder(gameState)

    abstract fun nextAction(): Action
    protected fun canBeTakenOutOfOven(ovenContents: Item): Boolean {
        // TODO calculer le temps qu'il faut pour aller au four avant que ça brule
        return cookBook.needToBeBakedByOven(ovenContents)
    }

    protected fun dropPlayerItem(comment: String = "Drop item", desiredPosition: Position = player.position): Action {
        return if (cookBook.contains(player.item!!)) {
            val nextEmptyTable = gameState.findClosestEmptyTablesTo(desiredPosition, pathFinder).firstOrNull()
                ?: throw CannotFindEmptyTables()
            use(nextEmptyTable, comment)
        } else {
            use(Equipment.WINDOW, comment)
        }
    }

    protected fun use(table: Table, comment: String = "Got some ${table.item?.name} on table $table"): Action {
        return Action.Use(table.position, comment)
    }

    protected fun use(equipment: Equipment, comment: String = "Use ${equipment.name}"): Action {
        if (!canBeUsed(equipment)) {
            if (player.item == null) return Action.Wait("Wait until $equipment can be used...")
            return dropPlayerItem(desiredPosition = kitchen.getPositionOf(equipment))
        }
        return Action.Use(kitchen.getPositionOf(equipment), comment)
    }

    private fun canBeUsed(equipment: Equipment): Boolean {
        return when (equipment) {
            is Oven -> gameState.ovenContents == null || canBeTakenOutOfOven(gameState.ovenContents)
            else -> true
        }
    }

    protected fun takeItemOutOfOven(ovenContents: Item): Action {
        if (player.item != null && player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) {
            if (customers.any { customer -> customer.item == player.item + ovenContents }) {
                return use(Equipment.OVEN, "Add ${ovenContents.name} to dish before it burns!")
            }
            return dropPlayerItem("Drop item to get ${ovenContents.name} before it burns!")
        }
        return use(Equipment.OVEN, "Run to oven to get ${ovenContents.name} before it burns!")
    }

    protected fun playerIsAllowedToGrab(item: Item): Boolean {
        val playerItem = player.item ?: return true
        if (item == Item.DISH) {
            return playerItem.isBase
        }
        if (playerItem == Item.CHOPPED_DOUGH && item == Item.BLUEBERRIES) {
            return true
        }
        if (playerItem.contains(Item.DISH)) {
            return !playerItem.contains(item)
        }
        return false
    }

    protected fun playerIsAllowedToUse(equipment: Equipment): Boolean {
        if (equipment is ItemProvider) {
            return playerIsAllowedToGrab(equipment.providedItem)
        }
        return true
    }

}

/**
 * Meilleur rang en Ligue Bronze : 21/713
 * Rang actuel en Ligue Bronze : 30/713
 */
class ActionsResolverWithActionToServeCustomer(gameState: GameState) : ActionsResolver(gameState) {

    override fun nextAction(): Action {
        if (ovenContents != null && canBeTakenOutOfOven(ovenContents)) {
            return takeItemOutOfOven(ovenContents)
        }

        return serveBestCustomer(customers, ::prepareNextMissingItem)
    }

    private fun prepareNextMissingItem(): Action {

        // FIXME pourquoi ne peut-on pas le mettre en private val ? => quand on le fait on se retrouve avec un SortedSet à 1 seul élément
//        val maxCustomerWithOnlyItemRemaining: Comparator<Item> = Comparator.comparing<Item, Int> { item ->
//            customers.count { customer -> gameState.contains(customer.item without item) }
//        }.reversed()

        // FIXME pourquoi ne peut-on pas le mettre en private val ? => quand on le fait on se retrouve avec un SortedSet à 1 seul élément
        val dropPlayerItemAction = if (player.item != null) dropPlayerItem() else null
        val fastestNextAction: Comparator<Item> = Comparator.comparing { item ->
            val actionToPrepareItem = prepare(item)
            debug("Action to prepare $item : $actionToPrepareItem")
            estimateTurnsToDo(actionToPrepareItem) + if (actionToPrepareItem == dropPlayerItemAction) 1 else 0
        }


//        val missingItemsToPrepare: Set<Item> = baseItemsWantedByCustomers
//            .filter { !gameState.contains(it) }
//            .toSortedSet(fastestNextAction)

        val missingItemsToPrepare: Set<Item> = baseItemsWantedByCustomers
            .filter { !gameState.contains(it) }
            .filter { ovenContents == null || cookBook.producedItemAfterBaking(ovenContents) != it }
            .sortedWith(fastestNextAction)
            .toSet()

        debug("Sorted missing items to prepare : $missingItemsToPrepare")
        val nextItemToPrepare: Item =
            missingItemsToPrepare.firstOrNull() ?: return Action.Wait("No first item to prepare")
        debug("Next item to prepare $nextItemToPrepare")
        return prepare(nextItemToPrepare)
    }

    private fun estimateTurnsToDo(action: Action): Int =
        when (action) {
            is Positioned -> estimateTurnsToGoTo(action.position)
            is Action.Wait -> ceil(gameState.ovenTimer / 2.0).toInt() // TODO est-ce une bonne estimation de se dire qu'on attend que pour le four ?
            else -> Int.MAX_VALUE
        }

    private fun estimateTurnsToGoTo(position: Position): Int {
        val distance = try {
            pathFinder.distance(player.position, position)
        } catch (e: Throwable) {
            debug("Cannot find path from ${player.position} to $position : ${e.message}")
            return Int.MAX_VALUE
        }
        return ceil(distance.toDouble() / 4).toInt()
    }

    private fun serveBestCustomer(customers: List<Customer>, fallbackActionSupplier: Supplier<Action>): Action {
        val actionToServerBestCustomer = customers
            .mapNotNull { customer ->
                try {
                    val action = serve(customer)
                    debug("Action to serve customer $customer : $action")
                    ActionToServeCustomer(action, customer)
                } catch (e: Throwable) {
                    debug("Cannot server customer $customer because of error : ${e.message}")
                    null
                }
            }
            .filter { it.action !is Action.Wait }
            .sortedWith(::compare)
            .reversed()
            .firstOrNull()
            ?: return fallbackActionSupplier.get()
        return actionToServerBestCustomer.action
    }

    private fun compare(
        actionToServeCustomer1: ActionToServeCustomer?,
        actionToServeCustomer2: ActionToServeCustomer?,
    ): Int {
        if (actionToServeCustomer1 == null) {
            return if (actionToServeCustomer2 == null) 0 else 1
        }
        if (actionToServeCustomer2 == null) {
            return -1
        }

        val action1Score = when (actionToServeCustomer1.action) {
            is Action.Use -> 1
            else -> 0
        }
        val action2Score = when (actionToServeCustomer2.action) {
            is Action.Use -> 1
            else -> 0
        }
        val actionScoreDiff = action1Score - action2Score
        if (actionScoreDiff != 0) return actionScoreDiff

        return actionToServeCustomer1.customer.award - actionToServeCustomer2.customer.award
    }

    data class ActionToServeCustomer(val action: Action, val customer: Customer)

    fun estimateAward(actionToServeCustomer: ActionToServeCustomer): Int {
        if (actionToServeCustomer.action is Action.Wait) return 0
        return actionToServeCustomer.customer.award
    }

    private fun serve(customer: Customer): Action {
        if (player.item != null) {
            if (player.item == customer.item) {
                return useWindow
            }
            if (player.item.isNotCompatibleWith(customer.item)) {
                return Action.Wait("Player has an item ${player.item} that is not compatible with ${customer.item}")
            }
        }
        return get(customer.item) { Action.Wait("Waiting for item to be prepared to serve $customer") }
    }

    private fun get(item: Item, actionOnMissingBaseItem: (Item) -> Action = ::prepare): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            if (!playerIsAllowedToGrab(tableWithItem.item!!)) {
                if (player.isNextTo(tableWithItem)) {
                    return dropPlayerItem("Player is not allowed to grab item from table $tableWithItem")
                }
            }
            return use(tableWithItem)
        }

        val equipment = kitchen.getEquipmentThatProvides(item)
        if (equipment != null) {
            if (!playerIsAllowedToUse(equipment)) {
                if (player.isNextTo(kitchen.getPositionOf(equipment))) {
                    return dropPlayerItem("Player is not allowed to grab item from equipment $equipment")
                }
            }
            return use(equipment)
        }

        return if (item.isBase) actionOnMissingBaseItem(item) else assemble(item.baseItems)
    }

    // TODO cache
    private fun prepare(item: Item): Action {
        if (!item.isBase) throw Throwable("Cannot prepare item to assemble $item")

        val stepsToPrepare = cookBook.stepsToPrepare(item)
        val lastDoneStepIndex = stepsToPrepare.indexOfLast { step -> step.isDone(gameState) }
        val nextStepToDo =
            if (lastDoneStepIndex < stepsToPrepare.size - 1) stepsToPrepare[lastDoneStepIndex + 1] else return Action.Wait(
                "Recipe completed ?!"
            )
        return when (nextStepToDo) {
            is Step.GetSome -> get(nextStepToDo.item)
            is Step.Transform -> use(nextStepToDo.equipment)
            is Step.PutInOven -> use(Equipment.OVEN)
            is Step.WaitForItemInOven -> if (player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) Action.Wait("Waiting for oven to bake ${nextStepToDo.item}") else use(
                Equipment.OVEN,
                "Moving to oven"
            )

            is Step.GetFromOven -> use(Equipment.OVEN)
            else -> Action.Wait("Cannot translate step into actions : $nextStepToDo")
        }
    }

    private fun assemble(baseItems: Set<Item>): Action {
        val playerBaseItems = player.item?.baseItems ?: emptySet()

        // TODO On peut très bien aller chercher une assiette en ayant un BaseItem dans les mains qui manque à l'assiette
        val tableWithMaxCompatibleItems = gameState.tablesWithDish
            .filter { table -> baseItems.containsAll(table.item!!.baseItems) }
            .maxByOrNull { table -> table.item!!.baseItems.size }

        if (tableWithMaxCompatibleItems != null) {
            val itemOnTable = tableWithMaxCompatibleItems.item!!
            if (baseItems - itemOnTable.baseItems == playerBaseItems) {
                return checkMissingBaseItemsAndThen(baseItems, tableWithMaxCompatibleItems.item!!.baseItems) {
                    use(tableWithMaxCompatibleItems)
                }
            }
        }

        return checkMissingBaseItemsAndThen(baseItems, playerBaseItems) { missingBaseItems ->
            if (playerBaseItems.isNotEmpty() && !playerBaseItems.contains(Item.DISH)) {
                get(Item.DISH)
            } else {
                val missingBaseItemsWithoutDish = missingBaseItems - Item.DISH
                debug("missingBaseItemsWithoutDish = $missingBaseItemsWithoutDish")

                val sorted = missingBaseItemsWithoutDish.sortedWith(
                    Comparator.comparing<Item, Int> { item -> inGameComparingValue(item) }
                        .thenComparing { item ->
                            try {
                                distanceFromPlayer(item)
                            } catch (e: Throwable) {
                                Int.MAX_VALUE
                            }
                        }
                )
                debug("sorted : $sorted")

                val firstBaseItemToGet = sorted.firstOrNull()

                if (firstBaseItemToGet == null) {
                    Action.Wait("Cannot choose first base item to get in empty list")
                } else {
                    debug("firstBaseItemToGet = $firstBaseItemToGet")
                    get(firstBaseItemToGet)
                }
            }
        }
    }

    private fun inGameComparingValue(
        item: Item,
    ): Int {
        return if (gameState.contains(item)) 1 else -1
    }

    private fun distanceFromPlayer(item: Item): Int {
        val position = gameState.getPositionOf(item)
        return pathFinder.distance(player.position, position)
    }

    private fun checkMissingBaseItemsAndThen(
        baseItems: Set<Item>,
        alreadyPreparedBaseItems: Set<Item>,
        nextActionFromMissingBaseItemsFunction: (Set<Item>) -> Action,
    ): Action {
        val missingBaseItems = (baseItems - alreadyPreparedBaseItems)
        val missingBaseItemsToPrepare = missingBaseItems.filter { baseItem -> !gameState.contains(baseItem) }
        if (missingBaseItemsToPrepare.isNotEmpty()) {
            if (ovenWillContainTheOnlyMissingBaseItemsToPrepare(missingBaseItemsToPrepare)) {
                debug("$missingBaseItemsToPrepare will be baked soon...")
            } else {
                return Action.Wait("Game does not contain all items to assemble : ${baseItems.joinToString(" & ")}")
            }
        }
        return nextActionFromMissingBaseItemsFunction(missingBaseItems)
    }

    private fun ovenWillContainTheOnlyMissingBaseItemsToPrepare(missingBaseItemsToPrepare: List<Item>): Boolean {
        val ovenContents = gameState.ovenContents ?: return false
        val lastMissingBaseItemToPrepare = missingBaseItemsToPrepare.getOrNull(0) ?: return false
        val producedItem = cookBook.producedItemAfterBaking(ovenContents)
        return producedItem == lastMissingBaseItemToPrepare
    }
}

class CannotFindEmptyTables : Throwable("Cannot find any empty tables")

class EquipmentCannotBeUsed(equipment: Equipment) : Throwable("${equipment.name} cannot be used")

class PlayerHasAlreadyAnItem : Throwable("Player has already an item")

class CookBook {
    fun stepsToPrepare(item: Item, fallbackValue: List<Step>? = null): List<Step> {
        val baseItemsWithoutDish = item.baseItems - Item.DISH
        return baseItemsWithoutDish.flatMap { stepsToPrepareBaseItem(it, fallbackValue) } + Step.GetSome(Item.DISH)
    }

    fun stepsToPrepareBaseItem(item: Item, fallbackValue: List<Step>? = null): List<Step> {
        return when (item) {

            // Ligue Bois 2

            Item.CHOPPED_STRAWBERRIES -> listOf(
                Step.GetSome(Item.STRAWBERRIES),
                Step.Transform(Item.STRAWBERRIES, Equipment.CHOPPING_BOARD, Item.CHOPPED_STRAWBERRIES),
            )

            // Ligue Bois 3

            Item.CROISSANT -> listOf(
                Step.GetSome(Item.DOUGH),
                Step.PutInOven(Item.DOUGH),
                Step.WaitForItemInOven(Item.CROISSANT),
                Step.GetFromOven(Item.CROISSANT),
            )

            // Ligue Bronze

            Item.TART -> listOf(
                Step.GetSome(Item.RAW_TART),
                Step.PutInOven(Item.RAW_TART),
                Step.WaitForItemInOven(Item.TART),
                Step.GetFromOven(Item.TART),
            )

            Item.RAW_TART -> listOf(
                Step.GetSome(Item.CHOPPED_DOUGH),
                Step.GetSome(Item.BLUEBERRIES),
            )

            Item.CHOPPED_DOUGH -> listOf(
                Step.GetSome(Item.DOUGH),
                Step.Transform(Item.DOUGH, Equipment.CHOPPING_BOARD, Item.CHOPPED_DOUGH),
            )

            else -> fallbackValue ?: throw DontKnowHowToPrepare(item)
        }
    }

    fun contains(item: Item): Boolean {
        val fallbackValue = emptyList<Step>()
        val stepsToPrepare = stepsToPrepare(item, fallbackValue)
        return stepsToPrepare !== fallbackValue
    }

    fun needToBeBakedByOven(item: Item): Boolean {
        val fallbackValue = emptyList<Step>()
        val stepsToPrepare = stepsToPrepare(item, fallbackValue)
        return stepsToPrepare.any { step -> step is Step.WaitForItemInOven && step.item == item }
    }

    fun producedItemAfterBaking(ovenContents: Item): Item? {
        // TODO utiliser les recettes
        return when (ovenContents) {
            Item.DOUGH -> Item.CROISSANT
            Item.RAW_TART -> Item.TART
            else -> null
        }
    }

    fun totalStepsToPrepare(item: Item): List<Step> {
        val fallbackValue = emptyList<Step>()
        return stepsToPrepareBaseItem(item, fallbackValue).flatMap { step ->
            if (step is Step.GetSome) {
                stepsToPrepareBaseItem(step.item, fallbackValue)
            } else {
                fallbackValue
            }
        }
    }
}

class DontKnowHowToPrepare(item: Item) : Throwable("Don't know how to prepare $item")

class PathFinder(private val gameState: GameState) {

    internal fun findPath(position: Position, target: Position): Path? {
        return possiblePaths(position, target)
            .sortedWith(
                Comparator.comparing<Path?, Double?> { path -> path.minDistanceWith(target) }
                    .thenComparing { path -> path.length }
            )
            .firstOrNull()
    }

    internal fun possiblePaths(
        position: Position,
        target: Position,
        path: Path = Path(listOf(gameState.player.position)),
    ) = possiblePathsWhile(position, { !it.isNextTo(target) }, path)

    fun nextEmptyPositions(position: Position): Set<Position> {
        return position.adjacentPositions
            .filter(gameState::isEmpty)
            .toSet()
    }

    fun possiblePathsWhile(
        position: Position,
        whileCondition: (Position) -> Boolean,
        path: Path = Path(listOf(gameState.player.position)),
    ): List<Path> {
        if (!whileCondition(position)) {
            return listOf(path)
        }
        val nextPositions = nextEmptyPositions(position) - path.positions.toSet()
        return if (nextPositions.isEmpty()) {
            emptyList()
        } else {
            nextPositions
                .map { nextPosition -> path.copy(positions = path.positions + nextPosition) }
                .flatMap { nextPath -> possiblePathsWhile(nextPath.end, whileCondition, nextPath) }
        }
    }

    fun distance(position: Position, target: Position): Int {
        if (position.isNextTo(target)) return 0
        val path = findPath(position, target) ?: throw CannotFindPathException(position, target)
        return path.length
    }
}

class CannotFindPathException(position: Position, target: Position) :
    Exception("Cannot find path from $position to $target")

data class Path(val positions: List<Position>) {
    fun minDistanceWith(target: Position): Double {
        return positions.minOf { position -> position.straightDistanceWith(target) }
    }

    val end: Position get() = positions.last()
    val length: Int get() = positions.size - 1

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
    private var currentLineIsEmpty = true

    companion object {
        val EMPTY_KITCHEN_LINES = listOf(
            "###########",
            "#.........#",
            "#.####.##.#",
            "#.#..#..#.#",
            "#.##.####.#",
            "#.........#",
            "###########",
        )
    }

    fun write(game: Game) {
        write(game.customers)
        newLine()
        write(game.kitchen)
    }

    private fun write(kitchen: Kitchen) {
        for (y in 0 until Position.MAX_Y + 1) {
            val emptyKitchenLine = EMPTY_KITCHEN_LINES[y]
            var kitchenLine = ""
            for (x in 0 until Position.MAX_X + 1) {
                val position = Position(x, y)
                val char = charAt(kitchen, position)
                kitchenLine += char ?: emptyKitchenLine[x]
            }
            write(kitchenLine)
            newLine()
        }
    }

    private fun charAt(kitchen: Kitchen, position: Position): Char? {
        val equipment = kitchen.getEquipmentAt(position)
        if (equipment != null) {
            return equipmentMapper.write(equipment)
        }
        return null
    }

    fun newLine() {
        out.appendLine()
        currentLineIsEmpty = true
    }

    fun write(gameState: GameState) {
        write(gameState.turnsRemaining)
        write(gameState.player)
        write(gameState.partner)

        write(gameState.tablesWithItem.size)
        gameState.tablesWithItem.forEach { write(it) }

        write(gameState.ovenContents)
        write(gameState.ovenTimer)

        write(gameState.customers)
    }

    private fun write(customers: List<Customer>) {
        write(customers.size)
        customers.forEach { write(it) }
    }

    private fun write(chef: Chef) {
        write(chef.position)
        write(chef.item ?: Item.NONE)
    }

    private fun write(position: Position) {
        write(position.x)
        write(position.y)
    }

    private fun write(item: Item?) {
        write(item?.name ?: "NONE")
    }

    private fun write(table: Table) {
        write(table.position)
        write(table.item)
    }

    private fun write(customer: Customer) {
        write(customer.item)
        write(customer.award)
    }

    private fun write(value: Int) {
        write(value.toString())
    }

    private fun write(value: String) {
        if (!currentLineIsEmpty) out.write(" ")
        out.write(value)
        if (currentLineIsEmpty) currentLineIsEmpty = false
    }

    override fun close() {
        out.close()
    }

    fun flush() {
        out.flush()
    }
}
