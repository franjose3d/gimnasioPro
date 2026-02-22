package com.example.gimnasiopro.utils

import android.content.Context
import android.util.Log
import com.example.gimnasiopro.GimnasioproApplication
import com.example.gimnasiopro.data.firestore.FirestoreConfigHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Utilidades para probar y verificar la recuperación de ejercicios.
 */
object RecoveryTestUtils {

    private const val TAG = "RecoveryTest"

    /**
     * Simula la pérdida de datos locales y prueba la recuperación desde Firebase.
     */
    fun testRecoveryFromFirebase(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as GimnasioproApplication

            Log.d(TAG, "🧪 INICIANDO PRUEBA DE RECUPERACIÓN")

            try {
                // 1. Verificar estado de Firebase
                val configHelper = FirestoreConfigHelper.createDefault()
                val configResult = configHelper.verificarConfiguracion()

                Log.d(TAG, "📊 Ejercicios en Firebase: ${configResult.ejerciciosCount}")
                Log.d(TAG, "🔗 Conexión Firebase: ${if (configResult.conexionOk) "OK" else "FALLO"}")

                if (configResult.ejerciciosCount > 0) {
                    // 2. Borrar datos locales para simular pérdida
                    Log.d(TAG, "🗑️ Simulando pérdida de datos locales...")
                    app.localEjercicioRepository.deleteAllEjercicios()

                    val localCount = app.localEjercicioRepository.getEjerciciosCount()
                    Log.d(TAG, "🏠 Ejercicios en Room después de limpiar: $localCount")

                    // 3. Probar recuperación híbrida
                    Log.d(TAG, "🔄 Iniciando recuperación desde Firebase...")
                    val ejerciciosRecuperados = app.ejercicioRepository.allEjercicios.first()

                    Log.d(TAG, "✅ RECUPERACIÓN EXITOSA: ${ejerciciosRecuperados.size} ejercicios")
                    Log.d(TAG, "🏠 Room repoblado con ${app.localEjercicioRepository.getEjerciciosCount()} ejercicios")

                } else {
                    Log.w(TAG, "❌ Firebase vacío - no se puede probar recuperación")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en prueba de recuperación: ${e.message}", e)
            }
        }
    }

    /**
     * Verifica el estado actual del sistema híbrido.
     */
    fun checkSystemStatus(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as GimnasioproApplication

            Log.d(TAG, "📊 ESTADO DEL SISTEMA HÍBRIDO")

            try {
                // Room count
                val roomCount = app.localEjercicioRepository.getEjerciciosCount()
                Log.d(TAG, "🏠 Ejercicios en Room: $roomCount")

                // Firebase count
                val configHelper = FirestoreConfigHelper.createDefault()
                val configResult = configHelper.verificarConfiguracion()
                Log.d(TAG, "🔥 Ejercicios en Firebase: ${configResult.ejerciciosCount}")

                // Híbrido count
                val hibridoCount = app.ejercicioRepository.allEjercicios.first().size
                Log.d(TAG, "🔗 Ejercicios vía híbrido: $hibridoCount")

                Log.d(TAG, "🎯 Sistema ${if (roomCount > 0 || configResult.ejerciciosCount > 0) "SALUDABLE" else "REQUIERE ATENCIÓN"}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error verificando estado: ${e.message}", e)
            }
        }
    }
}

