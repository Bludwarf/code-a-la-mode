package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.Equipment
import com.codingame.codealamode.GameState
import com.codingame.codealamode.Item

class ActionsResolverToGetFromOven(
    private val itemToGetFromOven: Item,
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // TODO c'est ici qu'on peut faire des actions en attendant la cuisson, aussi ?
        if (player.item != null && player.isNextTo(kitchen.getPositionOf(Equipment.OVEN))) {
            if (customers.any { customer -> customer.item == player.item + itemToGetFromOven }) {
                return use(Equipment.OVEN, "Add ${itemToGetFromOven.name} to dish before it burns!")
            }
            return dropPlayerItem("Drop item to get ${itemToGetFromOven.name} before it burns!")
        }
        return use(Equipment.OVEN, "Run to oven to get ${itemToGetFromOven.name} before it burns!")
    }

}
