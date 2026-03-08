package com.upnp.fakeCall.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.upnp.fakeCall.FakeCallViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FakeCallViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("FakeCall") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Open settings")
                    }
                }
            )
        },
        floatingActionButton = {
            val canTrigger = state.hasRequiredPermissions && state.isProviderEnabled
            val fabText = if (state.isTimerRunning) "Cancel Timer" else "Trigger Call"

            ExtendedFloatingActionButton(
                onClick = {
                    if (canTrigger || state.isTimerRunning) {
                        viewModel.onTriggerOrCancelClicked()
                    }
                },
                shape = RoundedCornerShape(32.dp),
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.callerName,
                onValueChange = viewModel::onCallerNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target Caller Name") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.callerNumber,
                onValueChange = viewModel::onCallerNumberChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target Caller Number") },
                singleLine = true
            )

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
                        text = "Call me in: ${FakeCallViewModel.formatDelay(state.selectedDelaySeconds)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        viewModel.delayOptionsSeconds.forEach { option ->
                            FilterChip(
                                selected = option == state.selectedDelaySeconds,
                                onClick = { viewModel.onDelaySelected(option) },
                                label = { Text(FakeCallViewModel.formatDelay(option)) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                    }
                }
            }

            if (state.isTimerRunning) {
                Text(
                    text = "Countdown active. Tap Cancel Timer to stop it.",
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

            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}
