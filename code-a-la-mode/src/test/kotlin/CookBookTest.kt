
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CookBookTest {

    @Test
    fun stepsToPrepareBaseItem() {
        val cookBook = CookBook()
        val itemToPrepare = Item.CHOPPED_STRAWBERRIES

        val actions = cookBook.stepsToPrepareBaseItem(itemToPrepare)

        assertThat(actions).containsExactly(
            Step.GetSome(Item.STRAWBERRIES),
            Step.Transform(Item.STRAWBERRIES, Equipment.CHOPPING_BOARD, Item.CHOPPED_STRAWBERRIES),
        )
    }
}
