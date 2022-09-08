package com.codingame.codealamode

data class Path(val positions: List<Position>) {
    fun minDistanceWith(target: Position): Double {
        return positions.minOf { position -> position.straightDistanceWith(target) }
    }

    val end: Position get() = positions.last()
    val length: Int get() = positions.size - 1

    fun subPath(maxDistance: Int): Path {
        return copy(
            positions = positions.subList(0, positions.size.coerceAtMost(maxDistance + 1))
        )
    }

    fun lastOrNextOf(targetPosition: Position): Position {
        return positions.firstOrNull { position -> position.isNextTo(targetPosition) } ?: positions.last()
    }
}
