package com.codingame.codealamode

data class Table(override val position: Position, val item: Item? = null) : Positioned {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Table) return false

        if (position != other.position) return false

        return true
    }

    override fun hashCode(): Int {
        return position.hashCode()
    }
}
