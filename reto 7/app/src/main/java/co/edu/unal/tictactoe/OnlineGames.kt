package co.edu.unal.tictactoe

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

/** One online game as stored in Firestore (collection "games"). */
data class OnlineGame(
    val id: String,
    val hostId: String,
    val hostName: String,
    val guestId: String,
    val guestName: String,
    val board: String,
    val turn: Char,          // 'X' (host) or 'O' (guest)
    val status: String,      // "waiting" | "playing" | "finished"
    val winner: String,      // "" | "X" | "O" | "TIE"
    val abandoned: Boolean,
    val createdAt: Long
) {
    /** The mark ('X' / 'O') this user plays with, or null if they are not in this game. */
    fun markOf(uid: String): Char? = when (uid) {
        hostId -> 'X'
        guestId -> 'O'
        else -> null
    }

    fun opponentName(uid: String): String = if (uid == hostId) guestName else hostName
}

/**
 * All the cloud work: Firebase Anonymous Auth identifies each phone and Cloud Firestore
 * mediates the games. Every move is validated inside a Firestore transaction.
 */
object OnlineGames {
    const val WAITING = "waiting"
    const val PLAYING = "playing"
    const val FINISHED = "finished"

    // Waiting games older than this are considered abandoned and hidden from the lobby
    private const val STALE_MS = 20 * 60 * 1000L

    private val auth get() = FirebaseAuth.getInstance()
    private val games get() = FirebaseFirestore.getInstance().collection("games")

    fun currentUid(): String? = auth.currentUser?.uid

    fun signIn(onResult: (uid: String?) -> Unit) {
        auth.currentUser?.let { onResult(it.uid); return }
        auth.signInAnonymously()
            .addOnSuccessListener { onResult(it.user?.uid) }
            .addOnFailureListener { onResult(null) }
    }

    private fun DocumentSnapshot.toGame(): OnlineGame? {
        if (!exists()) return null
        return OnlineGame(
            id = id,
            hostId = getString("hostId").orEmpty(),
            hostName = getString("hostName").orEmpty(),
            guestId = getString("guestId").orEmpty(),
            guestName = getString("guestName").orEmpty(),
            board = getString("board") ?: " ".repeat(TicTacToeGame.BOARD_SIZE),
            turn = (getString("turn") ?: "X").first(),
            status = getString("status") ?: WAITING,
            winner = getString("winner").orEmpty(),
            abandoned = getBoolean("abandoned") ?: false,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    /** Live list of the games that are waiting for a second player. */
    fun listenWaiting(onChange: (List<OnlineGame>) -> Unit, onError: () -> Unit): ListenerRegistration =
        games.whereEqualTo("status", WAITING).addSnapshotListener { snap, error ->
            if (error != null || snap == null) {
                onError()
                return@addSnapshotListener
            }
            val limit = System.currentTimeMillis() - STALE_MS
            onChange(
                snap.documents.mapNotNull { it.toGame() }
                    .filter { it.createdAt >= limit }
                    .sortedByDescending { it.createdAt }
            )
        }

    fun create(uid: String, name: String, onResult: (gameId: String?) -> Unit) {
        val ref = games.document()
        val data = mapOf(
            "hostId" to uid,
            "hostName" to name,
            "guestId" to "",
            "guestName" to "",
            "board" to " ".repeat(TicTacToeGame.BOARD_SIZE),
            "turn" to "X",
            "status" to WAITING,
            "winner" to "",
            "abandoned" to false,
            "createdAt" to System.currentTimeMillis()
        )
        ref.set(data)
            .addOnSuccessListener { onResult(ref.id) }
            .addOnFailureListener { onResult(null) }
    }

    /** Joins a waiting game. The transaction guarantees only ONE guest can take the seat. */
    fun join(gameId: String, uid: String, name: String, onResult: (Boolean) -> Unit) {
        val ref = games.document(gameId)
        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snap = tx.get(ref)
            if (snap.getString("status") != WAITING || !snap.getString("guestId").isNullOrEmpty()) {
                throw FirebaseFirestoreException("taken", FirebaseFirestoreException.Code.ABORTED)
            }
            tx.update(ref, mapOf("guestId" to uid, "guestName" to name, "status" to PLAYING))
            null
        }
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    /** Live updates of one game; null means the game no longer exists. */
    fun listenGame(gameId: String, onChange: (OnlineGame?) -> Unit): ListenerRegistration =
        games.document(gameId).addSnapshotListener { snap, error ->
            if (error == null && snap != null) onChange(snap.toGame())
        }

    /** Plays [cell] if (and only if) it is [uid]'s turn and the cell is free. */
    fun playMove(gameId: String, uid: String, cell: Int) {
        val ref = games.document(gameId)
        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snap = tx.get(ref)
            val game = snap.toGame() ?: return@runTransaction null
            val mark = game.markOf(uid) ?: return@runTransaction null
            if (game.status != PLAYING || game.turn != mark || game.board[cell] != TicTacToeGame.OPEN_SPOT) {
                return@runTransaction null
            }

            // The same game rules used offline decide the winner
            val rules = TicTacToeGame()
            rules.setBoardState(game.board)
            rules.setMove(mark, cell)
            val result = rules.checkForWinner()

            val update = mutableMapOf<String, Any>(
                "board" to rules.getBoardState(),
                "turn" to if (mark == 'X') "O" else "X"
            )
            if (result != TicTacToeGame.NO_WINNER) {
                update["status"] = FINISHED
                update["winner"] = when (result) {
                    TicTacToeGame.HUMAN_WON -> "X"   // X is stored as HUMAN_PLAYER in the rules class
                    TicTacToeGame.COMPUTER_WON -> "O"
                    else -> "TIE"
                }
            }
            tx.update(ref, update)
            null
        }
    }

    /** Leaves a game: cancels it if nobody joined, or forfeits it if it was being played. */
    fun leave(game: OnlineGame?, uid: String) {
        if (game == null) return
        val ref = games.document(game.id)
        when {
            game.status == WAITING && game.hostId == uid -> ref.delete()
            game.status == PLAYING -> {
                val mark = game.markOf(uid) ?: return
                ref.update(
                    mapOf(
                        "status" to FINISHED,
                        "winner" to if (mark == 'X') "O" else "X",
                        "abandoned" to true
                    )
                )
            }
        }
    }
}
