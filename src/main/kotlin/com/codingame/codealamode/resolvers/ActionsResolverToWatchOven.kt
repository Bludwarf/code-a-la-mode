package com.codingame.codealamode.resolvers

import com.codingame.codealamode.Action
import com.codingame.codealamode.Equipment
import com.codingame.codealamode.GameState

class ActionsResolverToWatchOven(
    gameState: GameState,
) : ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // TODO c'est ici qu'on peut faire des actions en attendant la cuisson !
        return use(Equipment.OVEN, "Watch oven")
    }

}
