package com.codingame.codealamode.resolvers

import com.codingame.codealamode.*
import debug
import java.util.function.Function
import java.util.function.Predicate

class ActionsResolverWithSimulation(gameState: GameState, private val simulator: Simulator) :
    ActionsResolver(gameState) {

    override fun nextAction(): Action {

        if (ovenContentsHasBeenByPutBy(player)) {
            return watchCook()
        }

        if (itemBeingPrepared != null) {
            return ActionsResolverToPrepare(itemBeingPrepared!!, gameState).nextAction()
        }

        if (itemThatWillBurnInOven != null && fastestChefToGetFromOven(itemThatWillBurnInOven!!) != partner) {
            return takeItemOutOfOven(itemThatWillBurnInOven!!)
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

                if (isReadyToBeServedOrAssembled(customer)) {
                    if (chefMustBeJoinedToServeOrAssemble(customer)) {
                        return givePlayerItemToPartner("Drop item to let partner serve or assemble ${customer.index}")
                    } else if (partnerIsIdle && fastestChefToServe(customer) == partner) {
                        debug("Partner will serve or assemble for ${customer.index}")
                        partnerIsIdle = false
                    } else if (customer canBeServedBy player) {
                        return serveOrAssemble(customer)
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
                                return prepare(item)
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
    private fun chefMustBeJoinedToServeOrAssemble(customer: Customer): Boolean {
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

    private fun serveOrAssemble(customer: Customer): Action {
        debug("Serve or assemble for customer $customer")

        val readyToBeServed = customer.withAllDishes.filter { it.missingItemsInDish.isEmpty() }

        if (readyToBeServed.isEmpty()) {
            // TODO fastest chef to assemble
            val fastestCustomerWithDishToAssemble = customer.withAllDishes
                .filter { it.missingItemsInDish.isNotEmpty() }
                .firstOrNull() ?: return Action.Wait("No more dishes to assemble") // TODO trouver d'autres dish
            return ActionsResolverToAssemble(fastestCustomerWithDishToAssemble, gameState).nextAction()
        } else {
            val fastestCustomerWithDishToServe = customer.withAllDishes.minByOrNull { countTurnsToServe(it) }
                ?: return Action.Wait("No more dishes to serve") // TODO trouver d'autres dish
            return ActionsResolverToServe(fastestCustomerWithDishToServe, gameState).nextAction()
        }
    }

    private fun countTurnsToServe(customerWithDish: CustomerWithDish): Int {
        val customerIsNotServed: (t: GameState) -> Boolean = { it.customers.contains(customerWithDish.customer) }
        val finalState = simulator.simulateWhile(gameState,
            customerIsNotServed,
            Function { gameState -> ActionsResolverToServe(customerWithDish, gameState) }
        )
        if (customerIsNotServed(finalState)) {
            // FIXME ne devrait jamais arriver
            debug("$customerWithDish cannot be served")
            return Int.MAX_VALUE
        }
        val turns = gameState.turnsRemaining - finalState.turnsRemaining
        debug("$turns turn(s) to serve $customerWithDish")
        return turns
    }

    private fun isReadyToBeServedOrAssembled(customer: Customer): Boolean =
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
            { ActionsResolverToPrepare(item, it) }
        )

    private fun fastestChef(
        toDoSomething: String,
        winCondition: Predicate<Chef>,
        actionsResolverSupplier: java.util.function.Function<GameState, ActionsResolver>,
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

    private fun prepare(item: Item): Action {
        debug("Prepare ${item.name}")
        return ActionsResolverToPrepare(item, gameState).nextAction() // TODO cache
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
