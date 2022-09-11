package com.codingame.codealamode

class Customer(
    val item: Item,
    /** Award intrinsèque + nombre de tours restants */
    val award: Int,
) {
    override fun toString(): String {
        return "Customer(item = $item, award = $award)"
    }

    fun copy(
        item: Item = this.item,
        award: Int = this.award,
    ): Customer = Customer(item, award)
}
