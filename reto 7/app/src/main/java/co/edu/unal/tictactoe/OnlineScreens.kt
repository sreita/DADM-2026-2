package co.edu.unal.tictactoe

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.unal.tictactoe.TicTacToeGame.Companion.HUMAN_WON
import co.edu.unal.tictactoe.TicTacToeGame.Companion.COMPUTER_WON
import co.edu.unal.tictactoe.TicTacToeGame.Companion.NO_WINNER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.TIE
import co.edu.unal.tictactoe.ui.theme.AccentBlue
import co.edu.unal.tictactoe.ui.theme.AccentViolet
import co.edu.unal.tictactoe.ui.theme.BackgroundBottom
import co.edu.unal.tictactoe.ui.theme.BackgroundTop
import co.edu.unal.tictactoe.ui.theme.ComputerRed
import co.edu.unal.tictactoe.ui.theme.HumanGreen
import co.edu.unal.tictactoe.ui.theme.SurfaceColor
import co.edu.unal.tictactoe.ui.theme.TextPrimary
import co.edu.unal.tictactoe.ui.theme.TextSecondary

private const val SCREEN_LOBBY = 0
private const val SCREEN_OFFLINE = 1
private const val SCREEN_ONLINE = 2

private const val PREFS_NAME = "ttt_prefs"
private const val PREF_PLAYER_NAME = "player_name"

private val ScreenBackground = Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom))

/** Lobby -> online game, or lobby -> offline game against Android. */
@Composable
fun AppRoot(game: TicTacToeGame) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var screen by rememberSaveable { mutableIntStateOf(SCREEN_LOBBY) }
    var gameId by rememberSaveable { mutableStateOf("") }
    var playerName by rememberSaveable { mutableStateOf(prefs.getString(PREF_PLAYER_NAME, "").orEmpty()) }

    // Anonymous sign-in: gives this phone a unique id without any registration
    var uid by remember { mutableStateOf(OnlineGames.currentUid()) }
    var authFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        if (uid == null) {
            authFailed = false
            OnlineGames.signIn { result ->
                uid = result
                authFailed = result == null
            }
        }
    }

    fun saveName(name: String) {
        playerName = name
        prefs.edit().putString(PREF_PLAYER_NAME, name).apply()
    }

    val myUid = uid
    when {
        screen == SCREEN_OFFLINE -> {
            BackHandler { screen = SCREEN_LOBBY }
            TicTacToeScreen(game)
        }

        screen == SCREEN_ONLINE && myUid != null && gameId.isNotEmpty() ->
            OnlineGameScreen(gameId = gameId, uid = myUid) {
                screen = SCREEN_LOBBY
                gameId = ""
            }

        else -> LobbyScreen(
            uid = myUid,
            authFailed = authFailed,
            name = playerName,
            onNameChange = { playerName = it },
            onRetry = { attempt++ },
            onEnterGame = { id, name ->
                saveName(name)
                gameId = id
                screen = SCREEN_ONLINE
            },
            onPlayOffline = { screen = SCREEN_OFFLINE }
        )
    }
}

// ---------------------------------------------------------------- Lobby

@Composable
private fun LobbyScreen(
    uid: String?,
    authFailed: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onRetry: () -> Unit,
    onEnterGame: (gameId: String, name: String) -> Unit,
    onPlayOffline: () -> Unit
) {
    var games by remember { mutableStateOf<List<OnlineGame>>(emptyList()) }
    var listError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var messageRes by remember { mutableStateOf<Int?>(null) }

    // Live list of the games waiting for an opponent
    DisposableEffect(uid) {
        if (uid == null) {
            onDispose { }
        } else {
            val registration = OnlineGames.listenWaiting(
                onChange = { list ->
                    games = list.filter { it.hostId != uid }
                    listError = false
                },
                onError = { listError = true }
            )
            onDispose { registration.remove() }
        }
    }

    val defaultName = stringResource(R.string.default_player_name)
    val effectiveName = name.trim().ifEmpty { defaultName }

    fun createGame() {
        val me = uid ?: return
        if (busy) return
        busy = true
        messageRes = null
        OnlineGames.create(me, effectiveName) { id ->
            busy = false
            if (id != null) onEnterGame(id, effectiveName) else messageRes = R.string.connection_error
        }
    }

    fun joinGame(target: OnlineGame) {
        val me = uid ?: return
        if (busy) return
        busy = true
        messageRes = null
        OnlineGames.join(target.id, me, effectiveName) { ok ->
            busy = false
            if (ok) onEnterGame(target.id, effectiveName) else messageRes = R.string.game_taken
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        val landscape = maxWidth > maxHeight

        val top: @Composable ColumnScope.() -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tic", color = HumanGreen, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("·", color = TextSecondary, fontSize = 28.sp)
                Text("Tac", color = ComputerRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("·", color = TextSecondary, fontSize = 28.sp)
                Text("Toe", color = AccentViolet, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.play_online), color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 16) onNameChange(it) },
                label = { Text(stringResource(R.string.player_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentViolet,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = AccentViolet,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = AccentViolet
                )
            )
            Spacer(Modifier.height(12.dp))
            when {
                authFailed -> {
                    Text(stringResource(R.string.connection_error), color = ComputerRed, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    GradientButton(stringResource(R.string.retry), onClick = onRetry)
                }

                uid == null -> Text(stringResource(R.string.connecting), color = TextSecondary, fontSize = 16.sp)

                else -> GradientButton(
                    text = stringResource(R.string.create_game),
                    enabled = !busy,
                    onClick = ::createGame
                )
            }
            messageRes?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(it), color = ComputerRed, fontSize = 14.sp)
            }
        }

        val offlineButton: @Composable ColumnScope.() -> Unit = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(BorderStroke(2.dp, AccentViolet.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
                    .clickable(onClick = onPlayOffline),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.play_vs_android),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val list: @Composable ColumnScope.() -> Unit = {
            Text(
                stringResource(R.string.available_games),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            when {
                listError -> Text(stringResource(R.string.connection_error), color = ComputerRed, fontSize = 14.sp)
                games.isEmpty() -> Text(stringResource(R.string.no_games), color = TextSecondary, fontSize = 15.sp)
                else -> LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(games, key = { it.id }) { item ->
                        GameCard(item) { joinGame(item) }
                    }
                }
            }
        }

        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    top()
                    Spacer(Modifier.height(16.dp))
                    offlineButton()
                }
                Column(Modifier.weight(1f).fillMaxHeight()) { list() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                top()
                Spacer(Modifier.height(18.dp))
                Column(Modifier.weight(1f)) { list() }
                Spacer(Modifier.height(12.dp))
                offlineButton()
            }
        }
    }
}

