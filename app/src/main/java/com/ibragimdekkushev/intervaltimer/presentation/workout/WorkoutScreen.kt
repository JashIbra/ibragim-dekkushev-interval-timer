package com.ibragimdekkushev.intervaltimer.presentation.workout

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibragimdekkushev.intervaltimer.R
import com.ibragimdekkushev.intervaltimer.domain.model.Interval
import com.ibragimdekkushev.intervaltimer.domain.model.IntervalTimer
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.BlueAccent
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.BlueLight
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.DividerGray
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.Green50
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.Green600
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.Green700
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.IntervalTimerTheme
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.OrangeAccent
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.OrangeLight
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.SurfaceGray
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.TextPrimary
import com.ibragimdekkushev.intervaltimer.presentation.ui.theme.TextSecondary
import kotlin.math.ceil

@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    val isActive = uiState is WorkoutUiState.Ready &&
            (uiState as WorkoutUiState.Ready).status.let {
                it == TimerStatus.Running || it == TimerStatus.Paused
            }

    BackHandler(enabled = isActive) {
        activity?.moveTaskToBack(true)
    }

    val notificationPermission =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        ) {
            viewModel.start()
        }

    val ctx = LocalContext.current
    val onStartWithPermission: () -> Unit = start@{
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.start()
            else notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }

    WorkoutContent(
        uiState = uiState,
        onBack = onBack,
        onStart = onStartWithPermission,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onReset = viewModel::reset,
    )
}

@Composable
private fun WorkoutContent(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold { innerPadding ->
        when (uiState) {
            is WorkoutUiState.Loading -> LoadingState(innerPadding)
            is WorkoutUiState.Error -> ErrorState(innerPadding, uiState.message, onBack)
            is WorkoutUiState.Ready -> ReadyState(
                innerPadding = innerPadding,
                state = uiState,
                onBack = onBack,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onReset = onReset,
            )
        }
    }
}

@Composable
private fun LoadingState(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    innerPadding: PaddingValues,
    message: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text(stringResource(R.string.btn_back))
        }
    }
}

@Composable
private fun ReadyState(
    innerPadding: PaddingValues,
    state: WorkoutUiState.Ready,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        WorkoutTopBar(state = state, onBack = onBack)

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TimerCard(state)
            if (state.status == TimerStatus.Finished) {
                FinishedStats(state.timer)
            }
        }

        IntervalsHeader(
            state = state,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        val listState = rememberLazyListState()

        LaunchedEffect(state.currentIntervalIndex) {
            listState.animateScrollToItem(state.currentIntervalIndex)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.timer.intervals) { index, interval ->
                IntervalItem(
                    index = index,
                    interval = interval,
                    remainingInCurrentIntervalMs = state.remainingInCurrentIntervalMs,
                    status = itemStatus(index, state),
                )
            }
        }

        BottomControls(
            status = state.status,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            onBack = onBack,
        )
    }
}

@Composable
private fun WorkoutTopBar(state: WorkoutUiState.Ready, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, DividerGray, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    modifier = Modifier.size(16.dp),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = TextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.timer.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            StatusBadge(state)
        }
    }
}

