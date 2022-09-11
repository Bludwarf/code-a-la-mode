package com.codingame.codealamode

import com.codingame.codealamode.TestUtils.Companion.game
import com.codingame.codealamode.resolvers.ActionsResolverWithSimulation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.function.Function

class CheckPlayerScoreTest {

    // FIXME : le simulateur ne donne pas les mêmes résultats que le jeu
    @ParameterizedTest
    @CsvSource(
        "ligueBronze/game-7942577706886182900.txt, 1554, 1553", // FIXME on devrait avoir 1554, 1554
        "ligueBronze/game--8170461584516249600.txt, 3765, 3767", // FIXME on devrait avoir 3765, 3765
        "ligueBronze/game--2553030406430916600.txt, 0, 0", // TODO nulos !
        "ligueBronze/game--3228865474660574200.txt, 0, 0",
        "ligueBronze/game--5458706346828992500.txt, 1423, 6588", // FIXME on devrait avoir 1423, 1423
        "ligueBronze/game--501847471512625220.txt, 0, 5825",  // FIXME on devrait avoir 0, 0
        "ligueBronze/game--2174831961734777090.txt, 2671, 2673", // FIXME on devrait avoir 2877, 2877
        "ligueBronze/game-3826859358225928200.txt, 7059, 7057", // TODO devrait donner 7067, 7067
        "ligueBronze/game--3442803331398166000.txt, 0, 0", // TODO nulos !
    )
    fun checkPlayerScore(gamePath: String, minimumScoreAsPlayer: Int, minimumScoreAsPartner: Int) {
        checkPlayerScore(gamePath, minimumScoreAsPlayer, spawnAsPlayer = true)
        checkPlayerScore(gamePath, minimumScoreAsPartner, spawnAsPlayer = false)
    }

    private fun checkPlayerScore(gamePath: String, minimumScore: Int, spawnAsPlayer: Boolean): Int {
        val game = game(gamePath)
        val spawnPositions: Array<Position> = if (spawnAsPlayer) game.spawnPositions else (game.spawnPositions.copyOf().also { it.reverse()})
        val initialGameState = GameState(
            game = game,
            turnsRemaining = 199,
            player = Chef("Player", spawnPositions[0]),
            partner = Chef("Partner", spawnPositions[1]),
            createdByTests = true,
        )

        val simulator = Simulator()

        val finalGameState = simulator.simulateWhile(
            initialGameState,
            { true },
            Function { gameState -> ActionsResolverWithSimulation(gameState, Simulator()) },
            true,
        )

        TestUtils.writeTestResult("Game \"$gamePath\" : Player score as ${if (spawnAsPlayer) "player" else "partner"} : ${finalGameState.playerScore}")
        assertThat(finalGameState.playerScore).isGreaterThanOrEqualTo(minimumScore)

        return finalGameState.playerScore
    }

}
