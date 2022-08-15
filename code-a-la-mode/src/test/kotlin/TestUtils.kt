import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class TestUtils {
    companion object {

        val ICE_CREAM = Item("ICE_CREAM")
        val BLUEBERRIES = Item("BLUEBERRIES")

        fun newInputStream(resourcesRelativePath: Path): InputStream {
            val resourcesPath = Path.of("src/test/resources")
            return Files.newInputStream(resourcesPath.resolve(resourcesRelativePath))
        }

        fun game(path: String): Game {
            val input = inputFrom(path)
            return input.nextGame()
        }

        fun gameState(path: String, game: Game = game(gamePathFromGameStatePath(path))): GameState {
            val input = inputFrom(path)
            return input.nextGameState(game)
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

    }
}
