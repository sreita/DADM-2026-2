package co.edu.unal.tictactoe

import co.edu.unal.tictactoe.TicTacToeGame.Companion.COMPUTER_PLAYER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.HUMAN_PLAYER
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicTacToeGameTest {

    private fun gameWith(board: String) = TicTacToeGame().apply { setBoardState(board) }

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
    fun computerTakesWinningMove() {
        // O can win at 2; X could win at 5 — winning has priority
        assertEquals(2, gameWith("OO XX  X ").getComputerMove())
    }

    @Test
    fun computerBlocksHuman() {
        assertEquals(2, gameWith("XX  O    ").getComputerMove())
    }

    @Test
    fun clearBoardEmptiesEverything() {
        val game = gameWith("XOXXOOOXX")
        game.clearBoard()
        assertEquals(" ".repeat(9), game.getBoardState())
    }
}
