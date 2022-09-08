package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.GameState
import com.codingame.codealamode.Item

class ActionsResolverToHelpPartnerToPrepare(
    private val customerWithDish: CustomerWithDish,
    private val lastMissingItemPreparedByPartner: Item,
    gameState: GameState,
) : ActionsResolver(gameState) {

    private val actionsResolverToAssemble: ActionsResolverToAssemble =
        ActionsResolverToAssemble(customerWithDish, gameState)

    override fun nextAction(): Action {
        // TODO Est-ce que je l'aide à préparer ou je l'aide à assembler ? Pour le moment on assemble
        return actionsResolverToAssemble.nextAction()
    }

}
