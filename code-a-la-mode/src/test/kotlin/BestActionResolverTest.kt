

import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BestActionResolverTest {

    @Test
    fun resolveBestAction() {
        val gameState = gameState("ligue2/game-2362403142607370200-state-1.txt")
        val bestActionResolver = BestActionResolver(PossibleActionResolverV2(gameState))

        val action = bestActionResolver.resolveBestActionFrom(gameState)

        val kitchen = gameState.kitchen
        assertThat(action).isEqualTo(Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER)))
    }
}
