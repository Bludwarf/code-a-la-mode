package com.codingame.codealamode

abstract class Step {

    abstract fun isDone(gameState: GameState): Boolean

    data class GetSome(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(item)
    }

    data class Transform(val itemToTransform: Item, val equipment: Equipment, val resultingItem: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(resultingItem)
    }

    data class PutInOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.ovenContents == item
    }

    data class WaitForItemInOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.ovenContents == item
    }

    data class GetFromOven(val item: Item) : Step() {
        override fun isDone(gameState: GameState): Boolean = gameState.player.has(item)
    }
}
