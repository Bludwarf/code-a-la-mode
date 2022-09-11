import com.codingame.codealamode.*
import com.codingame.codealamode.Timer
import com.codingame.codealamode.resolvers.BestActionResolver
import java.util.*

private const val PRINT_GAME = true
private const val PRINT_GAME_STATE = true

val equipmentMapper = EquipmentMapper()
var debugEnabled = true
var debugIndent = ""
var debugSimulation = false

// https://www.codingame.com/ide/puzzle/code-a-la-mode
/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main() {
    debug(System.currentTimeMillis())
    val input = Input(Scanner(System.`in`))
    val game = input.nextGame()
    val bestActionResolver = BestActionResolver()
    val writer = Writer(System.err)

    if (PRINT_GAME) {
        System.err.print("Game : ")
        writer.write(game)
        writer.flush()
        System.err.println()
    }

    // game loop
    while (true) {
        val gameState = input.nextGameState(game)
        val timer = Timer()

        if (PRINT_GAME_STATE) {
            System.err.print("GameState : ")
            writer.write(gameState)
            writer.flush()
            System.err.println()
        }

        val action = try {
            bestActionResolver.resolveBestActionFrom(gameState)
        } catch (e: Throwable) {
            Action.Wait("Error : ${e.message}")
            debug(e)
        }

        debug("Δt = $timer")
        println(action)
    }
}

fun debug(message: Any) {
    if (debugEnabled) System.err.println(debugIndent + message.toString())
}

fun debug(t: Throwable) {
    t.printStackTrace(System.err)
}

fun debug(elements: Collection<Any>) {
    if (debugEnabled) elements.forEach { debug("- $it") }
}

fun debug(titleMessage: Any, elements: Collection<Any>) {
    if (debugEnabled) {
        debug("$titleMessage :")
        elements.forEach { debug("- $it") }
    }
}

fun nextPairNumber(x: Int): Int {
    return when (x % 2) {
        0 -> x
        else -> x + 1
    }
}
