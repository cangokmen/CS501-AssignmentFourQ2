package com.example.assignmentfourq2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignmentfourq2.ui.theme.AssignmentFourQ2Theme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- Data class to hold the entire UI state ---
data class CounterUiState(
    val count: Int = 0,
    val isAutoIncrementing: Boolean = false,
    val autoIncrementIntervalMs: Long = 3000L,
    val showSettings: Boolean = false
)

// Define the custom green color
val androidGreen = Color(0xFF3DDC84)

// --- ViewModel to manage counter state and business logic ---
class CounterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    private var autoIncrementJob: Job? = null

    // --- Public actions that the UI can call ---

    fun increment() {
        _uiState.update { currentState ->
            currentState.copy(count = currentState.count + 1)
        }
    }

    fun decrement() {
        _uiState.update { currentState ->
            currentState.copy(count = currentState.count - 1)
        }
    }

    fun reset() {
        // Stop the coroutine if it is running
        if (_uiState.value.isAutoIncrementing) {
            stopAutoIncrement()
        }
        // Update the state to reset count and ensure auto-increment is off
        _uiState.update { currentState ->
            currentState.copy(
                count = 0,
                isAutoIncrementing = false // Explicitly set auto-increment to off
            )
        }
    }


    fun toggleAutoIncrement() {
        val isCurrentlyEnabled = _uiState.value.isAutoIncrementing
        _uiState.update { it.copy(isAutoIncrementing = !isCurrentlyEnabled) }

        if (!isCurrentlyEnabled) {
            startAutoIncrement()
        } else {
            stopAutoIncrement()
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun updateInterval(newIntervalSeconds: Long) {
        val newIntervalMs = newIntervalSeconds * 1000
        if (newIntervalMs > 0) {
            _uiState.update { it.copy(autoIncrementIntervalMs = newIntervalMs) }
            // If auto-increment is running, restart it with the new interval
            if (_uiState.value.isAutoIncrementing) {
                restartAutoIncrement()
            }
        }
    }

    // --- Private coroutine logic ---

    private fun startAutoIncrement() {
        // Cancel any existing job to prevent multiple coroutines running
        autoIncrementJob?.cancel()
        autoIncrementJob = viewModelScope.launch {
            while (true) {
                delay(_uiState.value.autoIncrementIntervalMs)
                increment()
                Log.d("CounterViewModel", "Auto-incremented to ${_uiState.value.count}")
            }
        }
    }

    private fun stopAutoIncrement() {
        autoIncrementJob?.cancel()
        autoIncrementJob = null
    }

    private fun restartAutoIncrement() {
        stopAutoIncrement()
        startAutoIncrement()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the coroutine when the ViewModel is destroyed
        stopAutoIncrement()
    }
}


class MainActivity : ComponentActivity() {
    // Use the by viewModels() delegate to get a reference to the ViewModel
    private val viewModel: CounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssignmentFourQ2Theme {
                // Collect the state from the ViewModel
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CounterScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onIncrement = viewModel::increment,
                        onDecrement = viewModel::decrement,
                        onReset = viewModel::reset,
                        onToggleAuto = viewModel::toggleAutoIncrement,
                        onSettingsClick = viewModel::openSettings
                    )

                    // Show settings screen as a dialog
                    if (uiState.showSettings) {
                        SettingsScreen(
                            currentIntervalSeconds = uiState.autoIncrementIntervalMs / 1000,
                            onDismiss = viewModel::closeSettings,
                            onIntervalSelected = viewModel::updateInterval
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CounterScreen(
    modifier: Modifier = Modifier,
    uiState: CounterUiState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onToggleAuto: () -> Unit,
    onSettingsClick: () -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Status Display ---
        Text(
            text = "Counter++",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Auto mode: ${if (uiState.isAutoIncrementing) "ON" else "OFF"}",
            style = MaterialTheme.typography.titleMedium,
            color = if (uiState.isAutoIncrementing) androidGreen else Color.Gray // Use green for status text
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Counter Display ---
        Text(
            text = "${uiState.count}",
            fontSize = 80.sp,
            fontWeight = FontWeight.ExtraBold,
            color = androidGreen
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- Action Buttons (Updated) ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onDecrement,
                modifier = Modifier.sizeIn(minWidth = 80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidGreen) // Apply color
            ) {
                Text("-1", fontSize = 20.sp)
            }
            Button(
                onClick = onIncrement,
                modifier = Modifier.sizeIn(minWidth = 80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidGreen) // Apply color
            ) {
                Text("+1", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Control Buttons (Updated) ---
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReset, colors = ButtonDefaults.buttonColors(containerColor = androidGreen)) {
                Text("Reset")
            }
            Button(
                onClick = onToggleAuto,
                colors = ButtonDefaults.buttonColors(containerColor = androidGreen) // Apply color
            ) {
                Text(if (uiState.isAutoIncrementing) "Stop Auto" else "Start Auto")
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // --- Settings Button ---
        OutlinedButton(onClick = onSettingsClick) {
            Text("Settings")
        }
    }
}

@Composable
fun SettingsScreen(
    currentIntervalSeconds: Long,
    onDismiss: () -> Unit,
    onIntervalSelected: (Long) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentIntervalSeconds.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("interval: ${sliderPosition.toInt()} seconds")
                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        sliderPosition = newValue
                    },
                    valueRange = 1f..8f,
                    steps = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onIntervalSelected(sliderPosition.toLong())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidGreen)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    AssignmentFourQ2Theme {
        CounterScreen(
            uiState = CounterUiState(count = 99, isAutoIncrementing = true),
            onIncrement = {},
            onDecrement = {},
            onReset = {},
            onToggleAuto = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AssignmentFourQ2Theme {
        SettingsScreen(
            currentIntervalSeconds = 5,
            onDismiss = {},
            onIntervalSelected = {}
        )
    }
}
