package com.upnp.fakeCall.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.upnp.fakeCall.FakeCallViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FakeCallViewModel,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onAudioFileSelected(uri)
    }
    val recordingsFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        viewModel.onRecordingFolderSelected(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    },
                    modifier = Modifier.statusBarsPadding()
                )
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

                SectionHeader("Provider Options")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!state.hasRequiredPermissions) {
                            SettingsRow(
                                icon = Icons.Filled.Phone,
                                title = "Phone permissions required",
                                subtitle = "Grant access to register the call provider.",
                                onClick = onRequestPermissions
                            )
                        }

                        ListItem(
                            headlineContent = { Text("Provider name") },
                            supportingContent = {
                                OutlinedTextField(
                                    value = state.providerName,
                                    onValueChange = viewModel::onProviderNameChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp)
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Filled.Phone, contentDescription = null)
                            }
                        )

                        SettingsRow(
                            icon = Icons.Filled.CheckCircle,
                            title = "Save & register provider",
                            subtitle = "Make this account available for incoming calls.",
                            onClick = viewModel::saveProvider
                        )

                        SettingsRow(
                            icon = Icons.Filled.Settings,
                            title = "Enable provider in system",
                            subtitle = if (state.isProviderEnabled) {
                                "Provider is enabled."
                            } else {
                                "Open Calling Accounts to enable it."
                            },
                            onClick = { openCallingAccounts(context, viewModel) }
                        )
                    }
                }

                SectionHeader("Audio & Media")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.MusicNote,
                            title = "Select audio file",
                            subtitle = "Current: ${state.selectedAudioName.ifBlank { "Default" }}",
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }
                        )

                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.VolumeOff,
                            title = "Use default audio",
                            subtitle = "Disable custom audio playback.",
                            onClick = viewModel::clearAudioSelection
                        )

                        SettingsToggleRow(
                            icon = Icons.Filled.Mic,
                            title = "Microphone recording",
                            subtitle = if (state.isRecordingEnabled) "Enabled" else "Disabled",
                            checked = state.isRecordingEnabled,
                            onCheckedChange = viewModel::onRecordingEnabledChange
                        )
                    }
                }

                SectionHeader("Storage")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsRow(
                            icon = Icons.Filled.Folder,
                            title = "Recording folder",
                            subtitle = "Save to: ${state.recordingsFolderName}",
                            onClick = { recordingsFolderLauncher.launch(null) }
                        )

                        SettingsRow(
                            icon = Icons.Filled.Refresh,
                            title = "Reset recording folder",
                            subtitle = "Use Downloads/FakeCall",
                            onClick = viewModel::clearRecordingFolderSelection
                        )
                    }
                }

                if (state.statusMessage.isNotBlank()) {
                    Surface(
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

private fun openCallingAccounts(context: Context, viewModel: FakeCallViewModel) {
    val intent = viewModel.openCallingAccountsIntent()
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        if (it is ActivityNotFoundException) {
            // Ignore silently on unsupported devices.
        }
    }
}
