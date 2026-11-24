package com.example.data.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("hasUser", currentUser != null)
        put("emailVerified", currentUser?.isEmailVerified)
        put("providerCount", providerInfoList.size)
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirestoreMessage(exception: Throwable): String {
    val firestoreEx = generateSequence(exception) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    val raw = firestoreEx?.message ?: exception.localizedMessage ?: exception.toString()
    return when {
        raw.contains("Quota exceeded", ignoreCase = true) ->
            "Cuota diaria de lectura en Firestore alcanzada. La cuota se restablece mañana o puedes actualizar tu plan Blaze en Firebase Console."
        firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Permisos insuficientes en Firestore para esta operación. Verifica tu sesión de Google."
        firestoreEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sin conexión temporal con Firestore. Tus datos locales se sincronizarán al recuperar red."
        else -> raw
    }
}
