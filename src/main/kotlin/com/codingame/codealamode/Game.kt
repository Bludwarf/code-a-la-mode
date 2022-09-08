package com.codingame.codealamode

class Game(val kitchen: Kitchen, val customers: List<Customer> = emptyList()) {
    val spawnPositions by kitchen::spawnPositions
}
