package com.codingame.codealamode

interface Positioned {
    val position: Position

    fun isNextTo(position: Position): Boolean {
        return this.position.isNextTo(position)
    }

    fun isNextTo(positioned: Positioned): Boolean {
        return this.isNextTo(positioned.position)
    }
}
