package com.codingame.codealamode

import com.codingame.codealamode.exceptions.CannotFindPathException

class PathFinder(private val gameState: GameState) {

    internal fun findPath(position: Position, target: Position): Path? {
        return possiblePaths(position, target)
            .sortedWith(
                Comparator.comparing { path -> path.length }
//                    .thenComparing { path -> path.length } // TODO si on veut faire du micro positionnement (comme teccles), il faut le faire dans le ActionResolver et pas dans le PathFinder
            )
            .firstOrNull()
    }

    // TODO cache
    internal fun possiblePaths(
        position: Position,
        target: Position,
        path: Path = Path(listOf(gameState.player.position)),
    ) = possiblePathsWhile(position, { !it.isNextTo(target) }, path)

    fun nextEmptyPositions(position: Position): Set<Position> {
        return position.adjacentPositions
            .filter(gameState::isEmpty)
            .toSet()
    }

    fun possiblePathsWhile(
        position: Position,
        whileCondition: (Position) -> Boolean,
        path: Path = Path(listOf(gameState.player.position)),
    ): List<Path> {
        if (!whileCondition(position)) {
            return listOf(path)
        }
        val nextPositions = nextEmptyPositions(position) - path.positions.toSet()
        return if (nextPositions.isEmpty()) {
            emptyList()
        } else {
            nextPositions
                .map { nextPosition -> path.copy(positions = path.positions + nextPosition) }
                .flatMap { nextPath -> possiblePathsWhile(nextPath.end, whileCondition, nextPath) }
        }
    }

    fun distance(position: Position, target: Position): Int {
        if (position.isNextTo(target)) return 0
        val path = findPath(position, target) ?: throw CannotFindPathException(position, target)
        return path.length
    }
}
