package com.upnp.fakeCall.ui.screens

import android.text.format.DateFormat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.upnp.fakeCall.CustomPreset
import com.upnp.fakeCall.FakeCallViewModel
import com.upnp.fakeCall.ScheduleKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FakeCallViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showCustomSheet by rememberSaveable { mutableStateOf(false) }
    var sheetMode by rememberSaveable {
        mutableStateOf(
            if (state.scheduleKind == ScheduleKind.CUSTOM_EXACT) {
                ScheduleKind.CUSTOM_EXACT
            } else {
                ScheduleKind.CUSTOM_COUNTDOWN
            }
        )
    }

    val blurRadius by animateFloatAsState(
        targetValue = if (showCustomSheet) 18f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy)
    )

    val backgroundScale by animateFloatAsState(
        targetValue = if (showCustomSheet) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val canTrigger = state.hasRequiredPermissions && state.isProviderEnabled
    val fabText = if (state.isTimerRunning) "Cancel Timer" else "Trigger Call"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = backgroundScale
                    scaleY = backgroundScale
                }
                .blur(blurRadius.dp)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "FakeCall",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Schedule your perfect escape",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Open settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (canTrigger || state.isTimerRunning) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onTriggerOrCancelClicked()
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = if (canTrigger || state.isTimerRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (canTrigger || state.isTimerRunning) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(fabText)
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Caller",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = state.callerName,
                                onValueChange = viewModel::onCallerNameChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Target Caller Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp)
                            )

                            OutlinedTextField(
                                value = state.callerNumber,
                                onValueChange = viewModel::onCallerNumberChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Target Caller Number") },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val timeDisplay = scheduleDisplay(state, DateFormat.is24HourFormat(context))
                            Text(
                                text = timeDisplay,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = scheduleSubtitle(state),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                viewModel.delayOptionsSeconds.forEach { option ->
                                    FilterChip(
                                        selected = state.scheduleKind == ScheduleKind.PRESET && option == state.selectedDelaySeconds,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.onDelaySelected(option)
                                        },
                                        label = { Text(FakeCallViewModel.formatDelay(option)) },
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sheetMode = if (state.scheduleKind == ScheduleKind.CUSTOM_EXACT) {
                                            ScheduleKind.CUSTOM_EXACT
                                        } else {
                                            ScheduleKind.CUSTOM_COUNTDOWN
                                        }
                                        showCustomSheet = true
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EditCalendar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Custom Call")
                                }
                            }

                            if (state.customPresets.isNotEmpty()) {
                                Text(
                                    text = "Saved presets",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    state.customPresets.forEach { preset ->
                                        val label = formatCustomPreset(preset, DateFormat.is24HourFormat(context))
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.onCustomPresetSelected(preset)
                                            },
                                            label = { Text(label) },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.removeCustomPreset(preset)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Remove preset"
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                    }
                                }
                            }

                            if (!viewModel.canScheduleExactAlarms()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Exact alarms are off.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val intent = viewModel.openExactAlarmSettingsIntent()
                                        runCatching { context.startActivity(intent) }
                                    }) {
                                        Text("Enable")
                                    }
                                }
                            }
                        }
                    }

                    if (state.isTimerRunning) {
                        val runningLabel = runningScheduleLabel(state.timerEndsAtMillis)
                        Text(
                            text = "Countdown active • $runningLabel",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (state.statusMessage.isNotBlank()) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!state.hasRequiredPermissions || !state.isProviderEnabled) {
                        Text(
                            text = "Go to Settings to grant permissions and enable provider.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        CustomCallSheet(
            visible = showCustomSheet,
            scheduleKind = sheetMode,
            countdownMinutes = state.customCountdownMinutes,
            countdownSeconds = state.customCountdownSeconds,
            exactHour = state.customExactHour,
            exactMinute = state.customExactMinute,
            onScheduleKindChange = { sheetMode = it },
            onCountdownChange = viewModel::onCustomCountdownChange,
            onExactTimeChange = viewModel::onCustomExactTimeChange,
            onSavePreset = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addCustomPreset(sheetMode)
            },
            onApply = {
                viewModel.onScheduleKindSelected(sheetMode)
                showCustomSheet = false
            },
            onDismiss = { showCustomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCallSheet(
    visible: Boolean,
    scheduleKind: ScheduleKind,
    countdownMinutes: Int,
    countdownSeconds: Int,
    exactHour: Int,
    exactMinute: Int,
    onScheduleKindChange: (ScheduleKind) -> Unit,
    onCountdownChange: (Int, Int) -> Unit,
    onExactTimeChange: (Int, Int) -> Unit,
    onSavePreset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = visible) { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
            backProgress = 0f
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    if (visible || progress > 0f) {
        val scrimAlpha = lerp(0f, 0.35f, progress)
        val cornerRadius = androidx.compose.ui.unit.lerp(40.dp, 22.dp, progress)
        val offsetY = androidx.compose.ui.unit.lerp(90.dp, 0.dp, progress)
        val scale = lerp(0.95f, 1f, progress) * lerp(1f, 0.92f, backProgress)
        val alpha = lerp(0.7f, 1f, progress) * lerp(1f, 0.6f, backProgress)

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .padding(bottom = 1.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .offset(y = offsetY)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .shadow(12.dp, RoundedCornerShape(cornerRadius)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null
                        )
                        Text(
                            text = "Custom Call",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = scheduleKind == ScheduleKind.CUSTOM_COUNTDOWN,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScheduleKindChange(ScheduleKind.CUSTOM_COUNTDOWN)
                            },
                            label = { Text("Countdown") },
                            shape = RoundedCornerShape(999.dp)
                        )
                        FilterChip(
                            selected = scheduleKind == ScheduleKind.CUSTOM_EXACT,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScheduleKindChange(ScheduleKind.CUSTOM_EXACT)
                            },
                            label = { Text("Exact Time") },
                            shape = RoundedCornerShape(999.dp)
                        )
                    }

                    if (scheduleKind == ScheduleKind.CUSTOM_COUNTDOWN) {
                        CountdownPicker(
                            minutes = countdownMinutes,
                            seconds = countdownSeconds,
                            onChange = onCountdownChange
                        )
                    } else {
                        val is24Hour = DateFormat.is24HourFormat(context)
                        ExactTimePicker(
                            hour = exactHour,
                            minute = exactMinute,
                            is24Hour = is24Hour,
                            onChange = onExactTimeChange
                        )
                    }

                    FilledTonalButton(onClick = onSavePreset) {
                        Text("Save as preset")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onApply()
                        }) {
                            Text("Use this time")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownPicker(
    minutes: Int,
    seconds: Int,
    onChange: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            label = "MIN",
            value = minutes,
            range = 0..59,
            onValueChange = {
                onChange(it, seconds)
            }
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        WheelPicker(
            label = "SEC",
            value = seconds,
            range = 0..59,
            onValueChange = {
                onChange(minutes, it)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExactTimePicker(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onChange: (Int, Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = is24Hour
    )

    LaunchedEffect(timePickerState, hour, minute) {
        snapshotFlow { timePickerState.hour to timePickerState.minute }
            .collect { (newHour, newMinute) ->
                if (newHour != hour || newMinute != minute) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onChange(newHour, newMinute)
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun WheelPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val listState = rememberPickerState(value - range.first)
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(value) {
        val target = (value - range.first).coerceIn(0, range.last - range.first)
        if ((listState.state.firstVisibleItemIndex - target).absoluteValue > 1) {
            listState.state.scrollToItem(target)
        }
    }

    LaunchedEffect(listState, value) {
        snapshotFlow {
            val offset = listState.state.firstVisibleItemScrollOffset
            val index = listState.state.firstVisibleItemIndex +
                if (offset > listState.itemHeightPx / 2) 1 else 0
            index.coerceIn(0, range.last - range.first)
        }.collect { index ->
            val selected = range.first + index
            if (selected != value) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(selected)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(itemHeight * 3)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState.state,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight),
            flingBehavior = listState.flingBehavior
        ) {
            items(range.count()) { index ->
                val displayed = range.first + index
                val isSelected = displayed == value
                Text(
                    text = displayed.toString().padStart(2, '0'),
                    style = if (isSelected) {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .height(itemHeight)
                        .padding(vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .height(itemHeight)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private data class PickerState(
    val state: androidx.compose.foundation.lazy.LazyListState,
    val flingBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    val itemHeightPx: Int
)

@Composable
private fun rememberPickerState(initialIndex: Int): PickerState {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val itemHeight = 40.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.roundToPx() }
    val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
    return remember(listState, flingBehavior, itemHeightPx) {
        PickerState(listState, flingBehavior, itemHeightPx)
    }
}

private fun scheduleDisplay(state: com.upnp.fakeCall.FakeCallUiState, is24Hour: Boolean): String {
    return when (state.scheduleKind) {
        ScheduleKind.CUSTOM_EXACT -> formatExactTime(state.customExactHour, state.customExactMinute, is24Hour)
        ScheduleKind.CUSTOM_COUNTDOWN -> FakeCallViewModel.formatDelay(
            state.customCountdownMinutes * 60 + state.customCountdownSeconds
        )
        ScheduleKind.PRESET -> FakeCallViewModel.formatDelay(state.selectedDelaySeconds)
    }
}

private fun scheduleSubtitle(state: com.upnp.fakeCall.FakeCallUiState): String {
    return when (state.scheduleKind) {
        ScheduleKind.CUSTOM_EXACT -> "Exact time"
        ScheduleKind.CUSTOM_COUNTDOWN -> "Countdown timer"
        ScheduleKind.PRESET -> "Quick preset"
    }
}

private fun formatExactTime(hour: Int, minute: Int, is24Hour: Boolean): String {
    val formatter = if (is24Hour) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("h:mm a")
    }
    val time = java.time.LocalTime.of(hour, minute)
    return time.format(formatter)
}

private fun runningScheduleLabel(triggerAtMillis: Long): String {
    if (triggerAtMillis <= 0L) return ""
    val time = Instant.ofEpochMilli(triggerAtMillis)
        .atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm")
    return time.format(formatter)
}

private fun formatCustomPreset(preset: CustomPreset, is24Hour: Boolean): String {
    return when (preset.kind) {
        ScheduleKind.CUSTOM_COUNTDOWN -> FakeCallViewModel.formatDelay(
            preset.minutes * 60 + preset.seconds
        )
        ScheduleKind.CUSTOM_EXACT -> formatExactTime(preset.hour, preset.minute, is24Hour)
        else -> ""
    }
}
