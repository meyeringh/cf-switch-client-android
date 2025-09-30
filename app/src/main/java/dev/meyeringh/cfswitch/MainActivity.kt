package dev.meyeringh.cfswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.meyeringh.cfswitch.ui.CfSwitchScreen
import dev.meyeringh.cfswitch.ui.theme.CfSwitchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = ServiceLocator.providePreferences(this)

        setContent {
            var viewModelKey by remember { mutableStateOf(0) }

            // Recreate repository and viewModel when key changes
            val repository = remember(viewModelKey) {
                ServiceLocator.resetRepository()
                ServiceLocator.provideRepository(this)
            }
            val viewModel = remember(viewModelKey) {
                CfSwitchViewModel(repository)
            }

            CfSwitchTheme {
                CfSwitchScreen(
                    viewModel = viewModel,
                    preferences = preferences,
                    onSettingsSaved = {
                        // Trigger recomposition with new repository
                        viewModelKey++
                    }
                )
            }
        }
    }
}
