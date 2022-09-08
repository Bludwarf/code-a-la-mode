package com.codingame.codealamode.resolvers

import com.codingame.codealamode.*

class ActionsResolverToPrepare(
    private val itemToPrepare: Item,
//    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action = prepare()

    private fun prepare(
        item: Item = itemToPrepare,
//        customerWithDish: CustomerWithDish = this.customerWithDish,
    ): Action {

        val stepsToPrepare = cookBook.stepsToPrepare(item)
        val lastDoneStepIndex = stepsToPrepare.indexOfLast { step -> step.isDone(gameState) }
        val nextStep =
            if (lastDoneStepIndex < stepsToPrepare.size - 1) stepsToPrepare[lastDoneStepIndex + 1] else return Action.Wait(
                "Recipe completed ?!"
            )
        return when (nextStep) {
            is Step.GetSome -> get(nextStep.item) { prepare(nextStep.item) }
            is Step.Transform -> use(nextStep.equipment)
            is Step.PutInOven -> use(Equipment.OVEN)
            is Step.WaitForItemInOven -> doWhileWaitingItemInOven(item)

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

    private fun doWhileWaitingItemInOven(item: Item): Action {
        if (player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) {
            // TODO si on peut chopper une assiette sans gêner le partner et sans faire bruler, il faut le faire
            return Action.Wait("Waiting for oven to bake $item")
        } else {
            return use(Equipment.OVEN, "Moving to oven")
        }
    }

}
