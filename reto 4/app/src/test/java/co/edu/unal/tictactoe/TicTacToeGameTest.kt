package co.edu.unal.tictactoe

import co.edu.unal.tictactoe.TicTacToeGame.Companion.COMPUTER_PLAYER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.HUMAN_PLAYER
import co.edu.unal.tictactoe.TicTacToeGame.DifficultyLevel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeGameTest {

    private fun gameWith(board: String, level: DifficultyLevel = DifficultyLevel.Expert) =
        TicTacToeGame().apply {
            setBoardState(board)
            setDifficultyLevel(level)
        }

    @Test
    fun emptyBoardHasNoWinner() {
        assertEquals(TicTacToeGame.NO_WINNER, TicTacToeGame().checkForWinner())
    }

    @Test
    fun detectsHumanWinAndLine() {
        val game = gameWith("XXXOO    ")
        assertEquals(TicTacToeGame.HUMAN_WON, game.checkForWinner())
        assertArrayEquals(intArrayOf(0, 1, 2), game.getWinningLine())
    }

    @Test
    fun detectsComputerDiagonalWin() {
        assertEquals(TicTacToeGame.COMPUTER_WON, gameWith("OX XOX  O").checkForWinner())
    }

    @Test
    fun detectsTie() {
        val game = gameWith("XOXXOOOXX")
        assertEquals(TicTacToeGame.TIE, game.checkForWinner())
        assertNull(game.getWinningLine())
    }

    @Test
    fun setMoveDoesNotOverwrite() {
        val game = TicTacToeGame()
        game.setMove(HUMAN_PLAYER, 4)
        game.setMove(COMPUTER_PLAYER, 4)
        assertEquals(HUMAN_PLAYER, game.getBoardOccupant(4))
    }

    @Test
    fun defaultLevelIsExpert() {
        assertEquals(DifficultyLevel.Expert, TicTacToeGame().getDifficultyLevel())
    }

    @Test
    fun expertTakesWinningMove() {
        // O can win at 2; X could win at 5 — winning has priority
        assertEquals(2, gameWith("OO XX  X ").getComputerMove())
    }

    @Test
    fun expertBlocksHuman() {
        assertEquals(2, gameWith("XX  O    ").getComputerMove())
    }

    @Test
    fun harderWinsButDoesNotBlock() {
        // Winning move available: it must be taken
        assertEquals(2, gameWith("OO XX  X ", DifficultyLevel.Harder).getComputerMove())
        // Only a blocking move available: Harder ignores it and plays at random
        val moves = (1..40).map { gameWith("XX  O    ", DifficultyLevel.Harder).getComputerMove() }
        assertTrue(moves.all { it in listOf(2, 3, 5, 6, 7, 8) })
        assertTrue(moves.any { it != 2 })
    }

    @Test
    fun easyAlwaysPlaysRandomly() {
        // Even with a winning move at 2 available, Easy just picks any open spot
        val moves = (1..40).map { gameWith("OO XX  X ", DifficultyLevel.Easy).getComputerMove() }
        assertTrue(moves.all { it in listOf(2, 5, 6, 8) })
        assertNotEquals(1, moves.distinct().size)
    }

    @Test
    fun moveFindersLeaveTheBoardUnchanged() {
        val game = gameWith("XX  O    ")
        game.getComputerMove()
        assertEquals("XX  O    ", game.getBoardState())
    }

    @Test
    fun fullBoardHasNoMove() {
        assertEquals(TicTacToeGame.NO_MOVE, gameWith("XOXXOOOXX").getComputerMove())
    }

    @Test
    fun impossibleTakesWinningMove() {
        assertEquals(2, gameWith("OO XX  X ", DifficultyLevel.Impossible).getComputerMove())
    }

    @Test
    fun impossibleBlocksHuman() {
        assertEquals(2, gameWith("XX  O    ", DifficultyLevel.Impossible).getComputerMove())
    }

    @Test
    fun impossibleAvoidsTheCornerFork() {
        // X on two opposite corners, O in the center: taking a corner would let X
        // build a double threat, so the only safe replies are the edges.
        val move = gameWith("X   O   X", DifficultyLevel.Impossible).getComputerMove()
        assertTrue("Expected an edge but played $move", move in listOf(1, 3, 5, 7))
    }

    @Test
    fun impossibleLeavesTheBoardUnchanged() {
        val game = gameWith("X   O   X", DifficultyLevel.Impossible)
        game.getComputerMove()
        assertEquals("X   O   X", game.getBoardState())
    }

    @Test
    fun impossibleNeverLoses() {
        val rnd = java.util.Random(7)
        repeat(60) { gameNumber ->
            val game = TicTacToeGame()
            game.setDifficultyLevel(DifficultyLevel.Impossible)
            // Alternate who starts, just like the app does
            var humanTurn = gameNumber % 2 == 0

            while (game.checkForWinner() == TicTacToeGame.NO_WINNER) {
                if (humanTurn) {
                    // The human plays greedily: wins or blocks if it can, otherwise at random
                    val open = (0 until TicTacToeGame.BOARD_SIZE)
                        .filter { game.getBoardOccupant(it) == TicTacToeGame.OPEN_SPOT }
                    val move = open.firstOrNull { spot ->
                        val probe = TicTacToeGame()
                        probe.setBoardState(game.getBoardState())
                        probe.setMove(HUMAN_PLAYER, spot)
                        probe.checkForWinner() == TicTacToeGame.HUMAN_WON
                    } ?: open.firstOrNull { spot ->
                        val probe = TicTacToeGame()
                        probe.setBoardState(game.getBoardState())
                        probe.setMove(COMPUTER_PLAYER, spot)
                        probe.checkForWinner() == TicTacToeGame.COMPUTER_WON
                    } ?: open[rnd.nextInt(open.size)]
                    game.setMove(HUMAN_PLAYER, move)
                } else {
                    game.setMove(COMPUTER_PLAYER, game.getComputerMove())
                }
                humanTurn = !humanTurn
            }

            assertNotEquals(
                "The human won game $gameNumber against the Impossible level",
                TicTacToeGame.HUMAN_WON,
                game.checkForWinner()
            )
        }
    }

    @Test
    fun clearBoardEmptiesEverything() {
        val game = gameWith("XOXXOOOXX")
        game.clearBoard()
        assertEquals(" ".repeat(9), game.getBoardState())
    }
}
