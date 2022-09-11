package com.codingame.game.model

import java.util.*

class Cell(val x: Int, val y: Int, val isTable: Boolean = true) {
    constructor() : this(-1, -1)

    override fun toString(): String = "($x, $y)"
    private val straightNeighbours = mutableListOf<Cell>()
    private val diagonalNeighbours = mutableListOf<Cell>()

    val neighbours by lazy {
        straightNeighbours.map { it to 2 } +
                diagonalNeighbours.filter { it.isTable || isTable }.map { it to 3 }
    }

    fun connect(other: Cell, isStraight: Boolean) {
        (if (isStraight) straightNeighbours else diagonalNeighbours) += other
    }

    data class DistanceMap(val distances: Map<Cell, Int>, val traceBack: Map<Cell, Cell>)

    fun buildDistanceMap(blockedCell: Cell?): DistanceMap {
        val visitedCells = mutableMapOf<Cell, Int>()
        val traceBack = mutableMapOf<Cell, Cell>()
        val floodedCells = PriorityQueue<Pair<Cell, Int>> { (_, d1), (_, d2) -> d1.compareTo(d2) }
        floodedCells += this to 0
        var isFirst = true

        while (floodedCells.any()) {
            val (cell, dist) = floodedCells.remove()!!
            if (cell in visitedCells) continue
            visitedCells += cell to dist
            if ((!cell.isTable && cell != blockedCell) || isFirst) {
                floodedCells += cell.neighbours
                    .filterNot { (nc, _) -> nc == blockedCell }
                    .filterNot { (nc, _) -> nc in visitedCells.keys }
                    .map { (nc, nd) ->
                        traceBack[nc] = cell
                        nc to dist + nd
                    }
            }
            isFirst = false
        }
        return DistanceMap(visitedCells, traceBack)
    }

    fun distanceTo(target: Cell, partnerCell: Cell? = null): Int? {
        return buildDistanceMap(partnerCell).distances[target]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cell) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }


}

class Board(
    width: Int,
    height: Int,
    layout: List<String>? = null,
) {

    val cells = Array(width) { x ->
        Array(height) { y ->
            Cell(x, y, layout != null && layout[y][x] != '.')
        }
    }

    operator fun get(x: Int, y: Int): Cell = cells[x][y]
    operator fun get(cellName: String): Cell {
        val file = cellName[0]
        val x = file - 'A'
        if (x !in xRange) throw IllegalArgumentException("x: $x")
        return get(x, cellName.substring(1).toInt())
    }

    private val xRange = 0 until width
    private val yRange = 0 until height

    init {
        for (x in xRange) {
            for (y in yRange) {
                for (dx in -1..1) for (dy in -1..1) {
                    if (dx != 0 || dy != 0) {
                        val x2 = x + dx;
                        val y2 = y + dy
                        if (x2 in xRange && y2 in yRange) {
                            this[x, y].connect(this[x2, y2], dx * dy == 0)
                        }
                    }
                }
            }
        }
    }

}
