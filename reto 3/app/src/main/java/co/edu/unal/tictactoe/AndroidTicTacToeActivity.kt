package co.edu.unal.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.unal.tictactoe.TicTacToeGame.Companion.COMPUTER_PLAYER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.COMPUTER_WON
import co.edu.unal.tictactoe.TicTacToeGame.Companion.HUMAN_PLAYER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.HUMAN_WON
import co.edu.unal.tictactoe.TicTacToeGame.Companion.NO_WINNER
import co.edu.unal.tictactoe.TicTacToeGame.Companion.OPEN_SPOT
import co.edu.unal.tictactoe.TicTacToeGame.Companion.TIE
import co.edu.unal.tictactoe.ui.theme.AccentBlue
import co.edu.unal.tictactoe.ui.theme.AccentViolet
import co.edu.unal.tictactoe.ui.theme.AndroidTicTacToeTheme
import co.edu.unal.tictactoe.ui.theme.BackgroundBottom
import co.edu.unal.tictactoe.ui.theme.BackgroundTop
import co.edu.unal.tictactoe.ui.theme.BoardColor
import co.edu.unal.tictactoe.ui.theme.CellColor
import co.edu.unal.tictactoe.ui.theme.ComputerRed
import co.edu.unal.tictactoe.ui.theme.HumanGreen
import co.edu.unal.tictactoe.ui.theme.SurfaceColor
import co.edu.unal.tictactoe.ui.theme.TextPrimary
import co.edu.unal.tictactoe.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val COMPUTER_DELAY_MS = 750L
private val EMPTY_BOARD = String(CharArray(TicTacToeGame.BOARD_SIZE) { OPEN_SPOT })

class AndroidTicTacToeActivity : ComponentActivity() {

    // Represents the internal state of the game
    private val mGame = TicTacToeGame()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AndroidTicTacToeTheme {
                TicTacToeScreen(mGame)
            }
        }
    }
}

@Composable
fun TicTacToeScreen(game: TicTacToeGame) {
    // UI state is saveable so the game survives screen rotation
    var board by rememberSaveable { mutableStateOf(EMPTY_BOARD) }
    var humanFirst by rememberSaveable { mutableStateOf(true) }
    var computerTurn by rememberSaveable { mutableStateOf(false) }
    var gameOver by rememberSaveable { mutableStateOf(false) }
    var winner by rememberSaveable { mutableIntStateOf(NO_WINNER) }
    var humanWins by rememberSaveable { mutableIntStateOf(0) }
    var ties by rememberSaveable { mutableIntStateOf(0) }
    var computerWins by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(0) }

    // Rebuild the game model from the saved board (e.g. after rotation)
    remember { game.setBoardState(board) }

    fun setMove(player: Char, location: Int) {
        game.setMove(player, location)
        board = game.getBoardState()
    }

    fun checkGameOver() {
        val result = game.checkForWinner()
        if (result == NO_WINNER) return
        winner = result
        gameOver = true
        when (result) {
            TIE -> ties++
            HUMAN_WON -> humanWins++
            else -> computerWins++
        }
    }

    fun startNewGame() {
        game.clearBoard()
        board = game.getBoardState()
        // Alternate who goes first
        humanFirst = !humanFirst
        computerTurn = !humanFirst
        gameOver = false
        winner = NO_WINNER
        round++
    }

    fun onCellClick(location: Int) {
        if (gameOver || computerTurn || board[location] != OPEN_SPOT) return
        setMove(HUMAN_PLAYER, location)
        checkGameOver()
        if (!gameOver) computerTurn = true
    }

    // Android's move, with a short "thinking" pause so its turn is visible
    LaunchedEffect(round, computerTurn) {
        if (computerTurn && !gameOver) {
            delay(COMPUTER_DELAY_MS)
            val move = game.getComputerMove()
            if (move >= 0) setMove(COMPUTER_PLAYER, move)
            computerTurn = false
            checkGameOver()
        }
    }

    @StringRes val statusRes = when {
        gameOver -> when (winner) {
            TIE -> R.string.result_tie
            HUMAN_WON -> R.string.result_human_wins
            else -> R.string.result_computer_wins
        }
        computerTurn && board == EMPTY_BOARD -> R.string.first_computer
        computerTurn -> R.string.turn_computer
        board == EMPTY_BOARD -> R.string.first_human
        else -> R.string.turn_human
    }
    val winningLine = remember(board) { game.getWinningLine() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)))
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val landscape = maxWidth > maxHeight

            val boardContent: @Composable (Modifier) -> Unit = { modifier ->
                GameBoard(
                    board = board,
                    round = round,
                    winningLine = winningLine,
                    winner = winner,
                    enabled = !gameOver && !computerTurn,
                    onCellClick = ::onCellClick,
                    modifier = modifier
                )
            }
            val controls: @Composable ColumnScope.() -> Unit = {
                StatusText(statusRes, thinking = computerTurn && !gameOver)
                Spacer(Modifier.height(16.dp))
                Scoreboard(humanWins, ties, computerWins)
                Spacer(Modifier.height(16.dp))
                NewGameButton(highlight = gameOver, onClick = ::startNewGame)
            }

            if (landscape) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    boardContent(
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    )
                    Column(
                        Modifier.fillMaxHeight().weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        TopBar(onNewGame = ::startNewGame)
                        PlayersRow(humanActive = !computerTurn && !gameOver, computerActive = computerTurn && !gameOver)
                        Spacer(Modifier.weight(1f))
                        controls()
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TopBar(onNewGame = ::startNewGame)
                    Spacer(Modifier.height(8.dp))
                    PlayersRow(humanActive = !computerTurn && !gameOver, computerActive = computerTurn && !gameOver)
                    Box(
                        Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        boardContent(
                            Modifier
                                .widthIn(max = 460.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                    controls()
                }
            }
        }

        if (gameOver && winner == HUMAN_WON) {
            ConfettiOverlay(key = round)
        }
    }
}

