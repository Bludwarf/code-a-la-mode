package com.codingame.codealamode

data class GameState(
    val game: Game,
    val turnsRemaining: Int,
    val player: Chef,
    val partner: Chef,
    val tablesWithItem: Set<Table> = emptySet(),
    val customers: List<Customer> = game.customers.subList(0, 3),
    val remainingCustomers: List<Customer> = game.customers.subList(3, game.customers.size),
    val playerScore: Int = 0,
    val ovenContents: Item? = null,
    val ovenTimer: Int = 0,
    val createdByTests: Boolean = false,
    val cookBook: CookBook = CookBook(),
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
