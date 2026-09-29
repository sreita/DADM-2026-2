package co.edu.unal.tictactoe

import java.util.Random

/**
 * Game logic for tic-tac-toe, fully separated from the UI.
 * The board is a char array: HUMAN_PLAYER (X), COMPUTER_PLAYER (O) or OPEN_SPOT.
 */
class TicTacToeGame {

    /** The computer's difficulty levels. */
    enum class DifficultyLevel { Easy, Harder, Expert, Impossible }

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

        /** Returned by the move finders when no such move exists. */
        const val NO_MOVE = -1

        private val WINNING_LINES = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // rows
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // columns
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                       // diagonals
        )
    }

    private val mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT }

    private val mRand = Random()

    // Current difficulty level
    private var mDifficultyLevel = DifficultyLevel.Expert

    fun getDifficultyLevel(): DifficultyLevel = mDifficultyLevel

    fun setDifficultyLevel(difficultyLevel: DifficultyLevel) {
        mDifficultyLevel = difficultyLevel
    }

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
     * Return the best move for the computer to make, according to the current
     * difficulty level. You must call setMove() to actually make the move.
     *
     * Easy       - always a random move.
     * Harder     - win if possible, otherwise a random move.
     * Expert     - win if possible, block the human, otherwise a random move.
     * Impossible - minimax over the whole game tree: it can never be beaten.
     *
     * @return The move for the computer to make (0-8), or NO_MOVE if the board is full.
     */
    fun getComputerMove(): Int {
        var move = NO_MOVE

        when (mDifficultyLevel) {
            DifficultyLevel.Easy -> move = getRandomMove()

            DifficultyLevel.Harder -> {
                move = getWinningMove()
                if (move == NO_MOVE) move = getRandomMove()
            }

            DifficultyLevel.Expert -> {
                // Try to win, but if that's not possible, block.
                // If that's not possible, move anywhere.
                move = getWinningMove()
                if (move == NO_MOVE) move = getBlockingMove()
                if (move == NO_MOVE) move = getRandomMove()
            }

            DifficultyLevel.Impossible -> move = getBestMove()
        }

        return move
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

    /** A random open spot, or NO_MOVE if the board is full. */
    private fun getRandomMove(): Int {
        val openSpots = (0 until BOARD_SIZE).filter { mBoard[it] == OPEN_SPOT }
        if (openSpots.isEmpty()) return NO_MOVE
        return openSpots[mRand.nextInt(openSpots.size)]
    }

    /** A move that makes the computer win right away, or NO_MOVE. */
    private fun getWinningMove(): Int = findMoveFor(COMPUTER_PLAYER, COMPUTER_WON)

    /** A move that stops the human from winning on the next turn, or NO_MOVE. */
    private fun getBlockingMove(): Int = findMoveFor(HUMAN_PLAYER, HUMAN_WON)

    /**
     * Minimax: plays out every possible continuation of the game and keeps the
     * move with the best utility for the computer. The board is always left
     * exactly as it was before the method was called.
     *
     * Utility of a finished game, seen from the computer:
     *   +10 - depth  the computer wins (winning in fewer moves is better)
     *   depth - 10   the human wins (losing in more moves is better)
     *   0            tie
     */
    private fun getBestMove(): Int {
        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Int>()

        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] != OPEN_SPOT) continue
            mBoard[i] = COMPUTER_PLAYER
            val score = minimax(depth = 1, maximizing = false)
            mBoard[i] = OPEN_SPOT

            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(i)
            } else if (score == bestScore) {
                bestMoves.add(i)
            }
        }

        // Several moves may be equally good; pick one at random so it is not predictable
        if (bestMoves.isEmpty()) return NO_MOVE
        return bestMoves[mRand.nextInt(bestMoves.size)]
    }

    private fun minimax(depth: Int, maximizing: Boolean): Int {
        when (checkForWinner()) {
            COMPUTER_WON -> return 10 - depth
            HUMAN_WON -> return depth - 10
            TIE -> return 0
        }

        val player = if (maximizing) COMPUTER_PLAYER else HUMAN_PLAYER
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE

        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] != OPEN_SPOT) continue
            mBoard[i] = player
            val score = minimax(depth + 1, !maximizing)
            mBoard[i] = OPEN_SPOT
            best = if (maximizing) maxOf(best, score) else minOf(best, score)
        }

        return best
    }

    /**
     * Tries every open spot for the given player. The board is always left
     * exactly as it was before the method was called.
     */
    private fun findMoveFor(player: Char, winValue: Int): Int {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] != OPEN_SPOT) continue
            mBoard[i] = player
            val wins = checkForWinner() == winValue
            mBoard[i] = OPEN_SPOT
            if (wins) return i
        }
        return NO_MOVE
    }
}
