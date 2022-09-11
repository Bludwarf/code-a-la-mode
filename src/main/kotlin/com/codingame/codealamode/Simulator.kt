package com.codingame.codealamode

import com.codingame.codealamode.resolvers.ActionsResolver
import debug
import debugEnabled
import debugIndent
import debugSimulation
import java.util.function.Function
import java.util.function.Predicate

class Simulator {

    private val cookbook = CookBook()
    private val playerAdapter = PlayerAdapter()
    private val cellAdapter = CellAdapter()

    fun simulateWhile(
        initialGameState: GameState,
        whileCondition: Predicate<GameState>,
        gameStateFunction: (GameState) -> GameState,
        debug: Boolean = initialGameState.createdByTests && debugSimulation,
    ): GameState {
        debug("=== Starting simulation ===")
        val production = !initialGameState.createdByTests
        val originalDebugEnabled = debugEnabled
        debugEnabled = debug
        debugIndent += "  "
        val timer = Timer()

        var gameState = initialGameState
        while (gameState.turnsRemaining > 0 && (!production || timer.interval < 50) && whileCondition.test(gameState)) {
            val turnsRemaining = gameState.turnsRemaining
            gameState = gameStateFunction(gameState)
            if (gameState.turnsRemaining == turnsRemaining) TODO("Le simulateur n'a pas fait avancer le tour $turnsRemaining");
        }

        debugIndent = debugIndent.dropLast(2)
        debugEnabled = originalDebugEnabled
        debug("=== Simulation finished in $timer ms ===")
        return gameState
    }

    fun fastestComparator(
        initialGameState: GameState,
        winCondition: Predicate<Chef>,
        actionsResolverSupplier: Function<GameState, ActionsResolver>,
    ): java.util.Comparator<Chef> {
        val whileCondition = Predicate<GameState> {
            !winCondition.test(it.player) && !winCondition.test(it.partner)
        }
        return Comparator { player: Chef, partner: Chef ->
            val initialState = initialGameState.copy(
                player = player.copy(),
                partner = partner.copy(),
            )
            val finalState = simulateWhile(initialState, whileCondition, actionsResolverSupplier)
            val lastChefToPlay = finalState.partner
            val winner: Chef? = if (winCondition.test(lastChefToPlay)) lastChefToPlay else null
            debug("Winner is $winner")
            when (winner) {
                player -> -1
                partner -> 1
                else -> 0
            }
        }
    }

    fun simulateWhile(
        initialGameState: GameState,
        whileCondition: Predicate<GameState>,
        actionsResolverSupplier: Function<GameState, ActionsResolver>,
        debug: Boolean = initialGameState.createdByTests && debugSimulation,
    ): GameState {
        return simulateWhile(initialGameState, whileCondition, { previousGameState ->
            val actionsResolver = actionsResolverSupplier.apply(previousGameState)
            if (debug && initialGameState.createdByTests) debug("= State ${201 - previousGameState.turnsRemaining} : ${previousGameState.player} =")
            val action = actionsResolver.nextAction()
            if (debug && initialGameState.createdByTests) println(debugIndent + action)
            val nextGameState = previousGameState
                .let { simulate(it, action) }
                .let { simulateCook(it) }
                .let { nextGameState ->
                    nextGameState.copy(
                        turnsRemaining = previousGameState.turnsRemaining - 1,
                        player = nextGameState.partner,
                        partner = nextGameState.player,
                        customers = nextGameState.customers.map { customer ->
                            customer.copy(
                                award = customer.award - 1
                            )
                        }
                    )
                }
                .let { simulateNextCustomer(it) }
            nextGameState
        }, debug)
    }

    private fun simulateNextCustomer(gameState: GameState): GameState {
        if (gameState.customers.size < 3 && gameState.remainingCustomers.isNotEmpty()) {
            val nextCustomer = gameState.remainingCustomers.first()
            return gameState.copy(
                customers = gameState.customers + nextCustomer,
                remainingCustomers = gameState.remainingCustomers - nextCustomer,
            )
        }
        return gameState
    }

    private fun simulateCook(gameState: GameState): GameState {
        if (gameState.ovenTimer == 0) return gameState

        val ovenTimer = gameState.ovenTimer - 1
        if (ovenTimer > 0) return gameState.copy(ovenTimer = ovenTimer)

        val previousOvenContents = gameState.ovenContents
        val nextOvenContents = cooked(previousOvenContents, gameState)
        val ovenContentsHasBurned = previousOvenContents != null && nextOvenContents == null
        return gameState.copy(
            ovenTimer = if (ovenContentsHasBurned) 0 else 10,
            ovenContents = nextOvenContents,
        )
    }

    private fun cooked(ovenContents: Item?, gameState: GameState): Item? {
        val cookBook by gameState::cookBook
        if (ovenContents == null) return null
        return cookBook.producedItemAfterBaking(ovenContents) ?: (null.also { debug("$ovenContents has burned !") })
    }

