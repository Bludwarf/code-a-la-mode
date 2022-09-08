package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.GameState
import com.codingame.codealamode.Simulator

class BestActionResolver {

    private val simulator = Simulator()

    fun resolveBestActionFrom(gameState: GameState): Action {
        val actionsResolver: ActionsResolver = ActionsResolverWithSimulation(gameState, simulator)
        return actionsResolver.nextAction()
    }

}
