import java.util.*

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

        val actionResolver: PossibleActionResolver = PossibleActionResolverV1(gameState)
        val nextPossibleActions = actionResolver.computeNextPossibleActions()

        // TODO choisir la meilleure action
        val action = nextPossibleActions.firstOrNull() ?: Action.Wait()

        println(action)
    }
}

class Game(val kitchen: Kitchen)

class GameState(val game: Game, val player: Chef, val tablesWithItem: Tables, val customers: Customers) {
    fun findTableWith(item: Item): Table? {
        return tablesWithItem.findTableWith(item)
    }

    val kitchen: Kitchen
        get() = game.kitchen
}

class CustomerActionsWithAward(val customer: Customer, val actions: List<Action>, val award: Int)

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
        val chef = Chef(nextPosition())
        chef.item = nextItem()
        return chef
    }

    private fun nextTable(): Table {
        return Table(nextPosition(), nextItem())
    }

    private fun nextCustomer(): Customer {
        return Customer(nextItem(), input.nextInt())
    }

    private fun readTablesWithItem(): Tables {
        val tables = Tables()
        val numTablesWithItems = input.nextInt() // the number of tables in the kitchen that currently hold an item
        for (i in 0 until numTablesWithItems) {
            val table = nextTable()
            System.err.println("table : $table")
            tables.add(table)
        }
        return tables
    }

    private fun nextCustomers(): Customers {
        val customers = Customers()
        val numCustomers = input.nextInt() // the number of customers currently waiting for food
        for (i in 0 until numCustomers) {
            val customer = nextCustomer()
            System.err.println("customer : $customer")
            customers.add(customer)
        }
        return customers
    }

    private fun nextKitchen(): Kitchen {
        val kitchen = Kitchen()
        val equipmentReader = EquipmentReader()
        for (y in 0 until 7) {
            val kitchenLine = input.nextLine()
            kitchenLine.forEachIndexed { x, char ->
                val equipment = equipmentReader.read(char)
                if (equipment != null) {
                    kitchen.putEquipment(equipment, Position(x, y))
                }
            }
        }
        return kitchen
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
        @Suppress("UNUSED_VARIABLE") val partner = nextChef()
        val tablesWithItem = readTablesWithItem()
        @Suppress("UNUSED_VARIABLE") val ovenContents = input.next() // ignore until wood 1 league
        @Suppress("UNUSED_VARIABLE") val ovenTimer = input.nextInt()
        val customers = nextCustomers()
        return GameState(game, player, tablesWithItem, customers)
    }
}

class Kitchen {
    private val equipmentPositions = mutableMapOf<Equipment, Position>()
    private val itemProviders = mutableMapOf<Item, ItemProvider?>()

    fun putEquipment(equipment: Equipment, position: Position) {
        equipmentPositions[equipment] = position
    }

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

}

class EquipmentNotFoundException(equipment: Equipment) : Exception("${equipment.name} not found in the kitchen")

data class Position(val x: Int, val y: Int) {
    override fun toString(): String {
        return "$x $y"
    }

    fun isNextTo(position: Position): Boolean {
        return kotlin.math.abs(this.x - position.x) <= 1 && kotlin.math.abs(this.y - position.y) <= 1
    }
}

private const val NONE = "NONE"

@JvmInline
value class Item(val name: String) { // FIXME il faudrait séparer BaseItem et ComposedItem
    fun contains(item: Item): Boolean {
        return name.startsWith(item.name) // FIXME on n'est pas obligé de mettre les ingrédients dans l'ordre
    }

    val baseItems: Items
        get() = Items(
            name.split("-")
                .map { namePart -> if (namePart == name) this else Item(namePart) }
                .toList()
        )
    val isNone get() = name == NONE
    val withoutLastBaseItem: Item
        get() {
            val newName = name.substringBeforeLast("-")
            return if (newName.isEmpty()) {
                Item(NONE)
            } else {
                Item(newName)
            }
        }
    val isBase get() = !name.contains("-")

}

class Items(private val value: List<Item>) : List<Item> by value

class Chef(override var position: Position) : Positioned {
    var item: Item = Item(NONE)

    fun needsToDropItemToPrepare(itemToPrepare: Item): Boolean {
        return !item.isNone && !itemToPrepare.contains(this.item)
    }

}

data class Table(override val position: Position, val item: Item = Item(NONE)) : Positioned

class Tables : ArrayList<Table>() {
    fun findTableWith(item: Item): Table? {
        return find { table -> table.item == item }
    }
}

class Customer(
    val item: Item,
    /** award intrinsèque + nombre de tours restants */
    val award: Int
) {
    override fun toString(): String {
        return "Customer(item = $item, award = $award)"
    }
}

class Customers : ArrayList<Customer>()

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

    @Suppress("unused") // données du jeu de base
    class Move(private val position: Position, comment: String? = null) : Action("MOVE", comment) {
        override fun toString(): String {
            return if (comment != null) {
                "$name $position; $comment"
            } else {
                "$name $position"
            }
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
            'B' -> ItemProvider("BLUBERRIES_CRATE", Item("BLUEBERRIES"))
            'I' -> ItemProvider("ICE_CREAM_CRATE", Item("ICE_CREAM"))
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
        val DISHWASHER = ItemProvider("DISHWASHER", Item("DISH"))
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

                    if (item.isNone || player.item == item) { // FIXME on n'est pas obligé de mettre les ingrédients dans l'ordre
                        return emptyList()
                    }

                    val actions = mutableListOf<Action>()

                    if (player.needsToDropItemToPrepare(item)) {
                        actions += dropPlayerItem()
                    }

                    if (item.isBase) {
                        actions += get(item)
                    } else if (player.item != item) { // FIXME on n'est pas obligé de mettre les ingrédients dans l'ordre
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
                            CustomerActionsWithAward(customer, actions, actionsAward)
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
        return if (player.item == customer.item) {
            setOf(Action.Use(kitchen.getPositionOf(Equipment.WINDOW)))
        } else {
            val tableWithItem = tablesWithItem.find { table -> table.item == customer.item }
            if (tableWithItem != null) {
                setOf(Action.Use(tableWithItem.position, tableWithItem.toString()))
            } else {
                prepare(customer.item)
            }
        }
    }

    private fun prepare(item: Item): Set<Action> {

        if (item.isNone || player.item == item) { // FIXME on n'est pas obligé de mettre les ingrédients dans l'ordre
            return emptySet()
        }

        if (player.needsToDropItemToPrepare(item)) {
            return dropPlayerItem()
        }

        if (item.isBase) {
            return get(item)
        } else if (player.item != item) { // FIXME on n'est pas obligé de mettre les ingrédients dans l'ordre
            return prepare(item.withoutLastBaseItem)
        }

        return emptySet() // TODO dans quel cas on arrive ici ?
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

        val equipment = kitchen.getEquipmentThatProvides(item)
        val equipmentPosition = kitchen.getPositionOf(equipment)
        possibleActions += Action.Use(equipmentPosition, equipment.name)

        return possibleActions.toSet()
    }
}
