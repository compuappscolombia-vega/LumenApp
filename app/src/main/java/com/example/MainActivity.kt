package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

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
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        AuthRepository(StudioRepository(dbId))
    }

    val currentUser by remember {
        Firebase.auth.authStateFlow()
    }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            authRepository = authRepository,
            onSignedIn = { /* authStateFlow updates automatically on Firebase.auth sign-in */ }
        )
    } else {
        key(user.uid) {
            MainStudioScreen(
                currentUser = user,
                onSignOut = {
                    scope.launch {
                        authRepository.signOut(context)
                    }
                },
                onShareFile = onShareFile
            )
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}
