package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.GameState
import com.codingame.codealamode.Item
import com.codingame.codealamode.Oven
import debug
import nextPairNumber
import kotlin.math.ceil

class ActionsResolverToAssemble(
    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {

    override fun nextAction(): Action {
        return assembleFor(customerWithDish)
    }

    private fun assembleFor(customerWithDish: CustomerWithDish): Action {
        debug("Assembling for $customerWithDish")

        if (partner.item == customerWithDish.customer.item) return dropPlayerItem("Drop dish to let partner serve $customerWithDish")

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
