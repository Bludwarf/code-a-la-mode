
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ItemTest {

    @ParameterizedTest(name = "{0} {1} : {2}")
    @CsvSource(
        "DISH, DISH, true",
        "DISH, BLUEBERRIES, false",
        "DISH-ICE_CREAM, BLUEBERRIES, false",
        "DISH-ICE_CREAM, DISH-BLUEBERRIES, false",
        "DISH-BLUEBERRIES-ICE_CREAM, DISH-BLUEBERRIES, false",
        "DISH-BLUEBERRIES-ICE_CREAM, DISH-ICE_CREAM-BLUEBERRIES, true",
    )
    fun testEquals(item1Name: String, item2Name: String, expected: Boolean) {
        val item1 = Item(item1Name)
        val item2 = Item(item2Name)
        assertThat(item1 == item2).isEqualTo(expected)
    }
}
