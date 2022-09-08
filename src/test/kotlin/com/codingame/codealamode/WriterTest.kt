package com.codingame.codealamode

import Writer
import com.codingame.codealamode.TestUtils.Companion.game
import com.codingame.codealamode.TestUtils.Companion.gameState
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

internal class WriterTest {

    @Test
    // TODO factoriser avec writeGameState
    fun writeGame() {
        val gamePath = Path.of("ligue2/game-2362403142607370200.txt")
        val game = game(gamePath.toString())
        val out = ByteArrayOutputStream()

        out.use {
            val writer = Writer(out)
            writer.use {
                writer.write(game)
            }
        }

        val expected = Files.readString(Path.of("src/test/resources/").resolve(gamePath))
        Assertions.assertThat(out.toString().trim()).isEqualTo(expected.trim())
    }

    @Test
    fun writeGameState() {
        val gameStatePath = Path.of("ligue2/game-2362403142607370200-state-1.txt")
        val gameState = gameState(gameStatePath.toString())
        val out = ByteArrayOutputStream()

        out.use {
            val writer = Writer(out)
            writer.use {
                writer.write(gameState)
            }
        }

        val expected = Files.readString(Path.of("src/test/resources/").resolve(gameStatePath))
        Assertions.assertThat(out.toString().trim()).isEqualTo(expected.trim())
    }
}
