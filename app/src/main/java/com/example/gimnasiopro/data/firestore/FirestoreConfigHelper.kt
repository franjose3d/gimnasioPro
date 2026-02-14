package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Abstracción sobre Firestore para facilitar pruebas.
 */
interface FirestoreService {
    suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long
    suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long
}

/**
 * Implementación real que usa FirebaseFirestore.
 */
class FirebaseFirestoreService(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) : FirestoreService {
    override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
        return withTimeout(timeoutMs) {
            val snapshot = firestore.collection(collectionPath).limit(1).get().await()
            snapshot.size().toLong()
        }
    }

    override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
        return withTimeout(timeoutMs) {
            // documentPath expected like "users/{userId}" -> get reference
            val parts = documentPath.split('/')
            var ref = firestore.collection(parts[0]).document(parts[1])
            // si el path tiene más segmentos, navegarlos
            if (parts.size > 2) {
                for (i in 2 until parts.size) {
                    ref = ref.collection(parts[i]).document() // no-op, sólo por compatibilidad; no debería pasar
                }
            }
            val snapshot = ref.collection(subcollectionName).limit(1).get().await()
            snapshot.size().toLong()
        }
    }
}

/**
 * Helper para verificar la configuración de Firestore.
 * Ahora inyectable para pruebas.
 */
class FirestoreConfigHelper(private val service: FirestoreService, private val timeoutMs: Long = DEFAULT_TIMEOUT_MS) {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 10_000L // 10 segundos

        // Factory por conveniencia para uso en producción
        fun createDefault(timeoutMs: Long = DEFAULT_TIMEOUT_MS) = FirestoreConfigHelper(FirebaseFirestoreService(), timeoutMs)
    }

    private val TAG = "FirestoreConfig"

    suspend fun verificarConfiguracion(): ConfiguracionResult {
        return try {
            val ejerciciosCount = service.getCollectionSize("ejercicios", timeoutMs)

            Log.d(TAG, "✅ Conexión a Firestore OK")
            Log.d(TAG, "✅ Reglas de seguridad: OK (puede leer ejercicios)")
            Log.d(TAG, "📊 Ejercicios en Firestore: $ejerciciosCount")

            ConfiguracionResult(
                conexionOk = true,
                reglasOk = true,
                ejerciciosCount = ejerciciosCount,
                mensaje = "Configuración correcta"
            )
        } catch (e: Exception) {
            val mensaje = when (e) {
                is FirebaseFirestoreException -> when (e.code) {
                    Code.PERMISSION_DENIED, Code.UNAUTHENTICATED ->
                        "❌ Error: Reglas de seguridad no configuradas o acceso no autenticado"
                    Code.UNAVAILABLE ->
                        "❌ Error: Servicio de Firestore no disponible (server error)"
                    else ->
                        "❌ Error de Firestore: ${e.message}"
                }
                is TimeoutCancellationException, is CancellationException ->
                    "❌ Error: Operación excedió el tiempo de espera ($timeoutMs ms)"
                is IOException ->
                    "❌ Error: Problema de conexión a Internet"
                else ->
                    // fallback seguro
                    "❌ Error inesperado: ${e.message ?: e.javaClass.simpleName}"
            }

            Log.e(TAG, mensaje, e)

            val reglasOk = !(e is FirebaseFirestoreException && (e.code == Code.PERMISSION_DENIED || e.code == Code.UNAUTHENTICATED))

            ConfiguracionResult(
                conexionOk = false,
                reglasOk = reglasOk,
                ejerciciosCount = 0,
                mensaje = mensaje
            )
        }
    }

    suspend fun verificarEstructuraUsuario(userId: String): EstructuraUsuarioResult {
        val documentPath = "users/$userId"
        return try {
            val rutinasCount = service.getSubcollectionSize(documentPath, "rutinas", timeoutMs)
            val calendarioCount = service.getSubcollectionSize(documentPath, "calendario", timeoutMs)
            val entrenamientosCount = service.getSubcollectionSize(documentPath, "entrenamientos", timeoutMs)
            val estadisticasCount = service.getSubcollectionSize(documentPath, "estadisticas", timeoutMs)

            val rutinasOk = rutinasCount > 0
            val calendarioOk = calendarioCount > 0
            val entrenamientosOk = entrenamientosCount > 0
            val estadisticasOk = estadisticasCount > 0

            Log.d(TAG, "✅ Estructura de usuario verificada:")
            Log.d(TAG, "   - Rutinas: $rutinasOk")
            Log.d(TAG, "   - Calendario: $calendarioOk")
            Log.d(TAG, "   - Entrenamientos: $entrenamientosOk")
            Log.d(TAG, "   - Estadísticas: $estadisticasOk")

            EstructuraUsuarioResult(
                rutinasOk = rutinasOk,
                calendarioOk = calendarioOk,
                entrenamientosOk = entrenamientosOk,
                estadisticasOk = estadisticasOk,
                mensaje = "Estructura verificada"
            )
        } catch (e: Exception) {
            val mensaje = when (e) {
                is FirebaseFirestoreException -> when (e.code) {
                    Code.PERMISSION_DENIED, Code.UNAUTHENTICATED ->
                        "❌ Error: Reglas de seguridad no permiten leer la estructura del usuario"
                    Code.UNAVAILABLE ->
                        "❌ Error: Servicio de Firestore no disponible"
                    else ->
                        "❌ Error de Firestore: ${e.message}"
                }
                is TimeoutCancellationException, is CancellationException ->
                    "❌ Error: Operación excedió el tiempo de espera ($timeoutMs ms)"
                is IOException ->
                    "❌ Error: Problema de conexión a Internet"
                else ->
                    "❌ Error inesperado: ${e.message ?: e.javaClass.simpleName}"
            }

            Log.e(TAG, "❌ Error al verificar estructura: $mensaje", e)
            EstructuraUsuarioResult(
                rutinasOk = false,
                calendarioOk = false,
                entrenamientosOk = false,
                estadisticasOk = false,
                mensaje = mensaje
            )
        }
    }

    data class ConfiguracionResult(
        val conexionOk: Boolean,
        val reglasOk: Boolean,
        val ejerciciosCount: Long,
        val mensaje: String
    )

    data class EstructuraUsuarioResult(
        val rutinasOk: Boolean,
        val calendarioOk: Boolean,
        val entrenamientosOk: Boolean,
        val estadisticasOk: Boolean,
        val mensaje: String
    )
}
