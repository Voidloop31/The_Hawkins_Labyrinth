package com.example.vecnasjungle  // ← DON'T change this line

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    // ════════════════════════════════
    // SECTION 2B — Game Variables
    // ════════════════════════════════

    val blocks = MutableLiveData<List<Block>>()     // The 25 grid blocks
    val playerHealth = MutableLiveData<Int>(150)    // Player starts at 150 HP
    val vecnaHealth = MutableLiveData(5)       // Vecna starts at 5 HP
    val playerPosition = MutableLiveData<Int>(20)   // Block 20 = bottom-left
    val lastDiceRoll = MutableLiveData<Int>(0)      // Last die result
    val gameMessage = MutableLiveData<String>("")   // Status messages

    val gameOver = MutableLiveData<String>("")  // "WIN", "LOSE", or ""
    private var vecnaJob: Job? = null   // Holds the timer task
    private var gameStarted = false     // Prevents timer starting twice

    // ════════════════════════════════
    // SECTION 2C — Setup
    // ════════════════════════════════

    // Runs automatically when GameViewModel is created
    init {
        setupGame()
    }

    private fun setupGame() {
        val blockList = mutableListOf<Block>()  // Empty list to fill

        for (row in 0..4) {         // Loops 0,1,2,3,4
            for (col in 0..4) {     // Loops 0,1,2,3,4 → 25 total blocks

                val id = row * 5 + col
                // Row 0 Col 0 → id 0  (top-left)
                // Row 4 Col 0 → id 20 (bottom-left = START)
                // Row 4 Col 4 → id 24 (bottom-right)

                blockList.add(
                    Block(
                        id = id,
                        row = row,
                        col = col,
                        unlockRequirement = (1..10).random(), // Random 1–10
                        isUpSideDown = (0..1).random() == 1  // 50% toxic
                    )
                )
            }
        }

        // Pick a random exit (not the start block at index 20)
        val exitIndex = (1..19).random().let { if (it == 20) 1 else it }
        val updatedList = blockList.toMutableList()
        updatedList[exitIndex] = updatedList[exitIndex].copy(isExit = true)

        // Reveal the starting block so player can see it
        updatedList[20] = updatedList[20].copy(isRevealed = true)

        blocks.value = updatedList  // Send to screen
    }

    // ════════════════════════════════
    // SECTION 2D — Movement Logic
    // ════════════════════════════════

    fun tryMove(targetBlockId: Int) {
        val currentBlocks = blocks.value ?: return  // if null, stop
        val currentPos = playerPosition.value ?: return

        val currentBlock = currentBlocks[currentPos]
        val targetBlock = currentBlocks[targetBlockId]

        // Rule 1: Must be a neighboring block
        if (!areAdjacent(currentBlock, targetBlock)) {
            gameMessage.value = "You can only move to neighboring blocks!"
            return
        }

        // Rule 2: Roll the die (1–10)
        val roll = (1..10).random()
        lastDiceRoll.value = roll

        // Rule 3: Roll must meet the block's requirement
        if (roll < targetBlock.unlockRequirement) {
            gameMessage.value = "Rolled $roll — needed ${targetBlock.unlockRequirement}. Failed!"
            return
        }

        // Rule 4: Move player + reveal block
        val newBlocks = currentBlocks.toMutableList()
        newBlocks[targetBlockId] = targetBlock.copy(isRevealed = true)
        blocks.value = newBlocks
        playerPosition.value = targetBlockId
        gameMessage.value = "Rolled $roll — moved successfully!"

        // Rule 5: Upside Down = take 10 damage
        if (targetBlock.isUpSideDown) {
            val newHp = (playerHealth.value ?: 150) - 10
            playerHealth.value = newHp
            gameMessage.value = "⚠️ Upside Down! -10 HP. HP left: $newHp"

            if (newHp <= 0) {
                gameMessage.value = "💀 Consumed by the Upside Down!"
                stopVecnaTimer()
                gameOver.value = "LOSE"  // ← ADD THIS
                return
            }
        }

        // Rule 6: Reached the exit = win!
        if (targetBlock.isExit) {
            gameMessage.value = "🚪 You escaped the Labyrinth!"
            stopVecnaTimer()
            gameOver.value = "WIN"  // ← ADD THIS
        }
    }

    // Checks if two blocks are side-by-side (no diagonals)
    private fun areAdjacent(a: Block, b: Block): Boolean {
        val rowDiff = Math.abs(a.row - b.row)
        val colDiff = Math.abs(a.col - b.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    // ════════════════════════════════
    // SECTION 2E — Vecna's Timer
    // ════════════════════════════════

    fun startVecnaTimer() {
        if (gameStarted) return  // Don't start twice!
        gameStarted = true

        vecnaJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val newVecnaHp = (vecnaHealth.value ?: 5) + 1
                vecnaHealth.postValue(newVecnaHp)

                // Vecna reaches 100 HP = game over!
                if (newVecnaHp >= 100) {
                    gameOver.postValue("VECNA")  // ← NEW lose condition
                    stopVecnaTimer()
                    break  // stop the loop
                }
            }
        }
    }

    fun stopVecnaTimer() {
        vecnaJob?.cancel()  // Kill the background timer
    }
    fun restartGame() {
        gameStarted = false
        vecnaJob?.cancel()
        playerHealth.value = 150
        vecnaHealth.value = 5
        playerPosition.value = 20
        lastDiceRoll.value = 0
        gameMessage.value = ""
        gameOver.value = ""
        setupGame()  // rebuild the board
    }
}