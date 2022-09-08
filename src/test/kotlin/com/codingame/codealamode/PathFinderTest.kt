package com.codingame.codealamode

import com.codingame.codealamode.TestUtils.Companion.gameState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PathFinderTest {

    @Test
    fun possiblePaths() {
        val gameState7 = gameState("ligue2/game-2362403142607370200-state-7.txt")
        val pathFinder = PathFinder(gameState7)
        val possiblePaths = pathFinder.possiblePaths(gameState7.player.position, Position(5, 3))
        assertThat(possiblePaths).hasSize(1)
    }

    @Test
    fun findPath() {
        val gameState7 = gameState("ligue2/game-2362403142607370200-state-7.txt")
        val pathFinder = PathFinder(gameState7)
        val path = pathFinder.findPath(gameState7.player.position, Position(5, 3))
        assertThat(path).isNotNull
        assertThat(path!!.positions).contains(
            Position(3, 1),
            Position(4, 4)
            )
        assertThat(path.positions).doesNotContain(
            Position(5, 1),
            Position(5, 5)
        )
    }
}
