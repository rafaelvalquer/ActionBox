package com.luminor.actionbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.navigation.ActionBoxRoot
import com.luminor.actionbox.ui.capture.CaptureViewModel
import com.luminor.actionbox.ui.designsystem.ActionBoxTheme

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: com.luminor.actionbox.ui.RootViewModel by viewModels()
    private val captureViewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings = viewModel.settings.collectAsStateWithLifecycle().value
            ActionBoxTheme(themeMode = settings.themeMode) {
                ActionBoxRoot(viewModel = viewModel, captureViewModel = captureViewModel)
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (text.isNotBlank()) captureViewModel.processInput(text, fromShare = true)
        }
    }
}
