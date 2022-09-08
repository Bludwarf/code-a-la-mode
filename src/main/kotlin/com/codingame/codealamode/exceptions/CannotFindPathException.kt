package com.codingame.codealamode.exceptions

import com.codingame.codealamode.Position

class CannotFindPathException(position: Position, target: Position) :
    Exception("Cannot find path from $position to $target")
