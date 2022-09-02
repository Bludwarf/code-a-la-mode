import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import java.util.function.Supplier
import kotlin.math.abs
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
        return tablesWithItem.firstOrNull { table -> table.position == position }
    }

    fun isEmpty(position: Position): Boolean {
        return position != player.position && position != partner.position && kitchen.isEmpty(position)
    }

    private val emptyTables: Set<Table> get() = game.kitchen.tables - tablesWithItem

    val kitchen: Kitchen
        get() = game.kitchen

    fun contains(item: Item): Boolean {
        return findTableWith(item) != null || kitchen.getEquipmentThatProvides(item) != null
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

                if (char == '.') {
                    emptyPositions += position
                }

                if (char == '#') {
                    tables += Table(position)
                }

                val equipment = equipmentMapper.read(char)
                if (equipment != null) {
                    equipmentPositions[equipment] = position
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

    fun distanceWith(position: Position): Double {
        return sqrt(abs(position.x - x).toDouble().pow(2) + abs(position.y - y).toDouble().pow(2))
    }

    fun adjacentPositions(): Set<Position> {
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

        return validAdjacentPositions.toSet()
    }

    companion object {
        const val MAX_X = 10
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

    fun with(otherItem: Item): Item {
        return if (this == otherItem || baseItems.contains(otherItem)) {
            this
        } else {
            copy(
                name = "$name-${otherItem.name}"
            )
        }
    }

    fun isCompatibleWith(item: Item): Boolean {
        return item.contains(this)
    }

    fun isNotCompatibleWith(item: Item): Boolean {
        return !isCompatibleWith(item)
    }

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

    class Move(val position: Position, comment: String? = null) : Action("MOVE $position", comment)

    /**
     * Limitations :
     *
     * - On ne peut pas utiliser le *partner*
     */
    class Use(val position: Position, comment: String? = null) : Action("USE $position", comment)

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

    protected val useWindow = Action.Use(kitchen.getPositionOf(Equipment.WINDOW))
    protected val recipeBook = RecipeBook()
    protected val pathFinder = PathFinder(gameState)

    abstract fun nextAction(): Action
    protected fun canBeTakenOutOfOven(ovenContents: Item): Boolean {
        return recipeBook.needToBeBakedByOven(ovenContents)
    }

    protected fun dropPlayerItem(comment: String = "Drop item", desiredPosition: Position = player.position): Action {
        return if (recipeBook.contains(player.item!!)) {
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
            if (player.item == null) throw EquipmentCannotBeUsed(equipment)
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
//        playerState.mode = PlayerStateMode.TAKING_ITEM_OUT_OF_OVEN
        return when (player.item) {
            null -> use(Equipment.OVEN)
            else -> dropPlayerItem("Drop item to get ${ovenContents.name} before it burns!")
        }
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

        return serveBestCustomer(customers, ::dropPlayerItem)
    }

    private fun serveBestCustomer(customers: List<Customer>, fallbackActionSupplier: Supplier<Action>): Action {
        val actionToServerBestCustomer = customers
            .mapNotNull { customer ->
                try {
                    val action = serve(customer)
                    debug("Action to serve customer $customer : $action")
                    ActionToServeCustomer(action, customer)
                } catch (e: Throwable) {
                    debug("Cannot server customer $customer : ${e.message}")
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
        actionToServeCustomer2: ActionToServeCustomer?
    ): Int {
        if (actionToServeCustomer1 == null) {
            return if (actionToServeCustomer2 == null) 0 else 1
        }
        if (actionToServeCustomer2 == null) {
            return if (actionToServeCustomer1 == null) 0 else -1
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

    fun serve(customer: Customer): Action {
        val resolverForThisCustomer = ActionsResolverForOneCustomer(gameState.copy(customers = listOf(customer)))
        return resolverForThisCustomer.nextAction()
    }

    private fun get(item: Item): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            if (!playerIsAllowedToGrab(tableWithItem.item!!)) {
                throw PlayerHasAlreadyAnItem()
            }
            return use(tableWithItem)
        }

        val equipment = kitchen.getEquipmentThatProvides(item)
        if (equipment != null) {
            if (!playerIsAllowedToUse(equipment)) {
                return dropPlayerItem("Drop item to get ${item.name}")
//                throw PlayerHasAlreadyAnItem()
            }
            return use(equipment)
        }

        return if (item.isBase) prepare(item) else assemble(item.baseItems)
    }

    private fun prepare(item: Item): Action {
        return if (item.isBase) {
            val stepsToPrepare = recipeBook.stepsToPrepare(item)
            val lastDoneStepIndex = stepsToPrepare.indexOfLast { step -> step.isDone(gameState) }
            val nextStepToDo =
                if (lastDoneStepIndex < stepsToPrepare.size - 1) stepsToPrepare[lastDoneStepIndex + 1] else return dropPlayerItem(
                    "Recipe completed ?!"
                )
            when (nextStepToDo) {
                is Step.GetSome -> get(nextStepToDo.item)
                is Step.Transform -> use(nextStepToDo.equipment)
                is Step.PutInOven -> use(Equipment.OVEN)
                is Step.WaitForItemInOven -> Action.Wait("Waiting for oven to bake ${nextStepToDo.item}")
                is Step.GetFromOven -> use(Equipment.OVEN)
                else -> Action.Wait("Cannot translate step into actions : $nextStepToDo")
            }
        } else {
            assemble(item.baseItems)
        }
    }

    private fun assemble(baseItems: Set<Item>): Action {
//        if (player.item == item) {
//            return emptyList()
//        }

        val playerBaseItems = player.item?.baseItems ?: emptySet()

        if (playerBaseItems.isEmpty()) {
            val tableWithMaxCompatibleItems = gameState.tablesWithItem
                .filter { table -> table.item!!.baseItems.contains(Item.DISH) }
                .filter { table -> baseItems.containsAll(table.item!!.baseItems) }
                .maxByOrNull { table -> table.item!!.baseItems.size }
            if (tableWithMaxCompatibleItems != null) {
                return prepareFirstMissingBaseItemOr(baseItems, tableWithMaxCompatibleItems.item!!.baseItems) {
                    use(tableWithMaxCompatibleItems)
                }
            }
        }

        return prepareFirstMissingBaseItemOr(baseItems, playerBaseItems) { missingBaseItems ->
            // TODO on ne devrait pas prendre la DISH en 1er mais plutôt le 1er ingrédient, comme dans les recettes de base
            if (!playerBaseItems.contains(Item.DISH)) {
                get(Item.DISH)
            } else {
                val missingBaseItemsWithoutDish = missingBaseItems - Item.DISH
                get(missingBaseItemsWithoutDish.first())
            }
        }
    }

    private fun prepareFirstMissingBaseItemOr(
        baseItems: Set<Item>,
        alreadyPreparedBaseItems: Set<Item>,
        nextActionFromMissingBaseItemsFunction: (Set<Item>) -> Action
    ): Action {
        val missingBaseItems = (baseItems - alreadyPreparedBaseItems)
        val missingBaseItemsToPrepare = missingBaseItems.filter { baseItem -> !gameState.contains(baseItem) }
        if (missingBaseItemsToPrepare.isNotEmpty()) {
            if (gameState.ovenContents != null) {
                val itemsThatDoNotNeedToBeBakedByOven =
                    missingBaseItemsToPrepare.filter { !recipeBook.needToBeBakedByOven(it) }
                if (itemsThatDoNotNeedToBeBakedByOven.isNotEmpty()) {
                    return prepare(itemsThatDoNotNeedToBeBakedByOven.first())
                }
            }
            return prepare(missingBaseItemsToPrepare.first())
        }
        return nextActionFromMissingBaseItemsFunction(missingBaseItems)
    }

}

/**
 * Resolver avec seulement un seul client. Échoue le plus rapidement possible.
 */
class ActionsResolverForOneCustomer(gameState: GameState) : ActionsResolver(gameState) {

    private val customer =
        customers.getOrNull(0) ?: throw NotImplementedError("ActionsResolverForOneCustomer works only for 1 customer")

    override fun nextAction(): Action {
        return serve(customer)
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
        return get(customer.item)
    }

    private fun get(item: Item): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            if (!playerIsAllowedToGrab(tableWithItem.item!!)) {
                return Action.Wait("Player is not allowed to grab item from table $tableWithItem")
            }
            return use(tableWithItem)
        }

        val equipment = kitchen.getEquipmentThatProvides(item)
        if (equipment != null) {
            if (!playerIsAllowedToUse(equipment)) {
                return Action.Wait("Player is not allowed to grab item from equipment $equipment")
            }
            return use(equipment)
        }

        return assemble(item.baseItems)
    }

    private fun assemble(baseItems: Set<Item>): Action {
        if (!gameState.containsAll(baseItems)) return Action.Wait("Game does not contain all items to assemble : $baseItems")

        val playerBaseItems = player.item?.baseItems ?: emptySet()

        // TODO On peut très bien aller chercher une assiette en ayant un BaseItem dans les mains qui manque à l'assiette

        if (playerBaseItems.isEmpty()) {
            val tableWithMaxCompatibleItems = gameState.tablesWithDish
                .filter { table -> baseItems.containsAll(table.item!!.baseItems) }
                .maxByOrNull { table -> table.item!!.baseItems.size }
            if (tableWithMaxCompatibleItems != null) {
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
                val firstBaseItemToGet = chooseFirstBaseItemToGet(missingBaseItemsWithoutDish)
                debug("firstBaseItemToGet = $firstBaseItemToGet")
                get(firstBaseItemToGet)
            }
        }
    }

    private fun chooseFirstBaseItemToGet(baseItems: Set<Item>): Item {
        return baseItems.minByOrNull { distanceFromPlayer(it) } ?: throw Throwable("Cannot choose first base item to get in empty list")
    }

    private fun distanceFromPlayer(item: Item): Int {
        val position = gameState.getPositionOf(item)
        return pathFinder.distance(player.position, position)
    }

    private fun checkMissingBaseItemsAndThen(
        baseItems: Set<Item>,
        alreadyPreparedBaseItems: Set<Item>,
        nextActionFromMissingBaseItemsFunction: (Set<Item>) -> Action
    ): Action {
        val missingBaseItems = (baseItems - alreadyPreparedBaseItems)
        val missingBaseItemsToPrepare = missingBaseItems.filter { baseItem -> !gameState.contains(baseItem) }
        if (missingBaseItemsToPrepare.isNotEmpty()) {
            if (ovenWillContainTheOnlyMissingBaseItemsToPrepare(missingBaseItemsToPrepare)) {
                debug("$missingBaseItemsToPrepare will be baked soon...")
            } else {
                throw Throwable("Missing base items to prepare : $missingBaseItemsToPrepare")
            }
        }
        return nextActionFromMissingBaseItemsFunction(missingBaseItems)
    }

    private fun ovenWillContainTheOnlyMissingBaseItemsToPrepare(missingBaseItemsToPrepare: List<Item>): Boolean {
        val ovenContents = gameState.ovenContents ?: return false
        val lastMissingBaseItemToPrepare = missingBaseItemsToPrepare.getOrNull(0) ?: return false
        val producedItem = recipeBook.producedItemAfterBaking(ovenContents)
        return producedItem == lastMissingBaseItemToPrepare
    }

}

class CannotFindEmptyTables : Throwable("Cannot find any empty tables")

class EquipmentCannotBeUsed(equipment: Equipment) : Throwable("${equipment.name} cannot be used")

class PlayerHasAlreadyAnItem : Throwable("Player has already an item")

class RecipeBook {
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

    fun producedItemAfterBaking(ovenContents: Item): Item {
        val RAW = "RAW_"
        val indexOfRaw = ovenContents.name.indexOf(RAW)
        if (indexOfRaw == -1) return ovenContents
        return Item(ovenContents.name.substring(RAW.length))
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
                    .thenComparing { path -> path.size }
            )
            .firstOrNull()
    }

    internal fun possiblePaths(
        position: Position,
        target: Position,
        path: Path = Path(listOf(gameState.player.position)),
    ) = possiblePathsWhile(position, { it != target }, path)

    fun nextEmptyPositions(position: Position): Set<Position> {
        return position.adjacentPositions()
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
            listOf(path)
        } else {
            nextPositions
                .map { nextPosition -> path.copy(positions = path.positions + nextPosition) }
                .flatMap { nextPath -> possiblePathsWhile(nextPath.end, whileCondition, nextPath) }
        }
    }

    fun distance(position: Position, target: Position): Int {
        val path = findPath(position, target) ?: throw CannotFindPathException(position, target)
        return path.size
    }
}

class CannotFindPathException(position: Position, target: Position) :
    Exception("Cannot find path from $position to $target")

data class Path(val positions: List<Position>) : List<Position> by positions {
    fun minDistanceWith(target: Position): Double {
        return positions.minOf { position -> position.distanceWith(target) }
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

    private fun newLine() {
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
