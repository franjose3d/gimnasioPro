package com.example.gimnasiopro.data.firestore

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
class FirebaseFirestoreService(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
) : FirestoreService {
    override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
        return withTimeout(timeoutMs) {
            val snapshot = firestore.collection(collectionPath).limit(1).get().await()
            snapshot.size().toLong()
        }
    }

    override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
        return withTimeout(timeoutMs) {
            val parts = documentPath.split('/')
            var ref = firestore.collection(parts[0]).document(parts[1])
            if (parts.size > 2) {
                for (i in 2 until parts.size) {
                    ref = ref.collection(parts[i]).document()
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

    private fun log(level: String, msg: String, e: Exception? = null) {
        try {
            val logClass = Class.forName("android.util.Log")
            if (level == "e") {
                logClass.getMethod("e", String::class.java, String::class.java, Throwable::class.java)
                    .invoke(null, TAG, msg, e)
            } else {
                logClass.getMethod("d", String::class.java, String::class.java)
                    .invoke(null, TAG, msg)
            }
        } catch (_: Exception) {
            // En tests JVM no hay android.util.Log disponible
        }
    }

    private fun classifyException(e: Exception): Pair<String, Boolean> {
        val className = e.javaClass.name
        // Check if it's a FirebaseFirestoreException without loading the class directly
        if (className.contains("FirebaseFirestoreException")) {
            val codeStr = try {
                val codeMethod = e.javaClass.getMethod("getCode")
                codeMethod.invoke(e)?.toString() ?: ""
            } catch (_: Exception) { "" }

            val reglasOk = !(codeStr == "PERMISSION_DENIED" || codeStr == "UNAUTHENTICATED")
            val mensaje = when (codeStr) {
                "PERMISSION_DENIED", "UNAUTHENTICATED" ->
                    "❌ Error: Reglas de seguridad no configuradas o acceso no autenticado"
                "UNAVAILABLE" ->
                    "❌ Error: Servicio de Firestore no disponible (server error)"
                else ->
                    "❌ Error de Firestore: ${e.message}"
            }
            return Pair(mensaje, reglasOk)
        }

        return when (e) {
            is TimeoutCancellationException, is CancellationException ->
                Pair("❌ Error: Operación excedió el tiempo de espera ($timeoutMs ms)", true)
            is IOException ->
                Pair("❌ Error: Problema de conexión a Internet", true)
            else ->
                Pair("❌ Error inesperado: ${e.message ?: e.javaClass.simpleName}", true)
        }
    }

    suspend fun verificarConfiguracion(): ConfiguracionResult {
        return try {
            val ejerciciosCount = service.getCollectionSize("ejercicios", timeoutMs)

            log("d", "✅ Conexión a Firestore OK")
            log("d", "✅ Reglas de seguridad: OK (puede leer ejercicios)")
            log("d", "📊 Ejercicios en Firestore: $ejerciciosCount")

            ConfiguracionResult(
                conexionOk = true,
                reglasOk = true,
                ejerciciosCount = ejerciciosCount,
                mensaje = "Configuración correcta"
            )
        } catch (e: Exception) {
            val (mensaje, reglasOk) = classifyException(e)
            log("e", mensaje, e)

            ConfiguracionResult(
                conexionOk = false,
                reglasOk = reglasOk,
                ejerciciosCount = 0,
                mensaje = mensaje
            )
        }
    }

    suspend fun verificarEstructuraUsuario(userId: String, tipoUsuario: String = "cliente"): EstructuraUsuarioResult {
        val coleccion = if (tipoUsuario == "trainer") "trainers" else "clientes"
        val documentPath = "$coleccion/$userId"
        return try {
            val rutinasCount = service.getSubcollectionSize(documentPath, "rutinas", timeoutMs)
            val calendarioCount = service.getSubcollectionSize(documentPath, "calendario", timeoutMs)
            val entrenamientosCount = service.getSubcollectionSize(documentPath, "entrenamientos", timeoutMs)
            val estadisticasCount = service.getSubcollectionSize(documentPath, "estadisticas", timeoutMs)

            val rutinasOk = rutinasCount > 0
            val calendarioOk = calendarioCount > 0
            val entrenamientosOk = entrenamientosCount > 0
            val estadisticasOk = estadisticasCount > 0

            log("d", "✅ Estructura de usuario verificada:")
            log("d", "   - Rutinas: $rutinasOk")
            log("d", "   - Calendario: $calendarioOk")
            log("d", "   - Entrenamientos: $entrenamientosOk")
            log("d", "   - Estadísticas: $estadisticasOk")

            EstructuraUsuarioResult(
                rutinasOk = rutinasOk,
                calendarioOk = calendarioOk,
                entrenamientosOk = entrenamientosOk,
                estadisticasOk = estadisticasOk,
                mensaje = "Estructura verificada"
            )
        } catch (e: Exception) {
            val (mensaje, _) = classifyException(e)
            log("e", "❌ Error al verificar estructura: $mensaje", e)
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
