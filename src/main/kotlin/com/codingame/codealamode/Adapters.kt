package com.codingame.codealamode

import com.codingame.game.Player
import com.codingame.game.model.Board
import com.codingame.game.model.Cell

class PlayerAdapter {
    private val cellAdapter = CellAdapter()

    fun adapt(chef: Chef, partnerChef: Chef, kitchen: Kitchen): Player {
        val player = adapt(chef, kitchen)
        val partner = adapt(partnerChef, kitchen)
        player.partner = partner
        return player;
    }

    private fun adapt(chef: Chef, kitchen: Kitchen): Player {
        val player = Player()
        player.location = cellAdapter.adapt(chef.position, kitchen)
        return player;
    }

}

class CellAdapter {
    private val boardAdapter = BoardAdapter()

    fun adapt(position: Position, kitchen: Kitchen): Cell {
        return try {
            val cellx = position.x
            val celly = position.y
            val board = boardAdapter.adapt(kitchen)
            board[cellx, celly]
        } catch (_: Exception) {
            throw Exception("Invalid position: $position")
        }
    }

    fun adapt(cell: Cell): Position {
        return Position(cell.x, cell.y)
    }
}

class BoardAdapter {
    val adaptedKitchens = mutableMapOf<Kitchen, Board>()
    fun adapt(kitchen: Kitchen): Board {
        if (!adaptedKitchens.containsKey(kitchen)) {
            val layout = kitchen.lines.map { line -> line.map { char ->
                when(char) {
                    '.', '0', '1' -> '.'
                    else -> '*'
                }
            }.joinToString("") }
            adaptedKitchens[kitchen] = Board(Position.MAX_X + 1, Position.MAX_Y + 1, layout)
        }
        return adaptedKitchens[kitchen]!!
    }
}
