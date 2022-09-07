import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

private const val PRINT_GAME = true
private const val PRINT_GAME_STATE = true

private val cookBook = CookBook()

val equipmentMapper = EquipmentMapper()
var debugEnabled = true

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
            debug(e)
        }

        debug("Δt = $timer")
        println(action)
    }
}

data class Timer(var t0: Long = System.currentTimeMillis()) {

    val interval: Long
        get() {
            val t1 = System.currentTimeMillis()
            return t1 - t0
        }

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
    val createdByTests: Boolean = false,
) {
    fun findTableWith(item: Item): Table? {
        return findTablesWith(item).firstOrNull()
    }

    fun findTablesWith(item: Item): List<Table> {
        return tablesWithItem.filter { table -> table.item == item }
    }


    fun getTableAt(position: Position): Table? {
        return (tablesWithItem + emptyTables).firstOrNull { table -> table.position == position }
    }

    fun isEmpty(position: Position): Boolean {
        return position != player.position && position != partner.position && kitchen.isEmpty(position)
    }

    /**
     * Toutes les assiettes différentes en jeu
     */
    val dishes: Set<Item> by lazy {
        val dishes = tablesWithDish.map { it.item!! }.toMutableList()
        dishes += chefs.mapNotNull { it.item }.filter { it.contains(Item.DISH) }.toSet()
        if (dishes.size < 3) dishes += Item.DISH
        dishes.toSet()
    }

    private val emptyTables: Set<Table> get() = game.kitchen.tables - tablesWithItem

    val kitchen: Kitchen
        get() = game.kitchen

    val chefs = setOf(player, partner)

    val ovens = listOf(Equipment.OVEN)

    fun contains(item: Item): Boolean {
        return ovenContents == item || chefs.any { chef -> chef.has(item) } || findTableWith(item) != null || kitchen.getEquipmentThatProvides(
            item
        ) != null
    }

    fun doesNotContain(item: Item): Boolean {
        return !contains(item)
    }

    fun getOvenThatContains(item: Item?): Oven? {
        return findOvensThatContain(item).firstOrNull()
    }

    fun findOvensThatContain(item: Item?): List<Oven> {
        return if (ovenContents == item) {
            ovens
        } else {
            emptyList()
        }
    }

    fun findOvensThatWillProduce(item: Item): List<Oven> {
        val producedItemAfterBaking = cookBook.producedItemAfterBaking(ovenContents)
        return if (producedItemAfterBaking == item) ovens else emptyList()
    }


    fun getEmptyOven(): Oven? {
        return getOvenThatContains(null)
    }

    fun findEmptyTablesNextTo(position: Position): List<Table> {
        return emptyTables
            .filter { emptyTable -> emptyTable.isNextTo(position) }
    }

    fun findClosestEmptyTablesTo(
        position: Position,
        pathFinder: PathFinder,
    ): List<Table> {
        val adjacentEmptyTables = findEmptyTablesNextTo(position)
        return adjacentEmptyTables.ifEmpty {
            val possiblePaths = pathFinder.possiblePathsWhile(position, { findEmptyTablesNextTo(it).isEmpty() })
            val minimumPathLength = possiblePaths.minOf { it.length }
            val shorterPaths = possiblePaths.filter { it.length == minimumPathLength }
            shorterPaths.flatMap { findClosestEmptyTablesTo(it.end, pathFinder) }
        }
    }

    fun containsAll(baseItems: Set<Item>): Boolean = baseItems.all { contains(it) }
    fun getPositionOf(item: Item): Position? {
        val tableWithItem = findTableWith(item)
        if (tableWithItem != null) return tableWithItem.position

        val ovenThatContains = getOvenThatContains(item)
        if (ovenThatContains != null) return kitchen.getPositionOf(ovenThatContains)

        return kitchen.getPositionOf(item)
    }

    fun getPositionsOf(item: Item): List<Position> {
        val positions = mutableListOf<Position>()
        positions += findTablesWith(item).map { it.position }
        positions += findOvensThatContain(item).map { kitchen.getPositionOf(it) }

        val positionInKitchen = kitchen.getPositionOf(item)
        if (positionInKitchen != null) {
            positions += positionInKitchen
        }

        return positions.toList()
    }

    val tablesWithDish by lazy {
        tablesWithItem.filter { table -> table.item!!.baseItems.contains(Item.DISH) }
    }

}

fun debug(message: Any) {
    if (debugEnabled) System.err.println(message.toString())
}

fun debug(t: Throwable) {
    t.printStackTrace(System.err)
}

fun debug(elements: Collection<Any>) {
    if (debugEnabled) elements.forEach { debug("- $it") }
}

