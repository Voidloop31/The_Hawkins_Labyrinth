package com.example.vecnasjungle
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.vecnasjungle.GameViewModel
import com.example.vecnasjungle.Block

class MainActivity : AppCompatActivity() {

    // ════════════════════════════════
    // SECTION 4A — Variables
    // ════════════════════════════════

    // viewModels() creates OR retrieves our GameViewModel
    private val viewModel: GameViewModel by viewModels()

    // We'll store all 25 buttons here so we can update them later
    private val blockButtons = mutableListOf<Button>()

    // ════════════════════════════════
    // SECTION 4B — onCreate (app starts here)
    // ════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // loads your XML

        setupGrid()       // Step 1: Build the 25 buttons
        observeViewModel() // Step 2: Watch for data changes
    }

    // ════════════════════════════════
    // SECTION 4C — Build the Grid
    // ════════════════════════════════

    private fun setupGrid() {
        val grid = findViewById<GridLayout>(R.id.gameGrid)
        grid.removeAllViews()  // Clear anything already there
        blockButtons.clear()

        for (i in 0..24) {
            val button = Button(this)

            // Tell GridLayout WHERE to place this button
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(i % 5, 1f)  // which column
                rowSpec = GridLayout.spec(i / 5, 1f)     // which row
                setMargins(4, 4, 4, 4)                   // small gap between buttons
            }
            button.layoutParams = params

            // Store the block ID on the button itself
            button.tag = i

            // Style the button
            button.text = "?"
            button.textSize = 10f
            button.setBackgroundColor(Color.parseColor("#1a1a2e"))
            button.setTextColor(Color.WHITE)

            // What happens when player taps this button
            button.setOnClickListener {
                val blockId = it.tag as Int
                viewModel.tryMove(blockId)
                viewModel.startVecnaTimer()
            }

            blockButtons.add(button)  // Save reference
            grid.addView(button)      // Add to screen
        }
    }

    // ════════════════════════════════
    // SECTION 4D — Observe LiveData
    // ════════════════════════════════

    private fun observeViewModel() {

        // Watch player health — update text when it changes
        viewModel.playerHealth.observe(this) { hp ->
            findViewById<TextView>(R.id.tvPlayerHealth).text = "❤️ HP: $hp"
        }

        // Watch Vecna health — update text when it changes
        viewModel.vecnaHealth.observe(this) { hp ->
            findViewById<TextView>(R.id.tvVecnaHealth).text = "👁️ Vecna: $hp"
        }

        // Watch game messages
        viewModel.gameMessage.observe(this) { message ->
            findViewById<TextView>(R.id.tvGameMessage).text = message
        }

        // Watch dice roll
        viewModel.lastDiceRoll.observe(this) { roll ->
            if (roll > 0) {
                findViewById<TextView>(R.id.tvDiceRoll).text = "🎲 You rolled: $roll"
            }
        }

        // Watch blocks — redraw grid when anything changes
        viewModel.blocks.observe(this) { blocks ->
            updateGridUI(blocks)
        }

        // Watch player position — highlight current block
        viewModel.playerPosition.observe(this) { position ->
            highlightPlayerPosition(position)
        }
        // Watch for game over state
        viewModel.gameOver.observe(this) { result ->
            if (result == "WIN" || result == "LOSE" || result == "VECNA") {
                showGameOverDialog(result)
            }
        }
    }

    // ════════════════════════════════
    // SECTION 4E — Update Grid Colors
    // ════════════════════════════════

    private fun updateGridUI(blocks: List<Block>) {
        val playerPos = viewModel.playerPosition.value ?: 20

        for (block in blocks) {
            val button = blockButtons[block.id]

            when {
                // Current player position — bright blue
                block.id == playerPos -> {
                    button.setBackgroundColor(Color.parseColor("#0077ff"))
                    button.text = "YOU"
                }

                // Revealed + Upside Down = red (toxic)
                block.isRevealed && block.isUpSideDown -> {
                    button.setBackgroundColor(Color.parseColor("#8b0000"))
                    button.text = "☠️"
                }

                // Revealed + Safe = green
                block.isRevealed && !block.isUpSideDown -> {
                    button.setBackgroundColor(Color.parseColor("#1a5c2a"))
                    button.text = "✓"
                }

                // Exit block (only shown if revealed)
                block.isRevealed && block.isExit -> {
                    button.setBackgroundColor(Color.parseColor("#ffaa00"))
                    button.text = "EXIT"
                }

                // Not yet revealed = dark/unknown
                else -> {
                    button.setBackgroundColor(Color.parseColor("#1a1a2e"))
                    button.text = "?"
                }
            }
        }
    }

    // Highlight just the player's current block
    private fun highlightPlayerPosition(position: Int) {
        // Reset all buttons first
        blockButtons.forEach { btn ->
            val id = btn.tag as Int
            val blocks = viewModel.blocks.value ?: return
            val block = blocks[id]
            if (id != position) {
                when {
                    block.isRevealed && block.isUpSideDown ->
                        btn.setBackgroundColor(Color.parseColor("#8b0000"))
                    block.isRevealed ->
                        btn.setBackgroundColor(Color.parseColor("#1a5c2a"))
                    else ->
                        btn.setBackgroundColor(Color.parseColor("#1a1a2e"))
                }
            }
        }
        // Highlight player block
        blockButtons[position].setBackgroundColor(Color.parseColor("#0077ff"))
        blockButtons[position].text = "YOU"
    }
    private fun showGameOverDialog(result: String) {

        // Build the message based on result
        val title: String
        val message: String

        when (result) {
            "WIN" -> {
                title = "🎉 YOU ESCAPED!"
                message = "You found the exit and survived the Hawkins Labyrinth!\n\nVecna reached ${viewModel.vecnaHealth.value} HP"
            }
            "LOSE" -> {
                title = "💀 GAME OVER"
                message = "You were consumed by the Upside Down!\n\nVecna reached ${viewModel.vecnaHealth.value} HP"
            }
            "VECNA" -> {
                title = "👁️ VECNA WINS!"
                message = "Vecna reached full power (100 HP)!\nThe Upside Down has consumed Hawkins!"
            }
            else -> return
        }

        // Build and show the popup
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)  // player MUST tap a button
            .setPositiveButton("🔄 Play Again") { dialog, _ ->
                dialog.dismiss()
                viewModel.restartGame()  // reset everything
                setupGrid()              // rebuild the grid buttons
            }
            .setNegativeButton("❌ Quit") { _, _ ->
                finish()  // close the app
            }
            .show()
    }
}