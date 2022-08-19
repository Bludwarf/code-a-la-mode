

import TestUtils.Companion.action
import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class BestActionResolverTest {

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200-state-1.txt, USE 8 3, Use STRAWBERRIES_CRATE",
        "ligue2/game-2362403142607370200-state-7.txt, USE 5 6, dropPlayerItem",
        "ligue2/game-2362403142607370200-state-45.txt, USE 8 4, \"Got some CHOPPED_STRAWBERRIES on table Table(position=8 4, item=Item(name=CHOPPED_STRAWBERRIES))\"",
        "ligue3/game-7942577706886182900-state-28.txt, USE 9 0, Use STRAWBERRIES_CRATE",
        quoteCharacter = '"',
    )
    fun resolveBestAction(gameStatePath: String, expectedActionString: String, expectedActionComment: String) {
        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        val action = bestActionResolver.resolveBestActionFrom(gameState)

        val expectedAction = action(expectedActionString)
        assertThat(action).isEqualTo(expectedAction)
        assertThat(action.comment).isEqualTo(expectedActionComment)
    }

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200-state-1.txt",
    )
    fun resolveBestActionFastEnough(gameStatePath: String) {
        val currentTimestamp = System.currentTimeMillis()

        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        val action = bestActionResolver.resolveBestActionFrom(gameState)

        assertThat(System.currentTimeMillis())
            .`as`("Maximum response time is <= 1 second")
            .isLessThanOrEqualTo(currentTimestamp + 1000)
    }

}
