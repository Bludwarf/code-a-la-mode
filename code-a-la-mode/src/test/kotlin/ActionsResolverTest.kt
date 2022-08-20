import TestUtils.Companion.game
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

const val USE_OVEN = "USE 0 5"
const val DROP_ITEM = "USE 2 2"
const val USE_DOUGH_CRATE = "USE 8 3"

internal class ActionsResolverTest {

    @ParameterizedTest
    @CsvSource(
        "DISH-CROISSANT, NONE, $USE_DOUGH_CRATE",
        "DISH-CROISSANT, DOUGH, $USE_OVEN",
        "DISH-BLUEBERRIES, DOUGH, $DROP_ITEM",
    )
    fun resolveBestActionWithDough(customerItemName: String, playerItemName: String, expectedActionCommand: String) {
        val game = game("ligue3/game-7942577706886182900.txt")
        val customer = Customer(Item(customerItemName), 1200)
        val player = Chef(Position(1, 3), if (playerItemName != "NONE") Item(playerItemName) else null)
        val gameState = GameState(game, 200, player, Chef(Position(9, 3)), emptySet(), listOf(customer))
        val actionsResolver = ActionsResolver(gameState)

        val action = actionsResolver.nextActionFrom(gameState)

        assertThat(action.command).isEqualTo(expectedActionCommand)
    }
}
