package com.codingame.codealamode

import com.codingame.codealamode.exceptions.EquipmentNotFoundException
import com.codingame.codealamode.exceptions.ItemProviderNotFoundException

internal val DEFAULT_SPAWN_POSITIONS = arrayOf(Position(1, 3), Position(8, 3))

data class Kitchen(
    val spawnPositions: Array<Position> = DEFAULT_SPAWN_POSITIONS,
    private val floorPositions: Set<Position> = emptySet(),
    private val equipmentPositions: Map<Equipment, Position> = emptyMap(),
    val tables: Set<Table> = emptySet(),
    val lines: List<String> = emptyList(),
) {
    private val itemProviders = mutableMapOf<Item, ItemProvider?>()

    fun getPositionOf(equipment: Equipment): Position {
        if (!equipmentPositions.containsKey(equipment)) {
            throw EquipmentNotFoundException(equipment)
        }
        return equipmentPositions[equipment]!!
    }

    fun requireEquipmentThatProvides(item: Item): ItemProvider {
        return itemProviders.computeIfAbsent(item) {
            equipmentPositions.keys
                .filterIsInstance<ItemProvider>()
                .firstOrNull { itemProvider -> itemProvider.providedItem == item }
        } ?: throw ItemProviderNotFoundException(item)
    }

    fun getEquipmentThatProvides(item: Item): ItemProvider? {
        return itemProviders.computeIfAbsent(item) {
            equipmentPositions.keys
                .filterIsInstance<ItemProvider>()
                .firstOrNull { itemProvider -> itemProvider.providedItem == item }
        }
    }

    fun isEmpty(position: Position): Boolean {
        return floorPositions.contains(position)
    }

    fun getEquipmentAt(position: Position): Equipment? {
        val entry = equipmentPositions.entries.firstOrNull { entry -> entry.value == position }
        return entry?.key
    }

    fun getPositionOf(item: Item): Position? {
        val equipmentThatProvides = getEquipmentThatProvides(item)
            ?: return null//.also { debug("Cannot find equipment that provides $item in kitchen") }
        return getPositionOf(equipmentThatProvides)
    }

    val mostAccessibleTableComparator: Comparator<Table> by lazy {
        Comparator.comparing<Table, Int> { table -> floorPositions.count { it.isNextTo(table.position) } }
            .reversed()
    }

}
