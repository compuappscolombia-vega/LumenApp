package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudioRepository
import com.example.security.configureAppCheck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainStudioScreen
import com.example.ui.theme.LumenReelTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            LumenReelRoot(
                onShareFile = { file, mimeType ->
                    shareExportedFile(file, mimeType)
                }
            )
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

    val studioRepository = remember {
        StudioRepository.getInstance(context.applicationContext)
    }
    val authRepository = remember {
        AuthRepository(studioRepository)
    }
    val isDarkMode by studioRepository.observeGlobalThemeDark()
        .collectAsStateWithLifecycle(initialValue = studioRepository.getCachedThemeDark())

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var persistedSession by remember { mutableStateOf(authRepository.getSessionSnapshot(context)) }
    var authStatusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val nextUser = auth.currentUser
            if (nextUser?.uid != currentUser?.uid) {
                studioRepository.clearSessionCache()
            }
            currentUser = nextUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser?.uid) {
        val user = currentUser
        if (user != null) {
            persistedSession = authRepository.recordSignedInSession(context, user)
            withContext(Dispatchers.IO) {
                studioRepository.ensureUserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Director LumenReel",
                    email = user.email ?: "${user.uid}@lumenreel.local"
                )
            }
        }
    }

    LumenReelTheme(darkTheme = isDarkMode) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                authRepository = authRepository,
                onSignedIn = {
                    val signedIn = Firebase.auth.currentUser
                    if (signedIn != null) {
                        persistedSession = authRepository.recordSignedInSession(context, signedIn)
                    }
                    currentUser = signedIn
                }
            )
        } else {
            key(user.uid) {
                MainStudioScreen(
                    currentUser = user,
                    persistedSession = persistedSession,
                    authStatusMessage = authStatusMessage,
                    onClearAuthStatus = { authStatusMessage = null },
                    onOptionalGoogleSignIn = {
                        scope.launch {
                            authStatusMessage = null
                            authRepository.signInInteractive(context)
                                .onSuccess { signedIn ->
                                    persistedSession = authRepository.recordSignedInSession(context, signedIn)
                                    currentUser = Firebase.auth.currentUser
                                }
                                .onFailure { err ->
                                    authStatusMessage = err.localizedMessage
                                        ?: "No se pudo conectar la cuenta de Google."
                                }
                        }
                    },
                    onResyncGoogleSession = {
                        scope.launch {
                            authStatusMessage = null
                            authRepository.resyncActiveAccount(context)
                                .onSuccess { synced ->
                                    persistedSession = authRepository.recordSignedInSession(context, synced)
                                    currentUser = Firebase.auth.currentUser
                                    authStatusMessage = "Sesión Google y Cloud Firestore sincronizadas (${synced.email ?: synced.uid})."
                                }
                                .onFailure { err ->
                                    authStatusMessage = err.localizedMessage
                                        ?: "No se pudo sincronizar la sesión de Google."
                                }
                        }
                    },
                    onSignOut = {
                        scope.launch {
                            authRepository.signOut(context)
                            persistedSession = authRepository.getSessionSnapshot(context)
                            currentUser = Firebase.auth.currentUser
                        }
                    },
                    onShareFile = onShareFile
                )
            }
        }
    }
}
