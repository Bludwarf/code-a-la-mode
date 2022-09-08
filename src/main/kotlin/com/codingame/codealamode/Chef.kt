package com.codingame.codealamode

class Chef(val name: String, override var position: Position, val item: Item? = null) : Positioned {

    fun copy(
        name: String = this.name,
        position: Position = this.position,
        item: Item? = this.item,
    ): Chef {
        return Chef(name, position, item)
    }

    val hasDish: Boolean by lazy { item?.contains(Item.DISH) ?: false }

    override fun equals(other: Any?): Boolean {
        if (other !is Chef) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return if (item != null) "$name with $item" else name
    }

    fun has(item: Item): Boolean {
        return this.item == item
    }

    fun contains(item: Item): Boolean {
        return has(item) || this.item != null && this.item.contains(item)
    }
}
