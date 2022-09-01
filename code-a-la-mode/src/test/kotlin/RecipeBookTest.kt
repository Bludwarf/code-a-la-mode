
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RecipeBookTest {

    @Test
    fun stepsToPrepareBaseItem() {
        val recipeBook = RecipeBook()
        val itemToPrepare = Item.CHOPPED_STRAWBERRIES

        val actions = recipeBook.stepsToPrepareBaseItem(itemToPrepare)

        assertThat(actions).containsExactly(
            Step.GetSome(Item.STRAWBERRIES),
            Step.Transform(Item.STRAWBERRIES, Equipment.CHOPPING_BOARD, Item.CHOPPED_STRAWBERRIES),
        )
    }
}
