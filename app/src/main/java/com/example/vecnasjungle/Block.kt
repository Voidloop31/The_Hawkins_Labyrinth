package com.example.vecnasjungle

data class Block (
    val id: Int,                              // Unique number 0–24
    val row: Int,                             // Which row (0–4)
    val col: Int,                             // Which column (0–4)
    val unlockRequirement: Int,               // Die roll needed to enter
    val isUpSideDown: Boolean,                // true = toxic block
    var isRevealed: Boolean = false,          // Has player visited?
    val isExit: Boolean = false
)