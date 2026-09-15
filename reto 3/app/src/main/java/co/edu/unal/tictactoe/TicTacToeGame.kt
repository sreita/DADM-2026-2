package co.edu.unal.tictactoe

import java.util.Random

/**
 * Game logic for tic-tac-toe, fully separated from the UI.
 * The board is a char array: HUMAN_PLAYER (X), COMPUTER_PLAYER (O) or OPEN_SPOT.
 */
class TicTacToeGame {

    companion object {
        // Characters used to represent the human, computer, and open spots
        const val HUMAN_PLAYER = 'X'
        const val COMPUTER_PLAYER = 'O'
        const val OPEN_SPOT = ' '

        const val BOARD_SIZE = 9

        // Values returned by checkForWinner()
        const val NO_WINNER = 0
        const val TIE = 1
        const val HUMAN_WON = 2
        const val COMPUTER_WON = 3

        private val WINNING_LINES = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // rows
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // columns
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                       // diagonals
        )
    }

    private val mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT }

    private val mRand = Random()

    /** Clear the board of all X's and O's by setting all spots to OPEN_SPOT. */
    fun clearBoard() {
        mBoard.fill(OPEN_SPOT)
    }

    /**
     * Set the given player at the given location on the game board.
     * The location must be available, or the board will not be changed.
     *
     * @param player - The HUMAN_PLAYER or COMPUTER_PLAYER
     * @param location - The location (0-8) to place the move
     */
    fun setMove(player: Char, location: Int) {
        if (location in 0 until BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player
        }
    }

    /**
     * Return the best move for the computer to make. You must call setMove()
     * to actually make the computer move to that location.
     * @return The best move for the computer to make (0-8), or -1 if the board is full.
     */
    fun getComputerMove(): Int {
        // First see if there's a move O can make to win
        findWinningMove(COMPUTER_PLAYER, COMPUTER_WON)?.let { return it }

        // See if there's a move O can make to block X from winning
        findWinningMove(HUMAN_PLAYER, HUMAN_WON)?.let { return it }

        // Generate random move
        val openSpots = (0 until BOARD_SIZE).filter { mBoard[it] == OPEN_SPOT }
        if (openSpots.isEmpty()) return -1
        return openSpots[mRand.nextInt(openSpots.size)]
    }

    /**
     * Check for a winner and return a status value indicating who has won.
     * @return Return 0 if no winner or tie yet, 1 if it's a tie, 2 if X won,
     * or 3 if O won.
     */
    fun checkForWinner(): Int {
        for (line in WINNING_LINES) {
            val first = mBoard[line[0]]
            if (first != OPEN_SPOT && first == mBoard[line[1]] && first == mBoard[line[2]]) {
                return if (first == HUMAN_PLAYER) HUMAN_WON else COMPUTER_WON
            }
        }
        return if (mBoard.any { it == OPEN_SPOT }) NO_WINNER else TIE
    }

    /** The three locations forming the winning line, or null if nobody has won. */
    fun getWinningLine(): IntArray? = WINNING_LINES.firstOrNull { line ->
        val first = mBoard[line[0]]
        first != OPEN_SPOT && first == mBoard[line[1]] && first == mBoard[line[2]]
    }

    /** Returns what is at the given location (HUMAN_PLAYER, COMPUTER_PLAYER or OPEN_SPOT). */
    fun getBoardOccupant(location: Int): Char = mBoard[location]

    /** Board as a 9-character string, used to save state across configuration changes. */
    fun getBoardState(): String = String(mBoard)

    /** Restores a board previously obtained with getBoardState(). */
    fun setBoardState(state: String) {
        if (state.length != BOARD_SIZE) return
        state.forEachIndexed { i, c -> mBoard[i] = c }
    }

    private fun findWinningMove(player: Char, winValue: Int): Int? {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] != OPEN_SPOT) continue
            mBoard[i] = player
            val wins = checkForWinner() == winValue
            mBoard[i] = OPEN_SPOT
            if (wins) return i
        }
        return null
    }
}
