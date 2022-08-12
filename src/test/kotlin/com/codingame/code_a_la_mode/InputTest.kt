package com.codingame.code_a_la_mode

import Input
import Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.io.ByteArrayInputStream
import java.util.*

internal class InputTest {

    @Test
    fun nextGame() {
        val inputStream = ByteArrayInputStream(
            listOf(
                listOf(
                    "3", // numAllCustomers
                    "DISH-ICE_CREAM-BLUEBERRIES", // customerItem
                    "150", // customerAward
                    "DISH-BLUEBERRIES-ICE_CREAM", // customerItem
                    "200", // customerAward
                    "DISH-BLUEBERRIES-ICE_CREAM", // customerItem
                    "300", // customerAward
                ).joinToString(" "),

                // Kitchen
                "#####D#####",
                "I.........#",
                "#.####.##.#",
                "#.#..#..#.#",
                "#.##.B###.#",
                "#.........#",
                "#####W#####",
            ).joinToString("\n").toByteArray()
        )
        val input = Input(Scanner(inputStream))

        val game = input.nextGame()

        val kitchen = game.kitchen
        assertThat(kitchen.getPositionOf(Equipment.DISHWASHER)).isEqualTo(Position(5, 0))
        assertThat(kitchen.getPositionOf(Equipment.ICE_CREAM_CRATE)).isEqualTo(Position(0, 1))
        assertThat(kitchen.getPositionOf(Equipment.BLUBERRIES_CRATE)).isEqualTo(Position(5, 4))
        assertThat(kitchen.getPositionOf(Equipment.WINDOW)).isEqualTo(Position(5, 6))
    }

    @Test
    fun nextGameState() {
        val inputStream = ByteArrayInputStream(
            listOf(
                listOf(
                    "999", // turnsRemaining

                    // player
                    "1", // x
                    "1", // y
                    "NONE", // item

                    // partner
                    "8", // x
                    "1", // y
                    "NONE", // item

                    "0", // the number of tables in the kitchen that currently hold an item

                    "NONE", // ovenContents
                    "0", // ovenTimer

                    // customers
                    "3", // numAllCustomers

                    "DISH-ICE_CREAM-BLUEBERRIES", // customerItem
                    "150", // customerAward

                    "DISH-BLUEBERRIES-ICE_CREAM", // customerItem
                    "200", // customerAward

                    "DISH-BLUEBERRIES-ICE_CREAM", // customerItem
                    "300", // customerAward

                ).joinToString(" "),
            ).joinToString("\n").toByteArray()
        )
        val input = Input(Scanner(inputStream))

        val gameState = input.nextGameState()

        assertThat(gameState.customers).hasSize(3)
        assertThat(gameState.tablesWithItem).isEmpty()
        assertThat(gameState.player.position).isEqualTo(Position(1,1))
    }

}