// ---------------------------------------------------------------- Top bar & menu

@Composable
private fun TopBar(onNewGame: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text("Tic", color = HumanGreen, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("·", color = TextSecondary, fontSize = 26.sp)
            Text("Tac", color = ComputerRed, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("·", color = TextSecondary, fontSize = 26.sp)
            Text("Toe", color = AccentViolet, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Box {
            val description = stringResource(R.string.more_options)
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.semantics { contentDescription = description }
            ) {
                Text("⋮", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_game)) },
                    onClick = {
                        menuOpen = false
                        onNewGame()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayersRow(humanActive: Boolean, computerActive: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerChip(stringResource(R.string.player_human), HUMAN_PLAYER, humanActive, Modifier.weight(1f))
        PlayerChip(stringResource(R.string.player_computer), COMPUTER_PLAYER, computerActive, Modifier.weight(1f))
    }
}

@Composable
private fun PlayerChip(name: String, mark: Char, active: Boolean, modifier: Modifier) {
    val color = markColor(mark)
    val scale by animateFloatAsState(if (active) 1f else 0.94f, spring(dampingRatio = 0.6f), label = "chipScale")
    val alpha by animateFloatAsState(if (active) 1f else 0.5f, tween(300), label = "chipAlpha")
    val border by animateColorAsState(if (active) color else Color.Transparent, tween(300), label = "chipBorder")
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(BorderStroke(2.dp, border), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(22.dp)) { drawMark(mark, 1f, color) }
        Spacer(Modifier.width(10.dp))
        Text(name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ---------------------------------------------------------------- Board

@Composable
private fun GameBoard(
    board: String,
    round: Int,
    winningLine: IntArray?,
    winner: Int,
    enabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier
) {
    val lineKey = winningLine?.contentToString()
    val lineProgress = remember(round, lineKey) { Animatable(0f) }
    LaunchedEffect(round, lineKey) {
        if (winningLine != null) {
            delay(380)
            lineProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
    }

    // Shake the board when Android wins
    val shake = remember(round) { Animatable(0f) }
    LaunchedEffect(round, winner) {
        if (winner == COMPUTER_WON) {
            delay(700)
            shake.animateTo(1f, tween(550, easing = LinearEasing))
        }
    }

    val gapDp = 10.dp
    BoxWithConstraints(
        modifier
            .graphicsLayer {
                val p = shake.value
                translationX = sin(p * PI.toFloat() * 6f) * (1f - p) * 18.dp.toPx()
            }
            .clip(RoundedCornerShape(28.dp))
            .background(BoardColor)
            .padding(12.dp)
    ) {
        val cellSize = (maxWidth - gapDp * 2) / 3
        Column(verticalArrangement = Arrangement.spacedBy(gapDp)) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(gapDp)) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val inLine = winningLine?.contains(index) == true
                        key(round, index) {
                            BoardCell(
                                index = index,
                                mark = board[index],
                                enabled = enabled,
                                highlighted = inLine,
                                dimmed = winner != NO_WINNER && !inLine,
                                onClick = { onCellClick(index) },
                                modifier = Modifier.size(cellSize)
                            )
                        }
                    }
                }
            }
        }

        if (winningLine != null) {
            val color = markColor(board[winningLine[0]])
            Canvas(Modifier.matchParentSize()) {
                val gap = gapDp.toPx()
                val cell = (size.width - gap * 2) / 3
                fun center(i: Int) = Offset(
                    (i % 3) * (cell + gap) + cell / 2,
                    (i / 3) * (cell + gap) + cell / 2
                )
                val a = center(winningLine[0])
                val c = center(winningLine[2])
                val dir = (c - a) / (c - a).getDistance()
                val start = a - dir * cell * 0.32f
                val end = c + dir * cell * 0.32f
                val current = start + (end - start) * lineProgress.value
                if (lineProgress.value > 0f) {
                    drawLine(color.copy(alpha = 0.25f), start, current, cell * 0.22f, StrokeCap.Round)
                    drawLine(color, start, current, cell * 0.09f, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    index: Int,
    mark: Char,
    enabled: Boolean,
    highlighted: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    // Staggered pop-in when a new game starts
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickable = enabled && mark == OPEN_SPOT
    val pressScale by animateFloatAsState(
        if (pressed && clickable) 0.88f else 1f,
        spring(dampingRatio = 0.5f), label = "press"
    )

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        1f, 1.07f,
        infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val dimAlpha by animateFloatAsState(if (dimmed) 0.35f else 1f, tween(450), label = "dim")
    val background by animateColorAsState(
        if (highlighted) markColor(mark).copy(alpha = 0.2f) else CellColor,
        tween(450), label = "cellBg"
    )

    val description = stringResource(
        R.string.cell_description, index + 1,
        if (mark == OPEN_SPOT) stringResource(R.string.cell_empty) else mark.toString()
    )

    Box(
        modifier
            .graphicsLayer {
                val s = appear.value * pressScale * (if (highlighted) pulse else 1f)
                scaleX = s
                scaleY = s
                alpha = appear.value.coerceIn(0f, 1f) * dimAlpha
            }
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = AccentViolet),
                enabled = clickable,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (mark != OPEN_SPOT) {
            key(mark) { AnimatedMark(mark, Modifier.fillMaxSize(0.58f)) }
        }
    }
}

@Composable
private fun AnimatedMark(mark: Char, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    val color = markColor(mark)
    Canvas(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        drawMark(mark, progress.value, color)
    }
}

private fun markColor(mark: Char) = if (mark == HUMAN_PLAYER) HumanGreen else ComputerRed

/** Draws an X (two strokes) or an O (arc) up to the given progress [0, 1]. */
private fun DrawScope.drawMark(mark: Char, progress: Float, color: Color) {
    val stroke = size.minDimension * 0.15f
    val inset = stroke / 2
    val w = size.width - inset * 2
    val h = size.height - inset * 2
    for ((widthMul, c) in listOf(2.2f to color.copy(alpha = 0.18f), 1f to color)) {
        val sw = stroke * widthMul
        if (mark == HUMAN_PLAYER) {
            val p1 = (progress * 2f).coerceIn(0f, 1f)
            val p2 = (progress * 2f - 1f).coerceIn(0f, 1f)
            if (p1 > 0f) drawLine(c, Offset(inset, inset), Offset(inset + w * p1, inset + h * p1), sw, StrokeCap.Round)
            if (p2 > 0f) drawLine(c, Offset(inset + w, inset), Offset(inset + w - w * p2, inset + h * p2), sw, StrokeCap.Round)
        } else if (progress > 0f) {
            drawArc(
                color = c,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(w, h),
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
    }
}

// ---------------------------------------------------------------- Status, score & button

@Composable
private fun StatusText(@StringRes statusRes: Int, thinking: Boolean) {
    AnimatedContent(
        targetState = statusRes to thinking,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 2 } + scaleIn(initialScale = 0.9f))
                .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 2 })
        },
        modifier = Modifier.fillMaxWidth().height(44.dp),
        contentAlignment = Alignment.Center,
        label = "status"
    ) { (res, isThinking) ->
        val color = when (res) {
            R.string.result_human_wins -> HumanGreen
            R.string.result_computer_wins -> ComputerRed
            else -> TextPrimary
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                stringResource(res),
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (isThinking) {
                Spacer(Modifier.width(8.dp))
                ThinkingDots()
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        0f, 3f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val d = ((phase - i + 3f) % 3f)
            val lift = if (d < 1f) sin(d * PI.toFloat()) else 0f
            Box(
                Modifier
                    .graphicsLayer { translationY = -lift * 6.dp.toPx() }
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ComputerRed.copy(alpha = 0.5f + 0.5f * lift))
            )
        }
    }
}

@Composable
private fun Scoreboard(humanWins: Int, ties: Int, computerWins: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ScoreCard(stringResource(R.string.score_human), humanWins, HumanGreen, Modifier.weight(1f))
        ScoreCard(stringResource(R.string.score_ties), ties, AccentBlue, Modifier.weight(1f))
        ScoreCard(stringResource(R.string.score_computer), computerWins, ComputerRed, Modifier.weight(1f))
    }
}

@Composable
private fun ScoreCard(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceColor)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.6f)) { it } + fadeIn())
                    .togetherWith(slideOutVertically { -it } + fadeOut())
            },
            label = "score"
        ) { count ->
            Text("$count", color = color, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NewGameButton(highlight: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "button")
    val pulse by transition.animateFloat(
        1f, 1.04f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "buttonPulse"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f), label = "btnPress")

    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                val s = pressScale * (if (highlight) pulse else 1f)
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(AccentViolet, AccentBlue)))
            .clickable(interaction, ripple(color = Color.White), onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.new_game),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------------------------------------------------- Confetti

