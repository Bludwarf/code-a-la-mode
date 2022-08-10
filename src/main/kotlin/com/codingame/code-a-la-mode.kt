import java.util.*

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

        val useWindow = Action.Use(kitchen.getEquipmentPosition(Equipment.WINDOW))

        fun actionsToServeItemFromTable(tableWithItem: Table): List<Action> {
            return listOf(
                Action.Use(tableWithItem.position), // On va le chercher
                useWindow
            );
        }

        fun actionsToPrepare(item: Item): List<Action> {
            debug("player.item.name" + player.item.name)
            if (player.item.name == "DISH-BLUEBERRIES") {
                return listOf(
                    Action.Use(kitchen.getEquipmentPosition(Equipment.ICE_CREAM_CRATE)),
                )
            }
            else if (player.item.name == "DISH") {
                return listOf(
                    Action.Use(kitchen.getEquipmentPosition(Equipment.BLUBERRIES_CRATE)),
                    Action.Use(kitchen.getEquipmentPosition(Equipment.ICE_CREAM_CRATE)),
                )
            }
            else {
                return listOf(
                    Action.Use(kitchen.getEquipmentPosition(Equipment.DISHWASHER)),
                    Action.Use(kitchen.getEquipmentPosition(Equipment.BLUBERRIES_CRATE)),
                    Action.Use(kitchen.getEquipmentPosition(Equipment.ICE_CREAM_CRATE)),
                )
            }
        }

        fun actionsToServe(customer: Customer): List<Action> {

            if (player.item == customer.item) {
                return listOf(useWindow)
            }

            // Le plat existe déjà sur une table ?
            val tableWithItem = tables.find { table -> table.item == customer.item }
            if (tableWithItem != null) {
                return actionsToServeItemFromTable(tableWithItem)
            }

            // Si le plat n'existe pas, il faut le préparer puis le servir
            return actionsToPrepare(customer.item) + useWindow
        }

        val actionsByCustomer = mutableMapOf<Customer, List<Action>>()
        for (customer in customers) {
            val actions = actionsToServe(customer)
            actionsByCustomer[customer] = actions;
        }

        val bestValuableCustomer = chooseBestValuableCustomer(actionsByCustomer)
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

fun chooseBestValuableCustomer(actionsByCustomer: MutableMap<Customer, List<Action>>): Customer {
    var bestValuableCustomer: Customer = actionsByCustomer.keys.first();
    for ((customer, actions) in actionsByCustomer) {
        val bestValuableCustomerActions = actionsByCustomer[bestValuableCustomer]!!
        if (actions.size < bestValuableCustomerActions.size) { // TODO faire un calcul plus précis du coût des actions
            bestValuableCustomer = customer
        }
    }
    return bestValuableCustomer
}

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

    fun getEquipmentPosition(equipment: Equipment): Position {
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
}

@JvmInline
value class Item(val name: String) {
}

class Chef(var position: Position) {
    var item: Item = Item("NONE")
}

class Table(override var position: Position) : Positioned {
    var item: Item = Item("NONE")
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
}

abstract class Action(val name: String, var comment: String? = null) {

    class Use(private val position: Position) : Action("USE") {
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

enum class Equipment(val char: Char) {
    DISHWASHER('D'),
    WINDOW('W'),
    BLUBERRIES_CRATE('B'),
    ICE_CREAM_CRATE('I'), ;

    companion object {
        fun get(char: Char): Equipment? {
            return Equipment.values().find { equipment -> equipment.char == char }
        }
    }
}
