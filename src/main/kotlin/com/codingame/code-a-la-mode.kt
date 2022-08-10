import java.util.*
import kotlin.collections.ArrayList

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args: Array<String>) {
    val input = Input(Scanner(System.`in`))
    val numAllCustomers = input.nextInt()
    for (i in 0 until numAllCustomers) {
        val customerItem = input.next() // the food the customer is waiting for
        val customerAward = input.nextInt() // the number of points awarded for delivering the food
    }
    input.nextLine()
    val kitchen = input.nextKitchen()

    // game loop
    while (true) {
        val turnsRemaining = input.nextInt()
        val player = input.nextChef()
        val partner = input.nextChef()
        val tables = input.readTables()
        val ovenContents = input.next() // ignore until wood 1 league
        val ovenTimer = input.nextInt()
        val customers = input.nextCustomers()

        val useWindow = Action.Use(kitchen.getPositionOf(Equipment.WINDOW))

        fun get(item: Item): Action {
            val equipment = Equipment.getEquipmentThatProvides(item)
            val equipmentPosition = kitchen.getPositionOf(equipment)
            return Action.Use(equipmentPosition, equipment.name)
        }

        fun dropPlayerItem(): Action {
            return Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER), "Drop player item")
        }

        fun prepare(item: Item): List<Action> {
            debug("player.item.name : ${player.item.name}")
            debug("item to prepare  : ${item}")

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
        for (customer in customers) {
            val actions = actionsToServe(customer)
            debug("actions for customer $customer : " + actions.joinToString("\n"))
            actionsByCustomer[customer] = actions;
        }

        fun costOf(action: Action): Int {
            if (action is Action.Use) {
                if (player.isNextTo(action.position)) {
                    return 1
                } else {
                    return 2 // TODO compute cost to go from player position to action.position
                }
            }
            return 1
        }

        fun costOf(actions: List<Action>): Int {
            return actions.sumOf { action -> costOf(action) }
        }

        fun chooseCustomerWithMaxAward(actionsByCustomer: MutableMap<Customer, List<Action>>, player: Chef): Customer {
            return actionsByCustomer
                .map { (customer, actions) ->
                    val actionsAward = customer.award - costOf(actions)
                    CustomerActionsWithAward(customer, actions, actionsAward)
                }
                .maxByOrNull(CustomerActionsWithAward::award)!!
                .customer
        }

        val bestValuableCustomer = chooseCustomerWithMaxAward(actionsByCustomer, player)
        val actions = actionsByCustomer[bestValuableCustomer]!!

        debug("actions : " + actions.joinToString("\n"))

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        val action = actions.firstOrNull() ?: Action.Wait();

        // MOVE x y
        // USE x y
        // WAIT
        println(action)
    }
}

class CustomerActionsWithAward(val customer: Customer, val actions: List<Action>, val award: Int)

fun debug(message: String) {
    System.err.println(message)
}

@JvmInline
value class Input(private val input: Scanner) {

    fun next(): String {
        return input.next()
    }

    fun nextInt(): Int {
        return input.nextInt()
    }

    fun nextLine(): String {
        return input.nextLine()
    }

    private fun nextPosition(): Position {
        return Position(input.nextInt(), input.nextInt())
    }

    private fun nextItem(): Item {
        return Item(input.next())
    }

    fun nextChef(): Chef {
        val chef = Chef(nextPosition())
        chef.item = nextItem()
        return chef
    }

    private fun nextTable(): Table {
        val table = Table(nextPosition())
        table.item = nextItem()
        return table
    }

    private fun nextCustomer(): Customer {
        return Customer(nextItem(), input.nextInt())
    }

    fun readTables(): Tables {
        val tables = Tables()
        val numTablesWithItems = input.nextInt() // the number of tables in the kitchen that currently hold an item
        for (i in 0 until numTablesWithItems) {
            val table = nextTable()
            System.err.println("table : $table")
            tables.add(table)
        }
        return tables
    }

    fun nextCustomers(): Customers {
        val customers = Customers()
        val numCustomers = input.nextInt() // the number of customers currently waiting for food
        for (i in 0 until numCustomers) {
            val customer = nextCustomer()
            System.err.println("customer : $customer")
            customers.add(customer)
        }
        return customers
    }

    fun nextKitchen(): Kitchen {
        val kitchen = Kitchen()
        for (y in 0 until 7) {
            val kitchenLine = input.nextLine()
            kitchenLine.forEachIndexed { x, char ->
                val equipment = Equipment.get(char)
                if (equipment != null) {
                    kitchen.putEquipment(equipment, Position(x, y))
                }
            }
        }
        return kitchen
    }
}

class Kitchen {
    private val equipmentPositions = mutableMapOf<Equipment, Position>()

    fun putEquipment(equipment: Equipment, position: Position) {
        equipmentPositions[equipment] = position
    }

    fun getPositionOf(equipment: Equipment): Position {
        if (!equipmentPositions.containsKey(equipment)) {
            throw EquipmentNotFoundException(equipment)
        }
        return equipmentPositions[equipment]!!
    }

}

class EquipmentNotFoundException(equipment: Equipment) : Exception("${equipment.name} not found in the kitchen") {
}

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
value class Item(val name: String) {
    fun contains(item: Item): Boolean {
        return name.startsWith(item.name)
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

class Items(private val value: List<Item>) : List<Item> by value {
}

class Chef(override var position: Position) : Positioned {
    var item: Item = Item(NONE)

    fun needsToDropItemToPrepare(itemToPrepare: Item): Boolean {
        return !item.isNone && !itemToPrepare.contains(this.item)
    }

}

class Table(override var position: Position) : Positioned {
    var item: Item = Item(NONE)
}

class Tables() : ArrayList<Table>() {

}

class Customer(val item: Item, val award: Int) {

}

class Customers() : ArrayList<Customer>() {
    fun first(): Customer {
        return get(0)
    }

}

interface Positioned {
    var position: Position

    fun isNextTo(position: Position): Boolean {
        return this.position.isNextTo(position)
    }
}

abstract class Action(val name: String, val comment: String? = null) {

    open class Use(val position: Position, comment: String? = null) : Action("USE", comment) {
        override fun toString(): String {
            return if (comment != null) {
                "$name $position; $comment"
            } else {
                "$name $position"
            }
        }
    }

    class Wait : Action("WAIT") {
    }

}

enum class Equipment(val char: Char, val providedItem: Item? = null) {
    DISHWASHER('D', Item("DISH")),
    WINDOW('W'),
    BLUBERRIES_CRATE('B', Item("BLUEBERRIES")),
    ICE_CREAM_CRATE('I', Item("ICE_CREAM")), ;

    companion object {
        fun get(char: Char): Equipment? {
            return Equipment.values().find { equipment -> equipment.char == char }
        }

        fun getEquipmentThatProvides(item: Item): Equipment {
            return values().find { equipment -> equipment.providedItem == item }
                ?: throw ItemProviderNotFoundException(item)
        }
    }
}

class ItemProviderNotFoundException(item: Item) : Exception("Cannot find provider for ${item}") {
}
