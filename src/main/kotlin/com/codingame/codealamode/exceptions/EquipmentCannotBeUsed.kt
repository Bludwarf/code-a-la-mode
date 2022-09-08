package com.codingame.codealamode.exceptions

import com.codingame.codealamode.Equipment

class EquipmentCannotBeUsed(equipment: Equipment) : Throwable("${equipment.name} cannot be used")