fun debug(titleMessage: Any, elements: Collection<Any>) {
    if (debugEnabled) {
        debug("$titleMessage :")
        elements.forEach { debug("- $it") }
    }
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

    private fun nextChef(name: String): Chef {
        return Chef(name, nextPosition(), nextItem())
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
        val player = nextChef("Player")
        val partner = nextChef("Partner")
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
    private val floorPositions: Set<Position> = emptySet(),
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
        return floorPositions.contains(position)
    }

    fun getEquipmentAt(position: Position): Equipment? {
        val entry = equipmentPositions.entries.firstOrNull { entry -> entry.value == position }
        return entry?.key
    }

    fun getPositionOf(item: Item): Position? {
        val equipmentThatProvides = getEquipmentThatProvides(item)
            ?: return null.also { debug("Cannot find equipment that provides $item in kitchen") }
        return getPositionOf(equipmentThatProvides)
    }

    val mostAccessibleTableComparator: Comparator<Table> by lazy {
        Comparator.comparing<Table, Int> { table -> floorPositions.count { it.isNextTo(table.position) } }
            .reversed()
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

    override fun toString(): String = name

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

class Chef(val name: String, override var position: Position, val item: Item? = null) : Positioned {

    fun copy(
        name: String = this.name,
        position: Position = this.position,
        item: Item? = this.item,
    ): Chef {
        return Chef(name, position, item)
    }

    val hasDish: Boolean by lazy { item?.contains(Item.DISH) ?: false }

    override fun equals(other: Any?): Boolean {
        if (other !is Chef) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return if (item != null) "$name with $item" else name
    }

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

    override fun toString(): String {
        return "$providedItem provider"
    }
}

class ItemProviderNotFoundException(item: Item) : Exception("Cannot find provider for $item")

class BestActionResolver {

    private val simulator = Simulator()

    fun resolveBestActionFrom(gameState: GameState): Action {
        val actionsResolver: ActionsResolver = ActionsResolverWithSimulation(gameState, simulator)
        return actionsResolver.nextAction()
    }

}

abstract class ActionsResolver(protected val gameState: GameState) {
    protected val kitchen = gameState.kitchen
    protected val player = gameState.player
    protected val partner = gameState.partner
    protected val customers = gameState.customers
    protected val ovenContents = gameState.ovenContents
    protected val ovenTimer = gameState.ovenTimer

    protected val baseItemsWantedByCustomers by lazy { customers.flatMap { it.item.baseItems }.toSet() }

    protected val useWindow = Action.Use(kitchen.getPositionOf(Equipment.WINDOW))
    protected val pathFinder = PathFinder(gameState)

    protected val Customer.index: Int get() = customers.indexOf(this) + 1
    protected fun Customer.toString(): String = "Customer #${customers.indexOf(this) + 1}"

    protected val Customer.withAllDishes: Set<CustomerWithDish>
        get() = allCustomersWithAllDishes.filter { it.customer == this }.toSet()

    abstract fun nextAction(): Action
    protected fun canBeTakenOutOfOven(ovenContents: Item): Boolean {
        // TODO calculer le temps qu'il faut pour aller au four avant que ça brule
        return cookBook.canBurnInOven(ovenContents) && (player.item == null || player.contains(Item.DISH) && !player.contains(
            ovenContents
        ))
    }

    protected val itemThatWillBurnInOven: Item? by lazy {
        if (cookBook.canBurnInOven(ovenContents)) ovenContents else null
    }

    protected fun dropPlayerItem(comment: String): Action {
        return if (cookBook.contains(player.item!!)) {
            val nextEmptyTable = gameState.findClosestEmptyTablesTo(player.position, pathFinder)
                .sortedWith(kitchen.mostAccessibleTableComparator)
                .firstOrNull() ?: throw CannotFindEmptyTables()
            use(nextEmptyTable, comment)
        } else {
            use(Equipment.WINDOW, comment)
        }
    }

    protected fun givePlayerItemToPartner(comment: String = "Give item to partner"): Action {

        // On cherche une table parfaite entre les deux joueur
        val playerAdjacentEmptyTables = gameState.findEmptyTablesNextTo(player.position)
        val partnerAdjacentEmptyTables = gameState.findEmptyTablesNextTo(partner.position)
        val commonAdjacentEmptyTables = playerAdjacentEmptyTables intersect partnerAdjacentEmptyTables
        val bestCommonTable = commonAdjacentEmptyTables
            .sortedWith(kitchen.mostAccessibleTableComparator)
            .firstOrNull()

        if (bestCommonTable == null) {
            // Si on a le temps, on se rapproche du partner
            val closestCommonTable = partnerAdjacentEmptyTables.filter { player.hasAccessTo(it) }
                .sortedWith(
                    closestTableFromPlayerComparator
                        .then(kitchen.mostAccessibleTableComparator)
                )
                .firstOrNull() ?: return dropPlayerItem(comment)

            return use(closestCommonTable, comment)
        }

        return use(bestCommonTable, comment)
    }

    private val closestTableFromPlayerComparator: Comparator<Table> by lazy {
        Comparator.comparing<Table, Int> { table -> distanceFromPlayerTo(table) }
    }

    protected fun use(table: Table, comment: String = "Got some ${table.item?.name} on table $table"): Action {
        return Action.Use(table.position, comment)
    }

    protected fun use(equipment: Equipment, comment: String = "Use ${equipment.name}"): Action {
        val equipmentPosition = kitchen.getPositionOf(equipment)
        if (player.isNextTo(equipmentPosition) && !canBeUsed(equipment)) {
            if (player.item == null) return Action.Wait("Wait until $equipment can be used...")
            return dropPlayerItem("Drop item because cannot use $equipment") // FIXME quand on attend le four, il ne faut pas jeter sa main
        }
        return Action.Use(equipmentPosition, comment)
    }

    private fun canBeUsed(equipment: Equipment): Boolean {
        return when (equipment) {
            is Oven -> gameState.ovenContents == null || canBeTakenOutOfOven(gameState.ovenContents)
            is ItemProvider -> if (equipment.providedItem == Item.STRAWBERRIES) player.item == null else true
            else -> true
        }
    }

    protected fun takeItemOutOfOven(item: Item): Action {
        return ActionsResolverToGetFromOven(item, gameState).nextAction()
    }

    protected fun playerIsAllowedToGrab(item: Item): Boolean {
        val playerItem = player.item ?: return true
        if (playerItem == Item.CHOPPED_DOUGH && item == Item.BLUEBERRIES) {
            return true
        }

        val playerItemAndItem = setOf(playerItem) + item
        if (playerItemAndItem.any { it == Item.STRAWBERRIES || it == Item.DOUGH }) return false

        val containsDishComparator = Comparator.comparing<Item, Boolean> { it.contains(Item.DISH) } // TODO constante
        if (containsDishComparator.compare(playerItem, item) == 0) return false

        // Le player ne doit pas contenir l'item et vice-versa
        return !player.item.contains(item) && !item.contains(player.item)
    }

    protected fun playerIsAllowedToUse(equipment: Equipment): Boolean {
        if (equipment is ItemProvider) {
            return playerIsAllowedToGrab(equipment.providedItem)
        }
        return true
    }

    protected fun distanceFromPlayerTo(item: Item): Int {
        val position = gameState.getPositionOf(item) ?: throw Throwable("Cannot find $item in kitchen")
        return distanceFromPlayerTo(position)
    }

    protected fun distanceFromPlayerTo(positioned: Positioned): Int {
        return distanceFromPlayerTo(positioned.position)
    }

    protected fun distanceFromPlayerTo(position: Position): Int {
        return pathFinder.distance(player.position, position)
    }

    protected val allCustomersWithAllDishes by lazy {
        customers.flatMap { customer ->
            gameState.dishes
                .filter { dish -> dish.isCompatibleWith(customer.item) }
                .map { dish ->
                    CustomerWithDish(customer, dish)
                }
        }
    }

    inner class CustomerWithDish(val customer: Customer, val dish: Item) {

        val missingItemsInDish: Set<Item> by lazy {
            customer.item.baseItems - dish.baseItems
        }

        val missingItemsInGame: Set<Item> by lazy {
            missingItemsInDish
                .filter(gameState::doesNotContain)
                .toSet()
        }

        val itemsToAssemble: Set<Item> by lazy {
            setOf(dish) + missingItemsInDish
        }

        val readyToBeServed: Boolean by lazy(missingItemsInGame::isEmpty)

        private val index: Int by lazy {
            customers.indexOf(customer)
        }

        override fun toString(): String {
            return "Customer ${index + 1} with $dish"
        }
    }

    protected fun Chef.canServe(customer: Customer): Boolean {
        val chef = this
        return customer.withAllDishes.any { customerWithDish ->
            customerWithDish.itemsToAssemble.all { item -> chef.hasAccessTo(item) }
        }
    }

    protected fun Chef.hasAccessTo(item: Item): Boolean {
        val chef = this
        if (chef.has(item)) return true
        val positionsOfItem = gameState.getPositionsOf(item)
        if (positionsOfItem.isEmpty()) return false
        return positionsOfItem.any { chef.hasAccessTo(it) }
    }

    protected fun Chef.hasAccessTo(positioned: Positioned): Boolean {
        val chef = this
        return chef.hasAccessTo(positioned.position)
    }

    protected fun Chef.hasAccessTo(position: Position): Boolean {
        val chef = this
        return pathFinder.possiblePaths(chef.position, position).isNotEmpty()
    }

    protected fun ovenContentsHasBeenByPutBy(chef: Chef): Boolean {
        if (ovenContents == null) return false
        val putter = if (ovenTimer % 2 == 0) player else partner
        return putter == chef
    }


}

class ActionsResolverWithSimulation(gameState: GameState, private val simulator: Simulator) :
    ActionsResolver(gameState) {

    override fun nextAction(): Action {

        if (ovenContentsHasBeenByPutBy(player)) {
            return watchCook()
        }

        if (itemThatWillBurnInOven != null) {
            if (fastestChefToGetFromOven(itemThatWillBurnInOven!!) != partner) {
                return takeItemOutOfOven(itemThatWillBurnInOven!!)
            }
        }

        if (customerBeingServedByPartner != null) {
            // TODO il faudrait aider le partner
        }

        var partnerIsIdle = customerBeingServedByPartner == null
        var itemPreparedByPartner: Item? = null
        customers
            .filter { it != customerBeingServedByPartner }
            .sortedByDescending { it.award }
            .forEach { customer: Customer ->

                if (isReadyToBeServed(customer)) {
                    if (chefMustBeJoinedToServe(customer)) {
                        return givePlayerItemToPartner("Drop item to let partner serve ${customer.index}")
                    } else if (partnerIsIdle && fastestChefToServe(customer) == partner) {
                        debug("Partner will serve ${customer.index}")
                        partnerIsIdle = false
                    } else if (customer canBeServedBy player) {
                        return serve(customer)
                    }
                } else {
                    val customerWithDish = customer.withAllDishes
                        .sortedBy { estimateComplexityToPrepare(it.missingItemsInDish) }
                        .also { debug("Customer with all dishes", it) }
                        .firstOrNull()
                    customerWithDish?.missingItemsInGame
                        ?.sortedByDescending { estimateValue(it) }
                        ?.also { debug("Missing items in game for $customerWithDish", it) }
                        ?.forEach { item ->
                            if (partnerIsIdle && fastestChefToPrepare(item, customerWithDish) == partner) {
                                debug("Partner will prepare $item for ${customer.index}")
                                partnerIsIdle = false
                                itemPreparedByPartner = item

                                val remainingMissingItemsInGame = customerWithDish.missingItemsInGame - item
                                if (remainingMissingItemsInGame.isEmpty()) {
                                    return helpPartnerToServe(customerWithDish, item)
                                }

                            } else {
                                return prepare(item, customerWithDish)
                            }
                        }
                }

            }

        return Action.Wait("No more customer")
    }

    private fun watchCook(): Action {
        return ActionsResolverToWatchOven(gameState).nextAction()
    }

    /**
     * - Le joueur porte le seul plat possible pour le client
     * - Le partenaire porte le dernier ingrédient nécessaire
     * - Il n'y a pas d'autres ingrédients en jeu
     */
    private fun chefMustBeJoinedToServe(customer: Customer): Boolean {
        val customerWithPartialyAssembledDishes = customer.withAllDishes.filter { it.dish != Item.DISH }
        if (customerWithPartialyAssembledDishes.size == 1) {
            val customerWithDish = customerWithPartialyAssembledDishes.first()

            if (player.has(customerWithDish.dish)) {
                if (customerWithDish.missingItemsInDish.size == 1) {
                    val lastItem = customerWithDish.missingItemsInDish.first()
                    return partner.has(lastItem)
                }
            }
        }
        return false
    }

    private val customerBeingServedByPartner: Customer? by lazy { findOnlyCompatibleCustomerOrNull(partner) }

    private fun findOnlyCompatibleCustomerOrNull(chef: Chef): Customer? =
        allCustomersWithAllDishes
            .filter { chef.has(it.dish) }
            .map { it.customer }.singleOrNull()

    private fun serve(customer: Customer): Action {
        debug("Serve customer $customer")

        val fastestCustomerWithDishToServe = customer.withAllDishes.minByOrNull { countTurnsToServe(it) }
            ?: return Action.Wait("No more dishes") // TODO trouver d'autres dish

        val actionsResolverToServe: ActionsResolver =
            if (fastestCustomerWithDishToServe.missingItemsInDish.isNotEmpty()) {
                ActionsResolverToAssemble(fastestCustomerWithDishToServe, gameState)
            } else {
                ActionsResolverToServe(fastestCustomerWithDishToServe, gameState)
            }
        return actionsResolverToServe.nextAction()
    }

    private fun countTurnsToServe(customerWithDish: CustomerWithDish): Int {
        val turns = 3 // TODO
        debug("$turns to serve $customerWithDish")
        return turns
    }

    private fun isReadyToBeServed(customer: Customer): Boolean =
        allCustomersWithAllDishes.filter { it.customer == customer }.any { it.readyToBeServed }

    private infix fun Customer.canBeServedBy(chef: Chef): Boolean {
        return this.withAllDishes.any {
            chef.hasAccessTo(it.dish) && it.missingItemsInDish.all { chef.hasAccessTo(it) }
        }
    }

    private fun estimateComplexityToPrepare(items: Set<Item>): Int? = estimateValue(items)

    private fun estimateValue(items: Set<Item>): Int? {
        if (items.isEmpty()) return 0
        return items.sumOf { estimateValue(it) ?: return null }
    }

    /**
     * TODO dans les calculs on a pris en compte les tours restants. Est-ce correct ?
     */
    private fun estimateValue(item: Item): Int? {
        if (!item.isBase) return estimateComplexityToPrepare(item.baseItems)
        return when (item) {
            Item.TART -> 1000
            Item.CROISSANT -> 850
            Item.CHOPPED_STRAWBERRIES -> 600
            Item.BLUEBERRIES -> 250
            Item.ICE_CREAM -> 200
            else -> null
        }
    }

    private fun fastestChefToGetFromOven(ovenContents: Item): Chef? =
        fastestChef("to get item from oven",
            { it.has(ovenContents) },
            { ActionsResolverToGetFromOven(ovenContents, it) }
        )

    private fun fastestChefToServe(customer: Customer): Chef? {

        val comparator: Comparator<Chef> = truesFirst { it.has(customer.item) }
            .then(truesFirst { it.canServe(customer) })
        // TODO simulation à faire

        val isTheFastest = "is the fastest to serve ${customer.index}"

        return when (comparator.compare(player, partner)) {
            -1 -> player.also { debug("Player $isTheFastest") }
            1 -> partner.also { debug("Partner $isTheFastest") }
            else -> null.also { debug("Don't know who $isTheFastest") }
        }
    }

    private fun fastestChefToPrepare(item: Item, customerWithDish: CustomerWithDish): Chef? =
        fastestChef("to prepare $item",
            { it.has(item) },
            { ActionsResolverToPrepare(item, customerWithDish, it) }
        )

    private fun fastestChef(
        toDoSomething: String,
        winCondition: Predicate<Chef>,
        actionsResolverSupplier: Function<GameState, ActionsResolver>,
    ): Chef? {

        val fastestComparator = simulator.fastestComparator(
            gameState,
            winCondition,
            actionsResolverSupplier
        )
        val comparator: Comparator<Chef> = fastestComparator // TODO ajouter des comparateurs plus rapides si possible
        val isTheFastest = "is the fastest $toDoSomething"

        return when (comparator.compare(player, partner)) {
            -1 -> player.also { debug("Player $isTheFastest") }
            1 -> partner.also { debug("Partner $isTheFastest") }
            else -> null.also { debug("Don't know who $isTheFastest") }
        }
    }

    private fun truesFirst(keyExtractor: (Chef) -> Boolean): Comparator<Chef> {
        return Comparator.comparing(keyExtractor).reversed()
    }

    private fun prepare(item: Item, customerWithDish: CustomerWithDish): Action {
        debug("Prepare ${item.name}")
        return ActionsResolverToPrepare(item, customerWithDish, gameState).nextAction() // TODO cache
    }

    /**
     * Aide le partenaire, qui prépare le dernier ingrédient manquant, à servir le plus rapidement possible le client
     */
    private fun helpPartnerToServe(customerWithDish: CustomerWithDish, lastMissingItemPreparedByPartner: Item): Action {
        debug("Help partner to serve $customerWithDish")
        return ActionsResolverToHelpPartnerToPrepare(
            customerWithDish,
            lastMissingItemPreparedByPartner,
            gameState
        ).nextAction()
    }

}

class ActionsResolverToWatchOven(
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // TODO c'est ici qu'on peut faire des actions en attendant la cuisson !
        return use(Equipment.OVEN, "Watch oven")
    }

}

class ActionsResolverToGetFromOven(
    private val itemToGetFromOven: Item,
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // TODO c'est ici qu'on peut faire des actions en attendant la cuisson, aussi ?
        if (player.item != null && player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) {
            if (customers.any { customer -> customer.item == player.item + itemToGetFromOven }) {
                return use(Equipment.OVEN, "Add ${itemToGetFromOven.name} to dish before it burns!")
            }
            return dropPlayerItem("Drop item to get ${itemToGetFromOven.name} before it burns!")
        }
        return use(Equipment.OVEN, "Run to oven to get ${itemToGetFromOven.name} before it burns!")
    }

}

