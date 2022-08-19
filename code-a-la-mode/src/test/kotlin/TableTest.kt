import org.assertj.core.api.Assertions

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

internal class TableTest {

    @Test
    fun testEquals() {
        val table1 = Table(Position(2, 2), Item.CHOPPED_STRAWBERRIES)
        val table2 = Table(Position(2, 2), null)
        Assertions.assertThat(table1).isEqualTo(table2)
    }
}
