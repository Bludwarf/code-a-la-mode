
import com.codingame.codealamode.*
import com.codingame.codealamode.resolvers.ActionsResolver
import java.util.function.Supplier
import kotlin.math.ceil

private val cookBook = CookBook()

class ActionsResolverItemFocused(gameState: GameState, private val playerState: PlayerState) :
    ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // On doit d'abord décider si on est en mode préparation ou assemblage ou sauvetage anti-cram !

        debug("ovenContents : $ovenContents")
        if (ovenContents != null && canBeTakenOutOfOven(ovenContents)) {
            return takeItemOutOfOven(ovenContents)
        }

        if (playerState.mode == PlayerStateMode.WAITING) {
            val items = customers.flatMap { it.item.baseItems.toSet() - Item.DISH }.toSet()
            if (items.contains(Item.TART)) {
                val stepNode = StepNode(Step.GetSome(Item.TART))
                debug("stepNode = $stepNode")
                val stepNodeExpander = StepNodeExpander(gameState)
                val expandedNode = stepNodeExpander.expand(stepNode)
                debug("expandedNode = $expandedNode")
            }
            debug("items : $items")
        }

        return Action.Wait("What to do ?") // TODO
    }

}

data class PlayerState(
    val mode: PlayerStateMode = PlayerStateMode.WAITING,
    val remainingSteps: List<Step> = emptyList(),
) {


}

enum class PlayerStateMode {
    WAITING,
    TAKING_ITEM_OUT_OF_OVEN,
}

data class StepNode(val step: Step, val children: Set<StepNode> = emptySet()) {
    val hasChildren get() = children.isNotEmpty()
    val isLeaf get() = children.isEmpty()
}

class StepNodeExpander(val gameState: GameState) {
    fun expand(node: StepNode): StepNode {
        if (node.hasChildren) return node

        val step = node.step
        if (step is Step.GetSome) {
            if (gameState.contains(step.item)) {
                return node
            } else {
                // TODO plusieurs combinaison possible
            }
        }

        TODO("Cannot expand node $node")
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
        val dropPlayerItemAction = if (player.item != null) dropPlayerItem("Drop item to be fast") else null
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
            if (player.item!!.isNotCompatibleWith(customer.item)) {
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
                                distanceFromPlayerTo(item)
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

class ActionsResolverInspiredByTeccles(gameState: GameState) : ActionsResolver(gameState) {
    override fun nextAction(): Action {

        val sortedCustomersWithDishes = allCustomersWithAllDishes.sortedWith(Comparator
            .comparing<CustomerWithDish?, Int?> { customerWithDish ->
                customerWithDish.missingItemsInGame.size
            }
            .thenComparing(Comparator.comparing<CustomerWithDish?, Int?> { customerWithDish ->
                customerWithDish.customer.award
            }.reversed())
        )
        debug(sortedCustomersWithDishes)
        sortedCustomersWithDishes.forEach {
            debug("---")
            debug(it.missingItemsInGame)
        }

        val firstCustomerWithDish = sortedCustomersWithDishes.firstOrNull() ?: return Action.Wait("No more customers")
        return serve(firstCustomerWithDish)
    }

    private fun serve(customerWithDish: CustomerWithDish): Action {

        val sortedMissingItems = customerWithDish.missingItemsInGame.sortedWith(kotlin.Comparator
            .comparing { missingItem -> missingItem.baseItems.size } // DEBUG juste pour DEBUG
        )

        val firstMissingItem = sortedMissingItems.firstOrNull() ?: return assembleFor(customerWithDish)
        return prepare(firstMissingItem)
    }

    private fun prepare(item: Item): Action {
        val stepsToPrepare = cookBook.stepsToPrepare(item)
        val lastDoneStepIndex = stepsToPrepare.indexOfLast { step -> step.isDone(gameState) }
        val nextStep =
            if (lastDoneStepIndex < stepsToPrepare.size - 1) stepsToPrepare[lastDoneStepIndex + 1] else return Action.Wait(
                "Recipe completed ?!"
            )
        return when (nextStep) {
            is Step.GetSome -> if (gameState.contains(nextStep.item)) get(nextStep.item) else prepare(nextStep.item)
            is Step.Transform -> use(nextStep.equipment)
            is Step.PutInOven -> use(Equipment.OVEN)
            is Step.WaitForItemInOven -> if (player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) Action.Wait("Waiting for oven to bake ${nextStep.item}") else use(
                Equipment.OVEN,
                "Moving to oven"
            )

            is Step.GetFromOven -> use(Equipment.OVEN)
            else -> Action.Wait("Cannot translate step into actions : $nextStep")
        }
    }

    private fun get(item: Item, actionOnMissingBaseItem: (Item) -> Action = ::prepare): Action {
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

    private fun assembleFor(customerWithDish: CustomerWithDish): Action {

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
        }.sortedWith(Comparator.comparing {
            distanceFromPlayerTo(it)
        })

        debug("Remaining items to assemble :")
        debug(remainingItems)
        val nextItem = remainingItems.firstOrNull() ?: return useWindow
        return get(nextItem)
    }

}
