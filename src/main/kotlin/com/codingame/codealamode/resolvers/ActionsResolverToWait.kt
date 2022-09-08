package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.GameState

class ActionsResolverToWait(gameState: GameState) : ActionsResolver(gameState) {
    private val wait = Action.Wait("Only waiting...")
    override fun nextAction() = wait
}
