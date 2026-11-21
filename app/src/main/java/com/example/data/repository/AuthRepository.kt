package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class AuthRepository(private val studioRepository: StudioRepository) {

    suspend fun attemptAutoSignIn(context: Context): FirebaseUser? {
        Firebase.auth.currentUser?.let { return it }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id unavailable for silent sign-in", e)
            return null
        }

        val activityContext: Context = context.findActivity() ?: context
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = activityContext, request = request)
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
            Log.w(TAG, "Silent auto sign-in did not complete (${e.javaClass.simpleName}): ${e.message}", e)
            null
        }
    }

    suspend fun signInInteractive(context: Context): Result<FirebaseUser> {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "Missing R.string.default_web_client_id", e)
            return Result.failure(
                IllegalStateException("Falta configuración de Google Sign-In (default_web_client_id).")
            )
        }

        val activity = context.findActivity()
        if (activity == null) {
            Log.e(TAG, "Could not unwrap Activity from Context (${context.javaClass.name})")
        }
        val requestContext: Context = activity ?: context

        val credentialManager = CredentialManager.create(requestContext)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = requestContext, request = request)
            val cred = result.credential
            if (cred !is CustomCredential || cred.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(
                    IllegalStateException("Credencial no compatible devuelta por Credential Manager.")
                )
            }
            val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(
                    IllegalStateException("Firebase Auth no devolvió un usuario activo.")
                )

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
            Result.failure(
                CancelledAuthException(
                    "Inicio de sesión cancelado o rechazado por Google Play Services. Si no ves el selector de cuentas, verifica que tengas una cuenta de Google activa en los Ajustes del dispositivo o emulador y vuelve a intentarlo."
                )
            )
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account/credentials available on device: ${e.message}", e)
            Result.failure(
                IllegalStateException(
                    "No hay ninguna cuenta de Google disponible en este dispositivo/emulador. Agrega una cuenta de Google en Ajustes > Cuentas (o inicia sesión en Play Store) e inténtalo de nuevo."
                )
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Credential Manager provider configuration error: ${e.message}", e)
            Result.failure(
                IllegalStateException(
                    "El proveedor de credenciales de Google Play Services no está disponible o requiere actualización en este dispositivo (${e.localizedMessage ?: "error de configuración"})."
                )
            )
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(TAG, "Credential Manager unsupported on device: ${e.message}", e)
            Result.failure(
                IllegalStateException(
                    "Credential Manager no es compatible con este entorno (${e.localizedMessage ?: "sin soporte de Play Services"})."
                )
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager failure (${e.type}): ${e.message}", e)
            Result.failure(
                IllegalStateException(
                    "Error al obtener credencial de Google (${e.type}): ${e.localizedMessage ?: "revisa tu conexión e inténtalo de nuevo"}."
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Interactive Google Sign-In failed (${e.javaClass.simpleName})", e)
            Result.failure(
                IllegalStateException(
                    "No se pudo completar el inicio de sesión con Google (${e.javaClass.simpleName}): ${e.localizedMessage ?: "error desconocido"}."
                )
            )
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            val requestContext = context.findActivity() ?: context
            try {
                studioRepository.clearSessionCache()
                Firebase.auth.signOut()
                CredentialManager.create(requestContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Error during CredentialManager clearCredentialState", e)
            }
        }
    }

    class CancelledAuthException(message: String) : Exception(message)
}
