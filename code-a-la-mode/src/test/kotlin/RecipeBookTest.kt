
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RecipeBookTest {

    @Test
    fun actionsToPrepare() {
        val recipeBook = RecipeBook()
        val itemToPrepare = Item("CHOPPED_STRAWBERRIES")

        val actions = recipeBook.stepsToPrepare(itemToPrepare)

        assertThat(actions).containsExactly(Step.GetSome(Item("STRAWBERRIES")))
    }
}