@Composable
private fun StatusBadge(state: WorkoutUiState.Ready) {
    val (bg, fg, dot, label) = when (state.status) {
        TimerStatus.Idle -> BadgeStyle(
            Color.Transparent,
            TextPrimary,
            Green600,
            formatMs(state.totalRemainingMs)
        )

        TimerStatus.Running -> BadgeStyle(
            Color.Transparent,
            TextPrimary,
            Green600,
            formatMs(state.totalRemainingMs)
        )

        TimerStatus.Paused -> BadgeStyle(
            OrangeLight,
            OrangeAccent,
            OrangeAccent,
            stringResource(R.string.status_paused)
        )

        TimerStatus.Finished -> BadgeStyle(
            BlueLight,
            BlueAccent,
            BlueAccent,
            stringResource(R.string.status_finished)
        )
    }
    val isTransparent = bg == Color.Transparent
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = if (isTransparent) 0.dp else 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class BadgeStyle(val bg: Color, val fg: Color, val dot: Color?, val label: String)

@Composable
private fun TimerCard(state: WorkoutUiState.Ready) {
    val (bg, accent, label) = when (state.status) {
        TimerStatus.Idle -> Triple(
            Color.White,
            TextPrimary,
            stringResource(R.string.timer_status_idle)
        )

        TimerStatus.Running -> Triple(
            Green50,
            Green700,
            stringResource(R.string.timer_status_running)
        )

        TimerStatus.Paused -> Triple(
            OrangeLight,
            OrangeAccent,
            stringResource(R.string.timer_status_paused)
        )

        TimerStatus.Finished -> Triple(
            Color.White,
            BlueAccent,
            stringResource(R.string.timer_status_finished)
        )
    }
    val labelColor = if (state.status == TimerStatus.Idle) TextSecondary else accent
    val borderColor = when (state.status) {
        TimerStatus.Idle -> DividerGray
        TimerStatus.Finished -> BlueLight
        else -> Color.Transparent
    }

    val currentInterval = state.timer.intervals[state.currentIntervalIndex]
    val totalMs = state.timer.totalTime * 1000L
    val elapsedMs = totalMs - state.totalRemainingMs
    val progress =
        if (totalMs == 0L) 0f else (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))

        if (state.status != TimerStatus.Finished) {
            Text(
                text = currentInterval.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (state.status == TimerStatus.Idle) formatMs(state.totalRemainingMs)
                else formatMs(state.remainingInCurrentIntervalMs),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            val subtitle = if (state.status == TimerStatus.Idle) {
                stringResource(R.string.stat_total_time)
            } else {
                stringResource(
                    R.string.timer_elapsed_of_total,
                    formatMs(elapsedMs),
                    formatMs(totalMs),
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        } else {
            Text(
                text = formatMs(state.timer.totalTime * 1000L),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = stringResource(R.string.timer_all_done),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accent,
            trackColor = Color.White,
        )

    }
}

@Composable
private fun FinishedStats(timer: IntervalTimer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = formatMs(timer.totalTime * 1000L),
            label = stringResource(R.string.stat_total_time),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = timer.intervals.size.toString(),
            label = stringResource(R.string.stat_intervals),
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, DividerGray, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun IntervalsHeader(state: WorkoutUiState.Ready, modifier: Modifier = Modifier) {
    val total = state.timer.intervals.size
    val countText = when (state.status) {
        TimerStatus.Idle -> stringResource(R.string.intervals_count, total)
        TimerStatus.Finished -> stringResource(R.string.intervals_progress, total, total)
        else -> stringResource(R.string.intervals_progress, state.currentIntervalIndex, total)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.intervals_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

private enum class ItemStatus { Upcoming, Current, Completed }

private fun itemStatus(index: Int, state: WorkoutUiState.Ready): ItemStatus {
    if (state.status == TimerStatus.Finished) return ItemStatus.Completed
    if (state.status == TimerStatus.Idle) return ItemStatus.Upcoming
    return when {
        index < state.currentIntervalIndex -> ItemStatus.Completed
        index == state.currentIntervalIndex -> ItemStatus.Current
        else -> ItemStatus.Upcoming
    }
}

@Composable
private fun IntervalItem(
    index: Int,
    remainingInCurrentIntervalMs: Long,
    interval: Interval,
    status: ItemStatus
) {
    val (bg, border) = when (status) {
        ItemStatus.Current -> Green50 to Green600
        ItemStatus.Completed -> SurfaceGray to SurfaceGray
        ItemStatus.Upcoming -> Color.White to DividerGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IntervalBullet(index = index, status = status)
        Spacer(Modifier.width(14.dp))
        Text(
            text = interval.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (status == ItemStatus.Completed) TextSecondary else TextPrimary,
            fontWeight = if (status == ItemStatus.Current) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (status == ItemStatus.Completed) TextDecoration.LineThrough else null,
        )
        Text(
            text = if (status == ItemStatus.Current) formatMs(remainingInCurrentIntervalMs)
            else formatMs(interval.duration * 1000L),
            style = MaterialTheme.typography.bodyMedium,
            color = if (status == ItemStatus.Current) Green700 else TextSecondary,
            fontWeight = if (status == ItemStatus.Current) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun IntervalBullet(index: Int, status: ItemStatus) {
    val bg = when (status) {
        ItemStatus.Current -> Green600
        ItemStatus.Completed -> DividerGray
        ItemStatus.Upcoming -> SurfaceGray
    }
    val fg = when (status) {
        ItemStatus.Current -> Color.White
        ItemStatus.Completed -> TextSecondary
        ItemStatus.Upcoming -> TextSecondary
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (status == ItemStatus.Completed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = (index + 1).toString(),
                color = fg,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BottomControls(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (status) {
            TimerStatus.Idle -> PrimaryButton(
                stringResource(R.string.btn_start),
                Green600,
                Icons.Default.PlayArrow,
                onStart
            )

            TimerStatus.Running -> {
                PrimaryButton(
                    stringResource(R.string.btn_pause),
                    OrangeAccent,
                    Icons.Default.Pause,
                    onPause
                )
                SecondaryButton(stringResource(R.string.btn_reset), onReset)
            }

            TimerStatus.Paused -> {
                PrimaryButton(
                    stringResource(R.string.btn_resume),
                    Green600,
                    Icons.Default.PlayArrow,
                    onResume
                )
                SecondaryButton(stringResource(R.string.btn_reset), onReset)
            }

            TimerStatus.Finished -> {
                PrimaryButton(
                    stringResource(R.string.btn_restart),
                    BlueAccent,
                    Icons.Default.Replay,
                    onStart
                )
                SecondaryButton(stringResource(R.string.btn_new_workout), onBack)
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text, color = TextSecondary)
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ceil(ms / 1000.0).toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private val previewTimer = IntervalTimer(
    id = 68,
    title = "Утренняя зарядка",
    totalTime = 900,
    intervals = listOf(
        Interval("Разминка", 120),
        Interval("Приседания", 180),
        Interval("Отдых", 60),
        Interval("Отжимания", 180),
        Interval("Отдых", 60),
        Interval("Планка", 120),
        Interval("Заминка", 180),
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutIdlePreview() {
    IntervalTimerTheme {
        WorkoutContent(
            uiState = WorkoutUiState.Ready(
                timer = previewTimer,
                status = TimerStatus.Idle,
                currentIntervalIndex = 0,
                remainingInCurrentIntervalMs = 120_000L,
                totalRemainingMs = 900_000L,
            ),
            onBack = {}, onStart = {}, onPause = {}, onResume = {}, onReset = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutRunningPreview() {
    IntervalTimerTheme {
        WorkoutContent(
            uiState = WorkoutUiState.Ready(
                timer = previewTimer,
                status = TimerStatus.Running,
                currentIntervalIndex = 2,
                remainingInCurrentIntervalMs = 42_000L,
                totalRemainingMs = 540_000L,
            ),
            onBack = {}, onStart = {}, onPause = {}, onResume = {}, onReset = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutPausedPreview() {
    IntervalTimerTheme {
        WorkoutContent(
            uiState = WorkoutUiState.Ready(
                timer = previewTimer,
                status = TimerStatus.Paused,
                currentIntervalIndex = 3,
                remainingInCurrentIntervalMs = 95_000L,
                totalRemainingMs = 380_000L,
            ),
            onBack = {}, onStart = {}, onPause = {}, onResume = {}, onReset = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun WorkoutFinishedPreview() {
    IntervalTimerTheme {
        WorkoutContent(
            uiState = WorkoutUiState.Ready(
                timer = previewTimer,
                status = TimerStatus.Finished,
                currentIntervalIndex = 6,
                remainingInCurrentIntervalMs = 0L,
                totalRemainingMs = 0L,
            ),
            onBack = {}, onStart = {}, onPause = {}, onResume = {}, onReset = {},
        )
    }
}
