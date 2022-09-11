package com.codingame.gameengine.core

abstract class AbstractMultiplayerPlayer : AbstractPlayer() {

    private val active = true

    /**
     * Returns true is the player is still active in the game (can be executed).
     *
     * @return true is the player is active.
     */
    val isActive: Boolean get() = active

}
