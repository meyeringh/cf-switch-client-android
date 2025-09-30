package dev.meyeringh.cfswitch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.meyeringh.cfswitch.CfSwitchViewModel
import dev.meyeringh.cfswitch.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CfSwitchScreen(
    viewModel: CfSwitchViewModel,
    preferences: android.content.SharedPreferences,
    onSettingsSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Check if settings are configured on first launch
    LaunchedEffect(Unit) {
        val baseUrl = preferences.getString("base_url", "") ?: ""
        val token = preferences.getString("api_token", "") ?: ""
        if (baseUrl.isEmpty() || token.isEmpty()) {
            showSettings = true
        } else {
            viewModel.loadState()
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (message == "Network error") "Retry" else null
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.loadState()
            }
            viewModel.clearError()
        }
    }

    if (showSettings) {
        SettingsSheet(
            sheetState = sheetState,
            preferences = preferences,
            onDismiss = { showSettings = false },
            onSaved = {
                onSettingsSaved()
                viewModel.loadState()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CF Switch") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (state.enabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (state.enabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.semantics { contentDescription = "Rule state" }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.toggleState() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .semantics { contentDescription = "Toggle rule" }
                        ) {
                            Text(
                                text = if (state.enabled) "Disable" else "Enable",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadState() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}
