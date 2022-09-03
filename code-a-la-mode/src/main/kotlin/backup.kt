class Simulator {
    fun simulateWhile(
        initialGameState: GameState,
        whileCondition: (GameState) -> Boolean,
        gameStateFunction: (GameState) -> GameState,
    ): GameState {
        var gameState = initialGameState
        while (whileCondition(gameState)) {
            val turnsRemaining = gameState.turnsRemaining
            gameState = gameStateFunction(gameState)
            if (gameState.turnsRemaining == turnsRemaining) TODO("Le simulateur n'a pas fait avancer le tour $turnsRemaining");
        }
        return gameState
    }

    fun simulate(gameState: GameState, action: Action): GameState {
        return when (action) {
            is Action.Use -> {
                simulate(gameState, action)
            }

            is Action.Move -> {
                simulate(gameState, action)
            }

            is Action.Wait -> {
                wait(gameState)
            }

            else -> {
                // TODO il faudrait séparer la simulation d'une action et la simulation du passage d'un tour (turns--, ovenTimer-- et ovenContents)
                gameState
            }
        }
    }

    private fun simulate(gameState: GameState, action: Action.Use): GameState {
        val position = action.position
        if (gameState.player.position.isNextTo(position)) {

            val table = gameState.getTableAt(position)
            if (table != null) {
                return simulateUse(table, gameState)
            }

            val equipment = gameState.kitchen.getEquipmentAt(position)
            if (equipment != null) {
                if (equipment == Equipment.DISHWASHER) {
                    return simulateUseDishwasher(gameState)
                } else if (equipment == Equipment.CHOPPING_BOARD) {
                    return simulateUseChoppingBoard(gameState)
                } else if (equipment == Equipment.WINDOW) {
                    return simulateUseWindow(gameState)
                } else if (equipment is ItemProvider) {
                    return simulateUse(equipment, gameState)
                }
            }

            TODO("simulate $action")
        } else {
            return simulate(gameState, Action.Move(position, action.comment), stopNextToPosition = true)
        }
    }

    private fun simulateUse(table: Table, gameState: GameState): GameState {
        val player = gameState.player

        val nextGameState = gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1
        )

        if (table.item == null) {
            if (player.item == null) {
                return nextGameState
            }
            val tableWithItem = table.copy(
                item = player.item
            )
            return nextGameState.copy(
                tablesWithItem = gameState.tablesWithItem + tableWithItem,
                player = player.copy(
                    item = null
                )
            )
        } else {
            return nextGameState.copy(
                tablesWithItem = gameState.tablesWithItem - table,
                player = if (player.item == null) player else player.copy(
                    item = player.item + table.item!!
                )
            )
        }
    }

    private fun simulate(
        gameState: GameState,
        action: Action.Move,
        stopNextToPosition: Boolean = false,
    ): GameState {
        val stopCondition =
            if (stopNextToPosition) gameState.player.position.isNextTo(action.position) else gameState.player.position == action.position
        val nextTurnGameState = gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
        )
        return if (stopCondition) {
            nextTurnGameState
        } else {
            val player = gameState.player
            if (player.position == action.position) {
                nextTurnGameState
            } else if (player.position.isNextTo(action.position)) {
                nextTurnGameState.copy(
                    player = player.copy(
                        position = action.position
                    )
                )
            } else {
                val pathFinder = PathFinder(gameState)
                val path = pathFinder.findPath(player.position, action.position)
                if (path == null) {
                    nextTurnGameState
                } else {
                    val nextPlayerPosition = path.subPath(4).lastOrNextOf(action.position)
                    nextTurnGameState.copy(
                        player = player.copy(
                            position = nextPlayerPosition
                        )
                    )
                }
            }
        }
    }

    private fun simulateUseDishwasher(gameState: GameState): GameState {
        val player = gameState.player
        val playerHasDish = player.item == Item.DISH
        return if (playerHasDish) {
            gameState
        } else {
            grabDishFromDishwasher(gameState)
        }
    }

    private fun simulateUseChoppingBoard(gameState: GameState): GameState {
        return if (gameState.player.item == Item.STRAWBERRIES) {
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
                player = gameState.player.copy(
                    item = Item.CHOPPED_STRAWBERRIES
                ),
            )
        } else {
            gameState
        }
    }

    private fun simulateUseWindow(gameState: GameState): GameState {
        val player = gameState.player
        val customerThatWantPlayerItem =
            gameState.customers.firstOrNull { customer -> customer.item == player.item }
        return if (customerThatWantPlayerItem != null) {
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
                player = player.copy(
                    item = null
                ),
                customers = gameState.customers - customerThatWantPlayerItem,
                playerScore = gameState.playerScore + customerThatWantPlayerItem.award,
            )
        } else {
            gameState.copy(
                turnsRemaining = gameState.turnsRemaining - 1,
            )
        }
    }

    private fun simulateUse(equipment: ItemProvider, gameState: GameState): GameState {
        // FIXME on ne peut pas prendre une fraise quand on a une assiette (cf message d'erreur : bludwarf: Cannot take Dish(contents=[ICE_CREAM, BLUEBERRIES]) while holding STRAWBERRIES!)
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = if (gameState.player.item == null) equipment.providedItem else gameState.player.item + equipment.providedItem
            )
        )
    }

    private fun grabDishFromDishwasher(gameState: GameState): GameState {
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
            player = gameState.player.copy(
                item = Item.DISH
            )
        )
    }

    private fun wait(gameState: GameState): GameState {
        return gameState.copy(
            turnsRemaining = gameState.turnsRemaining - 1,
        )
    }

}

class ActionsResolverItemFocused(gameState: GameState, private val playerState: PlayerState) :
    ActionsResolver(gameState) {
    override fun nextAction(): Action {
        // On doit d'abord décider si on est en mode préparation ou assemblage ou sauvetage anti-cram !

        debug("ovenContents : $ovenContents")
        if (ovenContents != null && canBeTakenOutOfOven(ovenContents)) {
            return takeItemOutOfOven(ovenContents)
        }

        if (playerState.mode == PlayerStateMode.WAITING) {
            val items = customers.flatMap { it.item.baseItems.toSet() - Item.DISH }.toSet()
            if (items.contains(Item.TART)) {
                val stepNode = StepNode(Step.GetSome(Item.TART))
                debug("stepNode = $stepNode")
                val stepNodeExpander = StepNodeExpander(gameState)
                val expandedNode = stepNodeExpander.expand(stepNode)
                debug("expandedNode = $expandedNode")
            }
            debug("items : $items")
        }

        return Action.Wait("What to do ?") // TODO
    }

}

data class PlayerState(
    val mode: PlayerStateMode = PlayerStateMode.WAITING,
    val remainingSteps: List<Step> = emptyList(),
) {


}

enum class PlayerStateMode {
    WAITING,
    TAKING_ITEM_OUT_OF_OVEN,
}

data class StepNode(val step: Step, val children: Set<StepNode> = emptySet()) {
    val hasChildren get() = children.isNotEmpty()
    val isLeaf get() = children.isEmpty()
}

class StepNodeExpander(val gameState: GameState) {
    fun expand(node: StepNode): StepNode {
        if (node.hasChildren) return node

        val step = node.step
        if (step is Step.GetSome) {
            if (gameState.contains(step.item)) {
                return node
            } else {
                // TODO plusieurs combinaison possible
            }
        }

        TODO("Cannot expand node $node")
    }
}
