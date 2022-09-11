package com.codingame.game

import com.codingame.game.model.Cell
import com.codingame.gameengine.core.AbstractMultiplayerPlayer

const val WALK_DISTANCE = 9

class Player : AbstractMultiplayerPlayer() {

    fun moveTo(cell: Cell): List<Cell> {
        val blockedCell = if (partner.isActive) partner.location else null

        val (fromSource, traceBack) = location.buildDistanceMap(blockedCell)
        val target =
            if (!cell.isTable && cell != blockedCell)
                cell
            else
                cell.neighbours.map { it.first }
                    .filter { !it.isTable }
                    .filter { it in fromSource.keys }
                    .minByOrNull { fromSource[it]!! } ?: return moveTo(blockedCell!!)

        if (target !in fromSource.keys) {
            System.err.println("Warning: cannot move! Moving to partner location instead")
            return moveTo(blockedCell!!)
        }

        if (location.distanceTo(target, blockedCell)!! <= WALK_DISTANCE) { // TODO on a du ajouter !! est-ce OK ?
            location = target
            return trace(cell, location, traceBack)
        }

        val fromTarget = target.buildDistanceMap(blockedCell).distances

        location = fromSource
            .filter { (cell, dist) -> dist <= WALK_DISTANCE && !cell.isTable }
            .minByOrNull { (cell, _) -> fromTarget[cell]!! }!!
            .key

        return trace(cell, location, traceBack)
    }

    private fun trace(source: Cell, target: Cell, traceBack: Map<Cell, Cell>): List<Cell> {
        return sequence {
            var cur = target
            yield(target)
            while (cur != source) {
                cur = traceBack[cur] ?: break
                yield(cur)
            }
        }.toList().reversed()

    }

    lateinit var location: Cell
    lateinit var partner: Player

}
