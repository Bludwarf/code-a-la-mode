
import org.apache.commons.io.IOUtils
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

class TestUtils {
    companion object {

        val DISH = Item("DISH")

        fun newInputStream(resourcesRelativePath: Path): InputStream {
            val resourcesPath = Path.of("src/test/resources")
            return Files.newInputStream(resourcesPath.resolve(resourcesRelativePath))
        }

        fun newOutputStream(testsRelativePath: Path): OutputStream {
            val testsPath = Path.of("tests")
            return Files.newOutputStream(testsPath.resolve(testsRelativePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }

        fun game(path: String): Game {
            val input = inputFrom(path)
            return input.nextGame()
        }

        fun gameState(path: String, game: Game = game(gamePathFromGameStatePath(path))): GameState {
            val input = inputFrom(path)
            return input.nextGameState(game).copy(
                createdByTests = true
            )
        }

        private fun gamePathFromGameStatePath(gameStatePath: String): String {
            val gamePathWithoutExtension = gameStatePath.substringBeforeLast("-state")
            val gamePathExtension = gameStatePath.substringAfterLast(".")
            return "$gamePathWithoutExtension.$gamePathExtension"
        }

        private fun inputFrom(path: String): Input {
            val inputStream = newInputStream(Path.of(path))
            val input = Input(Scanner(inputStream))
            return input
        }

        fun action(expectedActionString: String): Action {
            val inputStream = expectedActionString.byteInputStream()
            val input = Input(Scanner(inputStream))
            return input.nextAction()
        }

        fun writeTestResult(message: String) {
            val outputStream = newOutputStream(Path.of("results.txt"))
            IOUtils.write(message, outputStream, Charsets.UTF_8)
            IOUtils.write("\n", outputStream, Charsets.UTF_8)
        }

    }
}
