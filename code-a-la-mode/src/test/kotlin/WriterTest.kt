

import TestUtils.Companion.gameState
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

internal class WriterTest {

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