class ActionsResolverToPrepare(
    private val itemToPrepare: Item,
    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action = prepare()

    private fun prepare(
        item: Item = itemToPrepare,
        customerWithDish: CustomerWithDish = this.customerWithDish,
    ): Action {

        val stepsToPrepare = cookBook.stepsToPrepare(item)
        val lastDoneStepIndex = stepsToPrepare.indexOfLast { step -> step.isDone(gameState) }
        val nextStep =
            if (lastDoneStepIndex < stepsToPrepare.size - 1) stepsToPrepare[lastDoneStepIndex + 1] else return Action.Wait(
                "Recipe completed ?!"
            )
        return when (nextStep) {
            is Step.GetSome -> get(nextStep.item) { prepare(nextStep.item, customerWithDish) }
            is Step.Transform -> use(nextStep.equipment)
            is Step.PutInOven -> use(Equipment.OVEN)
            is Step.WaitForItemInOven -> doWhileWaitingItemInOven(item, customerWithDish)

            is Step.GetFromOven -> use(Equipment.OVEN)
            else -> Action.Wait("Cannot translate step into actions : $nextStep")
        }
    }

    private fun get(item: Item, actionOnMissingBaseItem: (Item) -> Action): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            if (!playerIsAllowedToGrab(tableWithItem.item!!)) {
                if (player.isNextTo(tableWithItem)) {
                    return dropPlayerItem("Drop to get $item")
                }
            }
            return use(tableWithItem)
        }

        val equipment = kitchen.getEquipmentThatProvides(item)
        if (equipment != null) {
            if (!playerIsAllowedToUse(equipment)) {
                if (player.isNextTo(kitchen.getPositionOf(equipment))) {
                    return dropPlayerItem("Drop to get $item")
                }
            }
            return use(equipment)
        }

        return actionOnMissingBaseItem(item)
    }

    private fun doWhileWaitingItemInOven(item: Item, customerWithDish: CustomerWithDish): Action {
        if (player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) {
            // TODO si on peut chopper une assiette sans gêner le partner et sans faire bruler, il faut le faire
            return Action.Wait("Waiting for oven to bake $item")
        } else {
            return use(Equipment.OVEN, "Moving to oven")
        }
    }

}

