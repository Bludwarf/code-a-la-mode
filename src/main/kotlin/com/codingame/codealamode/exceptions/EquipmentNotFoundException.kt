package com.codingame.codealamode.exceptions

import com.codingame.codealamode.Equipment

class EquipmentNotFoundException(equipment: Equipment) : Exception("${equipment.name} not found in the kitchen")
