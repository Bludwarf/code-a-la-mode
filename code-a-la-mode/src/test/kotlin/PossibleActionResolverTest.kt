

import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PossibleActionResolverTest {

    @Test
    fun computeNextPossibleActions() {
        val gameState = gameState("ligue2/game-2362403142607370200-state-1.txt")
        val possibleActionResolver = PossibleActionResolverV2()

        val nextPossibleActions = possibleActionResolver.computeNextPossibleActions(gameState)

        val kitchen = gameState.kitchen
        assertThat(nextPossibleActions).containsExactly(
            Action.Use(kitchen.getPositionOf(Equipment.DISHWASHER)),
            Action.Use(kitchen.getPositionOf(kitchen.getEquipmentThatProvides(Item.ICE_CREAM))),
            Action.Use(kitchen.getPositionOf(kitchen.getEquipmentThatProvides(Item.BLUEBERRIES))),
            Action.Use(kitchen.getPositionOf(kitchen.getEquipmentThatProvides(Item.STRAWBERRIES))),
        )
    }

}
