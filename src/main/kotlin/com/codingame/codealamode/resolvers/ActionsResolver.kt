package com.codingame.codealamode.resolvers

import com.codingame.codealamode.*
import com.codingame.codealamode.exceptions.CannotFindEmptyTables

abstract class ActionsResolver(protected val gameState: GameState) {
    protected val cookBook by gameState::cookBook
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

    protected val itemBeingPrepared: Item? by lazy {
        if (player.item != null) {
            cookBook.onlyPreparedItemThatNeedToGetSome(player.item)
        } else {
            null
        }
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
