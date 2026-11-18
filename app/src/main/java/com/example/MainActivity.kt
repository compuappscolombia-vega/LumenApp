package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.LOCAL_GUEST_UID
import com.example.data.repository.StudioRepository
import com.example.security.configureAppCheck
import com.example.ui.screens.MainStudioScreen
import com.example.ui.theme.LumenReelTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            LumenReelTheme {
                LumenReelRoot(
                    onShareFile = { file, mimeType ->
                        shareExportedFile(file, mimeType)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }

    private fun shareExportedFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir expediente LumenReel"))
    }
}

@Composable
private fun LumenReelRoot(
    onShareFile: (File, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authRepository = remember {
        AuthRepository(StudioRepository(context.applicationContext))
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var authStatusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val activeUserId = currentUser?.uid ?: LOCAL_GUEST_UID
    key(activeUserId) {
        MainStudioScreen(
            currentUser = currentUser,
            authStatusMessage = authStatusMessage,
            onClearAuthStatus = { authStatusMessage = null },
            onOptionalGoogleSignIn = {
                scope.launch {
                    authStatusMessage = null
                    authRepository.signInInteractive(context)
                        .onSuccess {
                            currentUser = Firebase.auth.currentUser
                        }
                        .onFailure { err ->
                            authStatusMessage = err.localizedMessage
                                ?: "No se pudo conectar la cuenta de Google."
                        }
                }
            },
            onSignOut = {
                scope.launch {
                    authRepository.signOut(context)
                    currentUser = Firebase.auth.currentUser
                }
            },
            onShareFile = onShareFile
        )
    }
}
