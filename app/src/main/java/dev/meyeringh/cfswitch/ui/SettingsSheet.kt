package dev.meyeringh.cfswitch.ui

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(sheetState: SheetState, preferences: SharedPreferences, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var baseUrl by remember {
        mutableStateOf(preferences.getString("base_url", "") ?: "")
    }
    var apiToken by remember {
        mutableStateOf(preferences.getString("api_token", "") ?: "")
    }

    val isEmulator = Build.FINGERPRINT.contains("generic") ||
        Build.FINGERPRINT.contains("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://your-host/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (isEmulator) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tip: Use http://10.0.2.2:8080 for host machine",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiToken,
                onValueChange = { apiToken = it },
                label = { Text("API Token") },
                placeholder = { Text("Bearer token") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    preferences.edit()
                        .putString("base_url", baseUrl)
                        .putString("api_token", apiToken)
                        .apply()

                    scope.launch {
                        sheetState.hide()
                        onSaved()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = baseUrl.isNotEmpty() && apiToken.isNotEmpty()
            ) {
                Text("Save")
            }
        }
    }
}
