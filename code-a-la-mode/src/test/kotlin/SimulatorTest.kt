import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class SimulatorTest {

    @Test
    fun simulate_state1() {
        val gameState1 = gameState("ligue2/game-2362403142607370200-state-1.txt")

        val simulator = Simulator()

        val gameState2 = simulator.simulate(gameState1, Action.Use(Position(5, 0)))
        assertThat(gameState2.player.position).isEqualTo(Position(3, 1))
        assertThat(gameState2.player.item).isEqualTo(Item.NONE)

        val gameState3 = simulator.simulate(gameState2, Action.Use(Position(5, 0)))
        assertThat(gameState3.player.position).isEqualTo(Position(4, 1))
        assertThat(gameState3.player.item).isEqualTo(Item.NONE)

        val gameState4 = simulator.simulate(gameState3, Action.Use(Position(5, 0)))
        assertThat(gameState4.player.position).isEqualTo(Position(4, 1))
        assertThat(gameState4.player.item).isEqualTo(Item.DISH)

    }

}
