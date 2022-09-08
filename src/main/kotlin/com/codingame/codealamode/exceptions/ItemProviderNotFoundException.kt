package com.codingame.codealamode.exceptions

import com.codingame.codealamode.Item

class ItemProviderNotFoundException(item: Item) : Exception("Cannot find provider for $item")
