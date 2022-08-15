

import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PossibleActionResolverV1Test {

    @Test
    fun computeNextPossibleActions() {
        val gameState = gameState("ligue2/game-2362403142607370200-state-1.txt")
        val possibleActionResolver = PossibleActionResolverV1(gameState)

        val nextPossibleActions = possibleActionResolver.computeNextPossibleActions()

        val kitchen = gameState.kitchen
        assertThat(nextPossibleActions).containsExactly(
            Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER))
        )
    }

}