class ActionsResolverToHelpPartnerToPrepare(
    private val customerWithDish: CustomerWithDish,
    private val lastMissingItemPreparedByPartner: Item,
    gameState: GameState,
) : ActionsResolver(gameState) {

    private val actionsResolverToAssemble: ActionsResolverToAssemble =
        ActionsResolverToAssemble(customerWithDish, gameState)

    override fun nextAction(): Action {
        // TODO Est-ce que je l'aide à préparer ou je l'aide à assembler ? Pour le moment on assemble
        return actionsResolverToAssemble.nextAction()
    }

}

class ActionsResolverToAssemble(
    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {

    override fun nextAction(): Action {
        return assembleFor(customerWithDish)
    }

    private fun assembleFor(customerWithDish: CustomerWithDish): Action {
        debug("Assembling for $customerWithDish")

        if (player.item == customerWithDish.customer.item) return dropPlayerItem("Drop dish to let partner serve $customerWithDish")

        val remainingItems = with(customerWithDish) {
            if (player.item != null) {
                if (player.item == dish) {
                    missingItemsInDish
                } else {
                    setOf(dish)
                }
            } else {
                setOf(dish) + missingItemsInDish
            }
        }.also { debug("Remaining items", it) }

        val remainingAccessibleItems = remainingItems
            .filter { player.hasAccessTo(it) } // On peut avoir des item en préparation par le partenaire
            .sortedWith(Comparator.comparing {
                distanceFromPlayerTo(it) // TODO trier les aliments restants à collecter pour player
            }).also { debug("Remaining accessible items", it) }

        if (remainingAccessibleItems.isEmpty()) {
            return partialAssembleFor(customerWithDish, remainingItems - remainingAccessibleItems.toSet())
        }

        val nextItem =
            remainingAccessibleItems.firstOrNull()
                ?: return Action.Wait("Waiting for someone to serve $customerWithDish")
        return get(nextItem)
    }

    /**
     * On fait un assemblage partiel car il ne reste que des items non accessibles
     */
    private fun partialAssembleFor(
        customerWithDish: CustomerWithDish,
        notAccessibleRemainingItems: Set<Item>,
    ): Action {
        debug("Partial assembling for $customerWithDish")
        if (notAccessibleRemainingItems.isEmpty()) return Action.Wait("Customer has no remaining item !?")

        val ovensThatWillProduceRemainingItems = notAccessibleRemainingItems.flatMap { item ->
            gameState.findOvensThatWillProduce(item)
        }

        val closestOven = ovensThatWillProduceRemainingItems.sortedBy { countTurnToTakeFrom(it) }
            .firstOrNull() ?: return Action.Wait("No oven will produce remaining items")

        if (ovenContentsHasBeenByPutBy(partner)) {
            return givePlayerItemToPartner("Give dish to partner to let him assemble remaining inaccessible items $notAccessibleRemainingItems")
        }

        return use(
            closestOven,
            "Using closest oven that will produce remaining items"
        )
    }

    private fun countTurnToTakeFrom(oven: Oven): Int {
        val distance = distanceFromPlayerTo(gameState.kitchen.getPositionOf(oven))
        val turnsToBeNextToOven = ceil(distance / 4.0).toInt()

        val turnsToWait = nextPairNumber((ovenTimer - turnsToBeNextToOven).coerceAtLeast(0))

        return turnsToBeNextToOven + turnsToWait + 1
    }

    private fun get(item: Item): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            if (!playerIsAllowedToGrab(tableWithItem.item!!)) {
                if (player.isNextTo(tableWithItem)) {
                    return dropPlayerItem("Drop to get $item")
                }
            }
            return use(tableWithItem)
        }

        val equipment = kitchen.getEquipmentThatProvides(item)
        if (equipment != null) {
            if (!playerIsAllowedToUse(equipment)) {
                if (player.isNextTo(kitchen.getPositionOf(equipment))) {
                    return dropPlayerItem("Drop to get $item")
                }
            }
            return use(equipment)
        }

        // TODO on pourrait regarder dans le four si l'élément va bientôt arriver
        // TODO il faut aussi s'assurer qu'on sera le 1er à ouvrir le four, sinon il faut déposer l'assiette au partenaire
        return Action.Wait("Waiting for missing $item to assemble ${customerWithDish.itemsToAssemble}")
    }

}

