package com.codingame.codealamode

abstract class Action(val command: String, val comment: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Action) return false
        return command == other.command
    }

    override fun hashCode(): Int {
        return command.hashCode()
    }

    class Move(override val position: Position, comment: String? = null) : Action("MOVE $position", comment), Positioned

    /**
     * Limitations :
     *
     * - On ne peut pas utiliser le *partner*
     */
    class Use(override val position: Position, comment: String? = null) : Action("USE $position", comment), Positioned

    class Wait(comment: String? = null) : Action("WAIT", comment)

    override fun toString(): String {
        return if (comment != null) {
            "$command; $comment"
        } else {
            command
        }
    }

}
