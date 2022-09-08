package com.codingame.codealamode

import com.codingame.codealamode.exceptions.DontKnowHowToPrepare

private val PREPARED_ITEMS = listOf(
    Item.CHOPPED_STRAWBERRIES,
    Item.CROISSANT,
    Item.TART,
    Item.RAW_TART,
    Item.CHOPPED_DOUGH,
)

class CookBook {
    fun stepsToPrepare(item: Item, fallbackValue: List<Step>? = null): List<Step> {
        val baseItemsWithoutDish = item.baseItems - Item.DISH
        return baseItemsWithoutDish.flatMap { stepsToPrepareBaseItem(it, fallbackValue) } + Step.GetSome(Item.DISH)
    }

    fun stepsToPrepareBaseItem(item: Item, fallbackValue: List<Step>? = null): List<Step> {
        return when (item) {

            // Ligue Bois 2

            Item.CHOPPED_STRAWBERRIES -> listOf(
                Step.GetSome(Item.STRAWBERRIES),
                Step.Transform(Item.STRAWBERRIES, Equipment.CHOPPING_BOARD, Item.CHOPPED_STRAWBERRIES),
            )

            // Ligue Bois 3

            Item.CROISSANT -> listOf(
                Step.GetSome(Item.DOUGH),
                Step.PutInOven(Item.DOUGH),
                Step.WaitForItemInOven(Item.CROISSANT),
                Step.GetFromOven(Item.CROISSANT),
            )

            // Ligue Bronze

            Item.TART -> listOf(
                Step.GetSome(Item.RAW_TART),
                Step.PutInOven(Item.RAW_TART),
                Step.WaitForItemInOven(Item.TART),
                Step.GetFromOven(Item.TART),
            )

            Item.RAW_TART -> listOf(
                Step.GetSome(Item.CHOPPED_DOUGH),
                Step.GetSome(Item.BLUEBERRIES),
            )

            Item.CHOPPED_DOUGH -> listOf(
                Step.GetSome(Item.DOUGH),
                Step.Transform(Item.DOUGH, Equipment.CHOPPING_BOARD, Item.CHOPPED_DOUGH),
            )

            else -> fallbackValue ?: throw DontKnowHowToPrepare(item)
        }
    }

    fun contains(item: Item): Boolean {
        val fallbackValue = emptyList<Step>()
        val stepsToPrepare = stepsToPrepare(item, fallbackValue)
        return stepsToPrepare !== fallbackValue
    }

    fun needToBeBakedByOven(item: Item): Boolean {
        val fallbackValue = emptyList<Step>()
        val stepsToPrepare = stepsToPrepare(item, fallbackValue)
        return stepsToPrepare.any { step -> step is Step.WaitForItemInOven && step.item == item }
    }

    fun canBurnInOven(item: Item?): Boolean {
        return producedItemAfterBaking(item) == null
    }

    fun producedItemAfterBaking(ovenContents: Item?): Item? {
        // TODO utiliser les recettes
        return when (ovenContents) {
            Item.DOUGH -> Item.CROISSANT
            Item.RAW_TART -> Item.TART
            else -> null
        }
    }

    fun totalStepsToPrepare(item: Item): List<Step> {
        val fallbackValue = emptyList<Step>()
        return stepsToPrepareBaseItem(item, fallbackValue).flatMap { step ->
            if (step is Step.GetSome) {
                stepsToPrepareBaseItem(step.item, fallbackValue)
            } else {
                fallbackValue
            }
        }
    }

    private fun preparedItemsThatNeedToGetSome(item: Item): List<Item> {
        val fallbackValue = emptyList<Step>()
        return PREPARED_ITEMS.filter { preparedItem ->
            stepsToPrepare(preparedItem, fallbackValue).any { step ->
                step == Step.GetSome(item)
            }
        }
    }

    /**
     * @param item l'ingrédient
     * @return l'aliment préparé s'il est le seul à utiliser l'ingrédient
     */
    fun onlyPreparedItemThatNeedToGetSome(item: Item): Item? {
        val preparedItem = preparedItemsThatNeedToGetSome(item)
        return if (preparedItem.size == 1) preparedItem[0] else null
    }

}
