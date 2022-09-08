package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.Customer
import com.codingame.codealamode.GameState
import com.codingame.codealamode.Item
import debug

class ActionsResolverToServe(
    private val customerWithDish: CustomerWithDish,
    gameState: GameState,
) : ActionsResolver(gameState) {

    private val customer: Customer by lazy(customerWithDish::customer)

    override fun nextAction(): Action {
        debug("Serve $customerWithDish")

        return if (player.item != customer.item) get(customer.item)
        else useWindow
    }

    private fun get(item: Item): Action {
        val tableWithItem = gameState.findTableWith(item)
        if (tableWithItem != null) {
            return use(tableWithItem)
        }
        return Action.Wait("Waiting for item $item to serve $customerWithDish")
    }

}