private class Particle(
    val x: Float, val y: Float, val vx: Float, val vy: Float,
    val size: Float, val spin: Float, val color: Color
)

@Composable
private fun ConfettiOverlay(key: Int) {
    val colors = listOf(HumanGreen, AccentViolet, AccentBlue, Color(0xFFFFD166), ComputerRed)
    val particles = remember(key) {
        List(110) {
            Particle(
                x = Random.nextFloat(),
                y = -Random.nextFloat() * 0.35f - 0.05f,
                vx = (Random.nextFloat() - 0.5f) * 0.35f,
                vy = Random.nextFloat() * 0.35f + 0.25f,
                size = Random.nextFloat() * 10f + 8f,
                spin = (Random.nextFloat() - 0.5f) * 1440f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }
    val time = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { time.animateTo(1f, tween(3000, easing = LinearEasing)) }

    Canvas(Modifier.fillMaxSize()) {
        val t = time.value
        if (t >= 1f) return@Canvas
        val alpha = 1f - t * t * t
        particles.forEach { p ->
            val px = (p.x + p.vx * t) * size.width
            val py = (p.y + p.vy * t * 1.6f + 0.9f * t * t) * size.height
            val s = p.size.dp.toPx() / 2.5f
            rotate(p.spin * t, pivot = Offset(px, py)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(px - s / 2, py - s * 0.3f),
                    size = Size(s, s * 0.6f)
                )
            }
        }
    }
}
