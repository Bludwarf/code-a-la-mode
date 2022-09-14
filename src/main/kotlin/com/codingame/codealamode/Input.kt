package com.codingame.codealamode

import equipmentMapper
import java.util.*

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
        val nullableSpawnPositions: Array<Position?> = arrayOfNulls<Position>(2)
        val kitchenLines = mutableListOf<String>()
        for (y in 0 until Position.MAX_Y + 1) {
            val kitchenLine = input.nextLine()
            kitchenLines += kitchenLine
            kitchenLine.forEachIndexed { x, char ->
                val position = Position(x, y)

                if (char == '#') {
                    tables += Table(position)
                } else {
                    if (char == '0' || char == '1') {
                        if (char == '0') {
                            nullableSpawnPositions[0] = position
                        }
                        if (char == '1') {
                            nullableSpawnPositions[1] = position
                        }
                        emptyPositions += position
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
        }

        val spawnPositions = arrayOf(
            nullableSpawnPositions[0] ?: throw NotImplementedError("Missing spawn position for player."),
            nullableSpawnPositions[1] ?: throw NotImplementedError("Missing spawn position for partner."),
        )

        // TODO init chefs spawn positions ("0" | "1")
        return Kitchen(spawnPositions, emptyPositions, equipmentPositions, tables, kitchenLines)
    }

    fun nextGame(): Game {
        val customers = nextCustomers()
        input.nextLine()
        val kitchen = nextKitchen()
        return Game(kitchen, customers.toMutableList())
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
