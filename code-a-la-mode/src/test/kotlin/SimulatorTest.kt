
import Item.Companion.ICE_CREAM
import Item.Companion.NONE
import TestUtils.Companion.DISH
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
        assertThat(gameState2.player.item).isNull()

        val gameState3 = simulator.simulate(gameState2, Action.Use(Position(5, 0)))
        assertThat(gameState3.player.position).isEqualTo(Position(4, 1))
        assertThat(gameState3.player.item).isNull()

        val gameState4 = simulator.simulate(gameState3, Action.Use(Position(5, 0)))
        assertThat(gameState4.player.position).isEqualTo(Position(4, 1))
        assertThat(gameState4.player.item).isEqualTo(DISH)

    }

    @Test
    fun simulate_state7() {
        val gameState7 = gameState("ligue2/game-2362403142607370200-state-7.txt")

        val simulator = Simulator()
        val action = Action.Use(Position(5, 3))

        val gameState8 = simulator.simulate(gameState7, action)
        assertThat(gameState8.player.position).isEqualTo(Position(1, 2))
        assertThat(gameState8.player.item).isEqualTo(DISH)

        val gameState9 = simulator.simulate(gameState8, action)
        assertThat(gameState9.player.position).isEqualTo(Position(2, 5))
        assertThat(gameState9.player.item).isEqualTo(DISH)

        val gameState10 = simulator.simulate(gameState9, action)
        assertThat(gameState10.player.position).isEqualTo(Position(4, 4))
        assertThat(gameState10.player.item).isEqualTo(DISH)

        val gameState11 = simulator.simulate(gameState10, action)
        assertThat(gameState11.player.position).isEqualTo(Position(4, 4))
        assertThat(gameState11.player.item).isEqualTo(DISH.with(ICE_CREAM))

    }

    @Test
    fun simulate_state23() {
        val gameState23 = gameState("ligue2/game-2362403142607370200-state-23.txt")

        val simulator = Simulator()
        val action = Action.Use(gameState23.kitchen.getPositionOf(Equipment.WINDOW))

        val gameState24 = simulator.simulate(gameState23, action)
        assertThat(gameState24.player.item).isNull()
        assertThat(gameState24.player.position).isEqualTo(gameState23.player.position)
        assertThat(gameState24.customers).hasSize(gameState23.customers.size - 1)

    }

}
