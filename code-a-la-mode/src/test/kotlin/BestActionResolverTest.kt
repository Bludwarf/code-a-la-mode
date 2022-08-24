

import TestUtils.Companion.action
import TestUtils.Companion.gameState
import org.apache.commons.lang3.StringUtils
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
        "ligue3/game-7942577706886182900-state-88.txt, USE 0 3, Drop item to get CROISSANT before it burns!",
        "ligue3/game-7942577706886182900-state-193.txt, USE 0 4, dropPlayerItem",
        "ligueBronze/game-7942577706886182900-state-5.txt, USE 2 3, Use CHOPPING_BOARD",
        "ligueBronze/game-7942577706886182900-state-13.txt, USE 10 5, Use BLUEBERRIES_CRATE",
        "ligueBronze/game-7942577706886182900-state-33.txt, USE 0 5, Use OVEN",
        "ligueBronze/game-7942577706886182900-state-121.txt, USE 5 0, Use DISHWASHER",
        "ligueBronze/game-7942577706886182900-state-125.txt, USE 0 2, Use ICE_CREAM_CRATE",
        "ligueBronze/game-7942577706886182900-state-129.txt, USE 2 4, \"Got some TART on table Table(position=2 4, item=Item(name=TART))\"",
        "ligueBronze/game--8170461584516249600-state-37.txt, USE 3 4, \"Got some DISH-BLUEBERRIES-ICE_CREAM-CHOPPED_STRAWBERRIES on table Table(position=3 4, item=Item(name=DISH-BLUEBERRIES-ICE_CREAM-CHOPPED_STRAWBERRIES))\"", // On récupère l'assiette déjà existante qui est compatible avec le 3e client
        quoteCharacter = '"',
    )
    fun resolveBestAction(gameStatePath: String, expectedActionString: String, expectedActionComment: String) {
        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        val action = bestActionResolver.resolveBestActionFrom(gameState)

        val expectedAction = action(expectedActionString)
        assertThat(action).isEqualTo(expectedAction)
        if (StringUtils.isNoneBlank(expectedActionComment)) {
            assertThat(action.comment).isEqualTo(expectedActionComment)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200-state-1.txt",
    )
    fun resolveBestActionFastEnough(gameStatePath: String) {
        val currentTimestamp = System.currentTimeMillis()

        val gameState = gameState(gameStatePath)
        val bestActionResolver = BestActionResolver()

        bestActionResolver.resolveBestActionFrom(gameState)

        assertThat(System.currentTimeMillis())
            .`as`("Maximum response time is <= 1 second")
            .isLessThanOrEqualTo(currentTimestamp + 1000)
    }

}
