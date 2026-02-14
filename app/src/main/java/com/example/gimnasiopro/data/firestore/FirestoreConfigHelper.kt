package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper para verificar la configuración de Firestore.
 * Útil para debugging y verificar que todo esté configurado correctamente.
 */
object FirestoreConfigHelper {

    private const val TAG = "FirestoreConfig"

    /**
     * Verificar que las reglas de seguridad estén configuradas
     * (solo verifica que se pueda leer, no las reglas específicas)
     */
    suspend fun verificarConfiguracion(): ConfiguracionResult {
        val firestore = FirebaseFirestore.getInstance()
        
        return try {
            // Intentar leer la colección de ejercicios (debe estar permitido para usuarios autenticados)
            val ejerciciosSnapshot = firestore.collection("ejercicios")
                .limit(1)
                .get()
                .await()
            
            Log.d(TAG, "✅ Conexión a Firestore OK")
            Log.d(TAG, "✅ Reglas de seguridad: OK (puede leer ejercicios)")
            Log.d(TAG, "📊 Ejercicios en Firestore: ${ejerciciosSnapshot.size()}")
            
            ConfiguracionResult(
                conexionOk = true,
                reglasOk = true,
                ejerciciosCount = ejerciciosSnapshot.size().toLong(),
                mensaje = "Configuración correcta"
            )
        } catch (e: Exception) {
            val mensaje = when {
                e.message?.contains("permission") == true || 
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "❌ Error: Reglas de seguridad no configuradas o incorrectas"
                e.message?.contains("network") == true ->
                    "❌ Error: Problema de conexión a Internet"
                else ->
                    "❌ Error: ${e.message}"
            }
            
            Log.e(TAG, mensaje, e)
            
            ConfiguracionResult(
                conexionOk = false,
                reglasOk = false,
                ejerciciosCount = 0,
                mensaje = mensaje
            )
        }
    }

    /**
     * Verificar estructura de datos de un usuario
     */
    suspend fun verificarEstructuraUsuario(userId: String): EstructuraUsuarioResult {
        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(userId)
        
        return try {
            val rutinasSnapshot = userDoc.collection("rutinas").limit(1).get().await()
            val calendarioSnapshot = userDoc.collection("calendario").limit(1).get().await()
            val entrenamientosSnapshot = userDoc.collection("entrenamientos").limit(1).get().await()
            val estadisticasSnapshot = userDoc.collection("estadisticas").limit(1).get().await()
            
            Log.d(TAG, "✅ Estructura de usuario verificada:")
            Log.d(TAG, "   - Rutinas: ${rutinasSnapshot.size() > 0}")
            Log.d(TAG, "   - Calendario: ${calendarioSnapshot.size() > 0}")
            Log.d(TAG, "   - Entrenamientos: ${entrenamientosSnapshot.size() > 0}")
            Log.d(TAG, "   - Estadísticas: ${estadisticasSnapshot.size() > 0}")
            
            EstructuraUsuarioResult(
                rutinasOk = true,
                calendarioOk = true,
                entrenamientosOk = true,
                estadisticasOk = true,
                mensaje = "Estructura correcta"
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar estructura: ${e.message}", e)
            EstructuraUsuarioResult(
                rutinasOk = false,
                calendarioOk = false,
                entrenamientosOk = false,
                estadisticasOk = false,
                mensaje = "Error: ${e.message}"
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