class ActionsResolverToServe(
    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {

    private val customer: Customer by lazy(customerWithDish::customer)

    override fun nextAction(): Action {
        debug("Serve $customerWithDish")

        return if (player.item != customer.item) get(customer.item)
        else useWindow
    }

    private fun get(item: Item): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            return use(tableWithItem)
        }
        return Action.Wait("Waiting for item $item to serve $customerWithDish")
    }

}

class ActionsResolverToWait(gameState: GameState) : ActionsResolver(gameState) {
    private val wait = Action.Wait("Only waiting...")
    override fun nextAction() = wait
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

    fun canBurnInOven(item: Item?): Boolean {
        return producedItemAfterBaking(item) == null
    }

    fun producedItemAfterBaking(ovenContents: Item?): Item? {
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

    // TODO cache
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

class Simulator {
    fun simulateWhile(
        initialGameState: GameState,
        whileCondition: Predicate<GameState>,
        gameStateFunction: (GameState) -> GameState,
    ): GameState {
        debug("=== Starting simulation ===")
        val production = !initialGameState.createdByTests
        if (production) debugEnabled = false
        val timer = Timer()

        var gameState = initialGameState
        while (gameState.turnsRemaining > 0 && (!production || timer.interval < 50) && whileCondition.test(gameState)) {
            val turnsRemaining = gameState.turnsRemaining
            gameState = gameStateFunction(gameState)
            if (gameState.turnsRemaining == turnsRemaining) TODO("Le simulateur n'a pas fait avancer le tour $turnsRemaining");
        }

        if (production) debugEnabled = true
        debug("=== Simulation finished in $timer ms ===")
        return gameState
    }

    fun fastestComparator(
        initialGameState: GameState,
        winCondition: Predicate<Chef>,
        actionsResolverSupplier: Function<GameState, ActionsResolver>,
    ): Comparator<Chef> {
        val whileCondition = Predicate<GameState> {
            !winCondition.test(it.player) && !winCondition.test(it.partner)
        }
        return Comparator { player: Chef, partner: Chef ->
            val initialState = initialGameState.copy(
                player = player.copy(),
                partner = partner.copy(),
            )
            val finalState = simulateWhile(initialState, whileCondition, actionsResolverSupplier)
            val lastChefToPlay = finalState.partner
            val winner: Chef? = if (winCondition.test(lastChefToPlay)) lastChefToPlay else null
            debug("Winner is $winner")
            when (winner) {
                player -> -1
                partner -> 1
                else -> 0
            }
        }
    }

    fun simulateWhile(
        initialGameState: GameState,
        whileCondition: Predicate<GameState>,
        actionsResolverSupplier: Function<GameState, ActionsResolver>,
    ): GameState {
        return simulateWhile(initialGameState, whileCondition) { previousGameState ->
            val actionsResolver = actionsResolverSupplier.apply(previousGameState)
            val action = actionsResolver.nextAction().also { debug("${previousGameState.player} => $it") }
            val nextGameState = previousGameState
                .let { simulate(it, action) }
                .let { simulateCook(it) }
                .let { nextGameState ->
                    nextGameState.copy(
                        turnsRemaining = previousGameState.turnsRemaining - 1,
                        player = nextGameState.partner,
                        partner = nextGameState.player,
                    )
                }
            nextGameState
        }
    }

    private fun simulateCook(gameState: GameState): GameState {
        if (gameState.ovenTimer == 0) return gameState

        val ovenTimer = gameState.ovenTimer - 1
        if (ovenTimer > 0) return gameState.copy(ovenTimer = ovenTimer)

        val previousOvenContents = gameState.ovenContents
        val nextOvenContents = cooked(previousOvenContents)
        val ovenContentsHasBurned = previousOvenContents != null && nextOvenContents == null
        return gameState.copy(
            ovenTimer = if (ovenContentsHasBurned) 0 else 10,
            ovenContents = nextOvenContents,
        )
    }

    private fun cooked(ovenContents: Item?): Item? {
        if (ovenContents == null) return null
        return cookBook.producedItemAfterBaking(ovenContents) ?: (null.also { debug("$ovenContents has burned !") })
    }

    /**
     * Ne simule pas autre chose que l'action
     */
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
                // TODO il faudrait séparer la simulation d'une action et la simulation du passage d'un tour (turns--, ovenTimer-- et ovenContents)
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
                return when (equipment) {
                    Equipment.DISHWASHER -> simulateUseDishwasher(gameState)
                    Equipment.CHOPPING_BOARD -> simulateUseChoppingBoard(gameState)
                    Equipment.OVEN -> simulateUseOven(gameState)
                    Equipment.WINDOW -> simulateUseWindow(gameState)
                    is ItemProvider -> simulateUse(equipment, gameState)
                    else -> TODO("Simulate use equipment $equipment")
                }
            }

            TODO("Simulate $action")
        } else {
            return simulate(gameState, Action.Move(position, action.comment), stopNextToPosition = true)
        }
    }

    private fun simulateUse(table: Table, gameState: GameState): GameState {
        val player = gameState.player

        val nextGameState = gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1
        )

        if (table.item == null) {
            if (player.item == null) {
                return nextGameState
            }
            val tableWithItem = table.copy(
                item = player.item
            )
            return nextGameState.copy(
                tablesWithItem = gameState.tablesWithItem + tableWithItem,
                player = player.copy(
                    item = null
                )
            )
        } else {
            return nextGameState.copy(
                tablesWithItem = gameState.tablesWithItem - table,
                player = if (player.item == null) player else player.copy(
                    item = player.item + table.item!!
                )
            )
        }
    }

    private fun simulate(
        gameState: GameState,
        action: Action.Move,
        stopNextToPosition: Boolean = false,
    ): GameState {
        val stopCondition =
            if (stopNextToPosition) gameState.player.position.isNextTo(action.position) else gameState.player.position == action.position
        val nextTurnGameState = gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
        )
        return if (stopCondition) {
            nextTurnGameState
        } else {
            val player = gameState.player
            if (player.position == action.position) {
                nextTurnGameState
            } else if (player.position.isNextTo(action.position)) {
                nextTurnGameState.copy(
                    player = player.copy(
                        position = action.position
                    )
                )
            } else {
                val pathFinder = PathFinder(gameState)
                val path = pathFinder.findPath(player.position, action.position)
                if (path == null) {
                    nextTurnGameState
                } else {
                    val nextPlayerPosition = path.subPath(4).lastOrNextOf(action.position)
                    nextTurnGameState.copy(
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
        val chopped = chopped(gameState.player.item) ?: return gameState
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = chopped
            ),
        )
    }

    private fun chopped(item: Item?): Item? {
        return when (item) {
            Item.STRAWBERRIES -> Item.CHOPPED_STRAWBERRIES
            Item.DOUGH -> Item.CHOPPED_DOUGH
            else -> null
        }
    }

    private fun simulateUseOven(gameState: GameState): GameState {
        val player = gameState.player
        val ovenContents = gameState.ovenContents
        if (player.item == null) {
            return gameState.copy(
                ovenContents = null,
                player = player.copy(
                    item = ovenContents
                ),
                ovenTimer = 0,
            )
        } else {
            if (ovenContents != null) {
                if (player.hasDish && !player.item.contains(ovenContents)) {
                    return gameState.copy(
                        ovenContents = null,
                        player = player.copy(
                            item = player.item + ovenContents
                        ),
                        ovenTimer = 0
                    )
                } else {
                    debug("ERROR : $player cannot take $ovenContents from oven")
                }
            } else {
                return gameState.copy(
                    ovenContents = player.item,
                    player = player.copy(
                        item = null
                    ),
                    ovenTimer = 10
                )
            }
        }
        return gameState
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
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
            )
        }
    }

    private fun simulateUse(equipment: ItemProvider, gameState: GameState): GameState {
        // FIXME on ne peut pas prendre une fraise quand on a une assiette (cf message d'erreur : bludwarf: Cannot take Dish(contents=[ICE_CREAM, BLUEBERRIES]) while holding STRAWBERRIES!)
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = if (gameState.player.item == null) {
                    equipment.providedItem
                } else if (gameState.player.item == Item.CHOPPED_DOUGH && equipment.providedItem == Item.BLUEBERRIES) {
                    Item.RAW_TART
                } else {
                    gameState.player.item + equipment.providedItem
                }
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

private fun nextPairNumber(x: Int): Int {
    return when (x % 2) {
        0 -> x
        else -> x + 1
    }
}
