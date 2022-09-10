package com.codingame.codealamode

import com.codingame.codealamode.TestUtils.Companion.game
import com.codingame.codealamode.resolvers.ActionsResolverWithSimulation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.function.Function

class CheckPlayerScoreTest {

    @ParameterizedTest
    @CsvSource(
        "ligue2/game-2362403142607370200.txt, 2750, 0", // TODO nulos !
        "ligue3/game-7942577706886182900.txt, 1700, 1700",
        "ligueBronze/game-7942577706886182900.txt, 1700, 1700",
        "ligueBronze/game--8170461584516249600.txt, 2050, 2050",
        "ligueBronze/game--2553030406430916600.txt, 0, 0", // TODO nulos ! // TODO nulos !
        "ligueBronze/game--3228865474660574200.txt, 0, 3700", // TODO nulos !
        "ligueBronze/game--5458706346828992500.txt, 0, 5850", // TODO nulos !
        "ligueBronze/game--501847471512625220.txt, 0, 2500", // TODO nulos !
        "ligueBronze/game--2174831961734777090.txt, 2050, 2050",
        "ligueBronze/game-3826859358225928200.txt, 0, 2300", // TODO nulos !
        "ligueBronze/game--3442803331398166000.txt, 2500, 2500",
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
