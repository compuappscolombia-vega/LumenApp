package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthRepository"

class AuthRepository(private val studioRepository: StudioRepository) {

    suspend fun attemptAutoSignIn(context: Context): FirebaseUser? {
        Firebase.auth.currentUser?.let { return it }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id unavailable for silent sign-in", e)
            return null
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    withContext(Dispatchers.IO) {
                        studioRepository.ensureUserProfile(
                            userId = user.uid,
                            displayName = user.displayName ?: "Director LumenReel",
                            email = user.email ?: "${user.uid}@lumenreel.local"
                        )
                    }
                }
                user
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No cached authorized Google account for silent sign-in.")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Silent auto sign-in did not complete: ${e.message}", e)
            null
        }
    }

    suspend fun signInInteractive(context: Context): Result<FirebaseUser> {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "Missing R.string.default_web_client_id", e)
            return Result.failure(IllegalStateException("Falta configuración de Google Sign-In (default_web_client_id)."))
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val cred = result.credential
            if (cred !is CustomCredential || cred.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(IllegalStateException("Credencial no compatible devuelta por Credential Manager."))
            }
            val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase Auth no devolvió un usuario activo."))

            withContext(Dispatchers.IO) {
                studioRepository.ensureUserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Director LumenReel",
                    email = user.email ?: "${user.uid}@lumenreel.local"
                )
            }
            Result.success(user)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            Result.failure(CancelledAuthException("Inicio de sesión cancelado por el usuario."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                Firebase.auth.signOut()
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Error during CredentialManager clearCredentialState", e)
            }
        }
    }

    class CancelledAuthException(message: String) : Exception(message)
}
