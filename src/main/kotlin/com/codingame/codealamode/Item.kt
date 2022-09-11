package com.codingame.codealamode

data class Item(val name: String) {

    constructor(baseItems: Set<Item>) : this(baseItems.joinToString("-") { it.name })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Item

        if (isBase) {
            if (!other.isBase) {
                return false
            }
            return name == other.name
        }
        if (other.isBase) {
            return false
        }

        return baseItems == other.baseItems
    }

    override fun hashCode(): Int {
        if (isBase) {
            return name.hashCode()
        }
        val sortedBaseItems = baseItems.sortedBy { item -> item.name }
        return sortedBaseItems.hashCode()
    }

    fun contains(item: Item): Boolean {
        return baseItems.containsAll(item.baseItems)
    }


    fun isCompatibleWith(item: Item): Boolean {
        return item.contains(this)
    }

    fun isNotCompatibleWith(item: Item): Boolean {
        return !isCompatibleWith(item)
    }

    operator fun plus(otherItem: Item?): Item = if (otherItem == null) this else Item(baseItems + otherItem)
    operator fun minus(otherItem: Item): Item = Item(baseItems - otherItem)

    val baseItems: Items
        get() = Items(
            name.split("-")
                .map { namePart -> if (namePart == name) this else Item(namePart) }
                .toSet()
        )
    val isBase get() = !name.contains("-")

    override fun toString(): String = name

    companion object {
        val NONE = Item("NONE")
        val DISH = Item("DISH")
        val BLUEBERRIES = Item("BLUEBERRIES")
        val ICE_CREAM = Item("ICE_CREAM")

        val STRAWBERRIES = Item("STRAWBERRIES")
        val CHOPPED_STRAWBERRIES = Item("CHOPPED_" + STRAWBERRIES.name)

        val DOUGH = Item("DOUGH")
        val CROISSANT = Item("CROISSANT")

        val CHOPPED_DOUGH = Item("CHOPPED_DOUGH")
        val RAW_TART = Item("RAW_TART")
        val TART = Item("TART")
    }

}

class Items(private val value: Set<Item>) : Set<Item> by value {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Items) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
