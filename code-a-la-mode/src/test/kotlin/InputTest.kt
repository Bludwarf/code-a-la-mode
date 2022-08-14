

import TestUtils.Companion.newInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.*

internal class InputTest {

    @Test
    fun nextGame() {
        val inputStream = newInputStream(Path.of("ligue1/game-1"))
        val input = Input(Scanner(inputStream))

        val game = input.nextGame()

        val kitchen = game.kitchen
        assertThat(kitchen.getPositionOf(Equipment.DISHWASHER)).isEqualTo(Position(5, 0))
        assertThat(kitchen.getPositionOf(Equipment.ICE_CREAM_CRATE)).isEqualTo(Position(0, 1))
        assertThat(kitchen.getPositionOf(Equipment.BLUBERRIES_CRATE)).isEqualTo(Position(5, 4))
        assertThat(kitchen.getPositionOf(Equipment.WINDOW)).isEqualTo(Position(5, 6))
    }

    @Test
    fun nextGameState() {
        val inputStream = newInputStream(Path.of("ligue1/game-state-1"))
        val input = Input(Scanner(inputStream))

        val gameState = input.nextGameState()

        assertThat(gameState.customers).hasSize(3)
        assertThat(gameState.tablesWithItem).isEmpty()
        assertThat(gameState.player.position).isEqualTo(Position(1,1))
    }

}