@Composable
private fun GameCard(game: OnlineGame, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(game.hostName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.join_hint), color = TextSecondary, fontSize = 13.sp)
        }
        Text("▶", color = AccentBlue, fontSize = 20.sp)
    }
}

@Composable
private fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(listOf(AccentViolet, AccentBlue)),
                alpha = if (enabled) 1f else 0.4f
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- Online game

@Composable
private fun OnlineGameScreen(gameId: String, uid: String, onExit: () -> Unit) {
    var game by remember { mutableStateOf<OnlineGame?>(null) }
    val exit by rememberUpdatedState(onExit)

    // Live updates of this game: every move of the opponent arrives here
    DisposableEffect(gameId) {
        val registration = OnlineGames.listenGame(gameId) { updated ->
            if (updated == null) exit() else game = updated
        }
        onDispose { registration.remove() }
    }

    fun leave() {
        OnlineGames.leave(game, uid)
        onExit()
    }
    BackHandler { leave() }

    val current = game
    val me: Char? = current?.markOf(uid)
    val myTurn = current != null && current.status == OnlineGames.PLAYING && current.turn == me
    val finished = current?.status == OnlineGames.FINISHED
    val iWon = finished && current?.winner == me?.toString()

    val board = current?.board ?: " ".repeat(TicTacToeGame.BOARD_SIZE)
    val winningLine = remember(board) {
        TicTacToeGame().apply { setBoardState(board) }.getWinningLine()
    }
    val winnerCode = when {
        !finished -> NO_WINNER
        current?.winner == "TIE" -> TIE
        iWon -> HUMAN_WON
        else -> COMPUTER_WON
    }

    val statusText = when {
        current == null -> stringResource(R.string.connecting)
        current.status == OnlineGames.WAITING -> stringResource(R.string.waiting_opponent)
        finished && current.winner == "TIE" -> stringResource(R.string.result_tie)
        finished && iWon && current.abandoned -> stringResource(R.string.opponent_left)
        finished && iWon -> stringResource(R.string.result_human_wins)
        finished -> stringResource(R.string.online_you_lost)
        myTurn -> stringResource(R.string.online_your_turn)
        else -> stringResource(R.string.online_opponent_turn, current.opponentName(uid))
    }
    val statusColor = when {
        finished && iWon -> HumanGreen
        finished && current?.winner != "TIE" -> ComputerRed
        else -> TextPrimary
    }

    Box(Modifier.fillMaxSize().background(ScreenBackground)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp)
        ) {
            val landscape = maxWidth > maxHeight

            val boardContent: @Composable (Modifier) -> Unit = { modifier ->
                GameBoard(
                    board = board,
                    round = 0,
                    winningLine = winningLine,
                    winner = winnerCode,
                    enabled = myTurn,
                    onCellClick = { cell -> OnlineGames.playMove(gameId, uid, cell) },
                    modifier = modifier
                )
            }
            val info: @Composable ColumnScope.() -> Unit = {
                me?.let {
                    Text(
                        stringResource(R.string.you_play_as, it.toString()),
                        color = if (it == 'X') HumanGreen else ComputerRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (current != null && current.status != OnlineGames.WAITING) {
                    Text(
                        "${current.hostName} (X)  vs  ${current.guestName} (O)",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                AnimatedContent(
                    targetState = statusText,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.CenterStart,
                    label = "onlineStatus"
                ) { text ->
                    Text(text, color = statusColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            val exitButton: @Composable ColumnScope.() -> Unit = {
                GradientButton(stringResource(R.string.leave_game), onClick = ::leave)
            }

            if (landscape) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    boardContent(Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        info()
                        Spacer(Modifier.weight(1f))
                        exitButton()
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    info()
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        boardContent(Modifier.widthIn(max = 460.dp).fillMaxWidth().aspectRatio(1f))
                    }
                    exitButton()
                }
            }
        }

        if (iWon && current?.abandoned != true) ConfettiOverlay(key = 0)
    }
}