    /**
     * Ne simule pas autre chose que l'action
     */
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
                return when (equipment) {
                    Equipment.DISHWASHER -> simulateUseDishwasher(gameState)
                    Equipment.CHOPPING_BOARD -> simulateUseChoppingBoard(gameState)
                    Equipment.OVEN -> simulateUseOven(gameState)
                    Equipment.WINDOW -> simulateUseWindow(gameState)
                    is ItemProvider -> simulateUse(equipment, gameState)
                    else -> TODO("Simulate use equipment $equipment")
                }
            }

            TODO("Simulate $action")
        } else {
            return simulate(gameState, Action.Move(position, action.comment), stopNextToPosition = true)
        }
    }

    private fun simulateUse(table: Table, gameState: GameState): GameState {
        val player = gameState.player

        if (table.item == null) {
            if (player.item == null) {
                return gameState
            }
            val tableWithItem = table.copy(
                item = player.item
            )
            return gameState.copy(
                tablesWithItem = gameState.tablesWithItem + tableWithItem,
                player = player.copy(
                    item = null
                )
            )
        } else {
            if (player.item == null) {
                return gameState.copy(
                    tablesWithItem = gameState.tablesWithItem - table,
                    player = player.copy(
                        item = table.item!!
                    )
                )
            } else {
                if (player.hasDish) {
                    if (cookbook.isPrepared(table.item)) {
                        return gameState.copy(
                            tablesWithItem = gameState.tablesWithItem - table,
                            player = player.copy(
                                item = player.item + table.item
                            )
                        )
                    } else {
                        debug("Cannot take ${table.item} in ${player.item}")
                        return gameState
                    }
                } else {
                    if (cookbook.isPrepared(player.item)) {
                        val tableWithCombinedItems = table.copy(item = table.item + player.item)
                        return gameState.copy(
                            tablesWithItem = gameState.tablesWithItem - table + tableWithCombinedItems,
                            player = player.copy(
                                item = null
                            )
                        )
                    } else {
                        debug("Cannot put ${player.item} in ${table.item}")
                        return gameState
                    }
                }
            }

        }
    }

    private fun simulate(
        gameState: GameState,
        action: Action.Move,
        stopNextToPosition: Boolean = false,
    ): GameState {
        val player = gameState.player
        val gamePlayer = playerAdapter.adapt(player, gameState.partner, gameState.kitchen)
        val cell = cellAdapter.adapt(action.position, gameState.kitchen)
        gamePlayer.moveTo(cell)
        val newPosition = cellAdapter.adapt(gamePlayer.location)
        return gameState.copy(
            player = player.copy(
                position = newPosition
            )
        )
    }

    private fun simulateUseDishwasher(gameState: GameState): GameState {
        val player = gameState.player
        return if (player.hasDish) {
            gameState
        } else {
            grabDishFromDishwasher(gameState)
        }
    }

    private fun simulateUseChoppingBoard(gameState: GameState): GameState {
        val chopped = chopped(gameState.player.item) ?: return gameState
        return gameState.copy(
            player = gameState.player.copy(
                item = chopped
            ),
        )
    }

    private fun chopped(item: Item?): Item? {
        return when (item) {
            Item.STRAWBERRIES -> Item.CHOPPED_STRAWBERRIES
            Item.DOUGH -> Item.CHOPPED_DOUGH
            else -> null
        }
    }

    private fun simulateUseOven(gameState: GameState): GameState {
        val player = gameState.player
        val ovenContents = gameState.ovenContents
        if (player.item == null) {
            return gameState.copy(
                ovenContents = null,
                player = player.copy(
                    item = ovenContents
                ),
                ovenTimer = 0,
            )
        } else {
            if (ovenContents != null) {
                if (player.hasDish && !player.item.contains(ovenContents)) {
                    return gameState.copy(
                        ovenContents = null,
                        player = player.copy(
                            item = player.item + ovenContents
                        ),
                        ovenTimer = 0
                    )
                } else {
                    debug("ERROR : $player cannot take $ovenContents from oven")
                }
            } else {
                return gameState.copy(
                    ovenContents = player.item,
                    player = player.copy(
                        item = null
                    ),
                    ovenTimer = 10
                )
            }
        }
        return gameState
    }

    private fun simulateUseWindow(gameState: GameState): GameState {
        val player = gameState.player
        val customerThatWantPlayerItem =
            gameState.customers.firstOrNull { customer -> customer.item == player.item }
        return if (customerThatWantPlayerItem != null) {
            gameState.copy(
                player = player.copy(
                    item = null
                ),
                customers = gameState.customers - customerThatWantPlayerItem,
                playerScore = gameState.playerScore + customerThatWantPlayerItem.award,
            )
        } else {
            gameState
        }
    }

    private fun simulateUse(equipment: ItemProvider, gameState: GameState): GameState {
        // FIXME on ne peut pas prendre une fraise quand on a une assiette (cf message d'erreur : bludwarf: Cannot take Dish(contents=[ICE_CREAM, BLUEBERRIES]) while holding STRAWBERRIES!)
        return gameState.copy(
            player = gameState.player.copy(
                item = if (gameState.player.item == null) {
                    equipment.providedItem
                } else if (gameState.player.item == Item.CHOPPED_DOUGH && equipment.providedItem == Item.BLUEBERRIES) {
                    Item.RAW_TART
                } else {
                    gameState.player.item + equipment.providedItem
                }
            )
        )
    }

    private fun grabDishFromDishwasher(gameState: GameState): GameState {
        return gameState.copy(
            player = gameState.player.copy(
                item = Item.DISH + gameState.player.item
            )
        )
    }

    private fun wait(gameState: GameState): GameState {
        return gameState
    }

}
