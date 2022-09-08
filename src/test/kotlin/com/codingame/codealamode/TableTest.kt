package com.codingame.codealamode

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class TableTest {

    @Test
    fun testEquals() {
        val table1 = Table(Position(2, 2), Item.CHOPPED_STRAWBERRIES)
        val table2 = Table(Position(2, 2), null)
        Assertions.assertThat(table1).isEqualTo(table2)
    }
}
