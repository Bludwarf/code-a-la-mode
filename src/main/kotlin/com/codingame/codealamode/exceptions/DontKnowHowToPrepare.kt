package com.codingame.codealamode.exceptions

import com.codingame.codealamode.Item

class DontKnowHowToPrepare(item: Item) : Throwable("Don't know how to prepare $item")
