package com.codingame.codealamode

data class Timer(var t0: Long = System.currentTimeMillis()) {

    val interval: Long
        get() {
            val t1 = System.currentTimeMillis()
            return t1 - t0
        }

    override fun toString(): String {
        val t1 = System.currentTimeMillis()
        val interval = t1 - t0
        t0 = System.currentTimeMillis()
        return interval.toString()
    }
}
