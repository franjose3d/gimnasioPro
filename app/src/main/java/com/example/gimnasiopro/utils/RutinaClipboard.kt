package com.example.gimnasiopro.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper para copiar/pegar rutinas usando SharedPreferences.
 * Actúa como un "portapapeles" de rutinas.
 */
object RutinaClipboard {

    private const val PREFS_NAME = "rutina_clipboard"
    private const val KEY_TIENE_RUTINA = "tiene_rutina_copiada"
    private const val KEY_NUMERO_RUTINA = "numero_rutina"
    private const val KEY_NOMBRE_RUTINA = "nombre_rutina"
    private const val KEY_EJERCICIO_IDS = "ejercicio_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Copia una rutina al portapapeles.
     */
    fun copiar(
        context: Context,
        numeroRutina: Int,
        nombreRutina: String,
        ejercicioIds: List<String>
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_TIENE_RUTINA, true)
            putInt(KEY_NUMERO_RUTINA, numeroRutina)
            putString(KEY_NOMBRE_RUTINA, nombreRutina)
            putString(KEY_EJERCICIO_IDS, ejercicioIds.joinToString(","))
            apply()
        }
    }

    /**
     * Verifica si hay una rutina copiada.
     */
    fun tieneRutinaCopiada(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TIENE_RUTINA, false)
    }

    /**
     * Obtiene la rutina copiada.
     */
    fun obtenerRutinaCopiada(context: Context): RutinaCopiada? {
        val prefs = getPrefs(context)

        if (!prefs.getBoolean(KEY_TIENE_RUTINA, false)) {
            return null
        }

        val numeroRutina = prefs.getInt(KEY_NUMERO_RUTINA, 0)
        val nombreRutina = prefs.getString(KEY_NOMBRE_RUTINA, null) ?: return null
        val ejercicioIdsStr = prefs.getString(KEY_EJERCICIO_IDS, "") ?: ""
        val ejercicioIds = if (ejercicioIdsStr.isEmpty()) {
            emptyList()
        } else {
            ejercicioIdsStr.split(",")
        }

        return RutinaCopiada(numeroRutina, nombreRutina, ejercicioIds)
    }

    /**
     * Limpia el portapapeles.
     */
    fun limpiar(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    /**
     * Clase que representa una rutina copiada.
     */
    data class RutinaCopiada(
        val numeroRutina: Int,
        val nombreRutina: String,
        val ejercicioIds: List<String>
    )
}