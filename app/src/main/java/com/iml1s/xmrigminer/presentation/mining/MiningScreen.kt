package com.iml1s.xmrigminer.presentation.mining

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiningScreen(
    viewModel: MiningViewModel = hiltViewModel(),
    onNavigateToConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Collect one-time effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MiningEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MiningEffect.NavigateToConfig -> {
                    Toast.makeText(context, effect.reason, Toast.LENGTH_LONG).show()
                    onNavigateToConfig()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("XMRig Miner") },
                actions = {
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error notification
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ErrorCard(
                    error = uiState.error ?: "",
                    onDismiss = { viewModel.onEvent(MiningEvent.ClearError) }
                )
            }

            // Status card
            StatusCard(
                isRunning = uiState.isRunning,
                stats = uiState.stats
            )

            // Control buttons
            ControlButtons(
                isRunning = uiState.isRunning,
                isLoading = uiState.isLoading,
                onStartClick = { viewModel.onEvent(MiningEvent.StartMining) },
                onStopClick = { viewModel.onEvent(MiningEvent.StopMining) }
            )

            // Detailed statistics
            StatsDetailCard(stats = uiState.stats)

            // CPU information
            CpuInfoCard()
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    isRunning: Boolean,
    stats: com.iml1s.xmrigminer.data.model.MiningStats
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRunning) "🟢 Mining" else "⚪ Stopped",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Hashrate
            StatRow(
                label = "Hashrate",
                value = if (isRunning && stats.hashrate == 0.0) "Calculating..." else "%.2f H/s".format(stats.hashrate),
                icon = Icons.Default.Speed
            )

            // Shares
            StatRow(
                label = "Accepted/Rejected",
                value = "${stats.acceptedShares} / ${stats.rejectedShares}",
                icon = Icons.Default.CheckCircle
            )

            // Success rate
            StatRow(
                label = "Success Rate",
                value = if (stats.acceptedShares + stats.rejectedShares == 0) 
                    "0.0%" 
                else 
                    "%.1f%%".format(stats.successRate),
                icon = Icons.Default.TrendingUp
            )

            // Difficulty
            StatRow(
                label = "Difficulty",
                value = if (stats.difficulty == 0L) "-" else stats.difficulty.toString(),
                icon = Icons.Default.GridOn
            )

            // Temperature
            StatRow(
                label = "Temperature",
                value = if (stats.temperature > 0) "%.1f°C".format(stats.temperature) else "-",
                icon = Icons.Default.Thermostat
            )

            // CPU usage
            StatRow(
                label = "CPU Usage",
                value = when {
                    stats.cpuUsage > 0f -> "${stats.cpuUsage.roundToInt()}%"
                    isRunning -> "Calculating..."
                    else -> "-"
                },
                icon = Icons.Default.Memory
            )

            // Battery
            StatRow(
                label = "Battery",
                value = "${stats.batteryLevel}% ${if (stats.isCharging) "⚡" else ""}",
                icon = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ControlButtons(
    isRunning: Boolean,
    isLoading: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartClick,
            modifier = Modifier.weight(1f),
            enabled = !isRunning && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading && !isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Start Mining")
        }

        Button(
            onClick = onStopClick,
            modifier = Modifier.weight(1f),
            enabled = isRunning && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isLoading && isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Icon(Icons.Default.Stop, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Stop Mining")
        }
    }
}

@Composable
fun StatsDetailCard(
    stats: com.iml1s.xmrigminer.data.model.MiningStats
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Detailed Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            DetailRow("CPU Usage", when {
                stats.cpuUsage > 0f -> "${stats.cpuUsage.roundToInt()}%"
                else -> "-"
            })
            DetailRow("Difficulty", if (stats.difficulty == 0L) "-" else stats.difficulty.toString())
            DetailRow("Hashrate (10s)", if (stats.hashrate10s > 0) "%.2f H/s".format(stats.hashrate10s) else "-")
            DetailRow("Hashrate (60s)", if (stats.hashrate60s > 0) "%.2f H/s".format(stats.hashrate60s) else "-")
            DetailRow("Hashrate (15m)", if (stats.hashrate15m > 0) "%.2f H/s".format(stats.hashrate15m) else "-")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CpuInfoCard() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CPU Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            val cpuInfo = remember {
                try {
                    com.iml1s.xmrigminer.native.XMRigBridge.getCpuInfo()
                } catch (e: Exception) {
                    "Unable to retrieve CPU info"
                }
            }

            Text(
                text = cpuInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
