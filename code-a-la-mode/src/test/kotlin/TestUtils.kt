import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class TestUtils {
    companion object {
        fun newInputStream(resourcesRelativePath: Path): InputStream {
            val resourcesPath = Path.of("src/test/resources")
            return Files.newInputStream(resourcesPath.resolve(resourcesRelativePath))
        }
    }
}
