package com.example.gimnasiopro.utils

import android.view.View

/**
 * Helper para manejar la lógica del badge de notificaciones.
 * Esta clase contiene lógica pura (sin dependencias de Android) para facilitar testing.
 */
object BadgeHelper {

    /**
     * Obtiene el texto que debe mostrarse en el badge.
     *
     * @param count Número de mensajes no leídos
     * @return Texto a mostrar ("1", "2"... "9", "9+")
     */
    fun obtenerTextoBadge(count: Int): String {
        return if (count <= 9) {
            count.toString()
        } else {
            "9+"
        }
    }

    /**
     * Determina si el badge debe estar visible.
     *
     * @param count Número de mensajes no leídos
     * @return true si debe estar visible, false si debe ocultarse
     */
    fun debeEstarVisible(count: Int): Boolean {
        return count > 0
    }

    /**
     * Obtiene la visibilidad de Android (View.VISIBLE o View.GONE).
     *
     * @param count Número de mensajes no leídos
     * @return View.VISIBLE si count > 0, View.GONE si count = 0
     */
    fun obtenerVisibilidad(count: Int): Int {
        return if (count > 0) View.VISIBLE else View.GONE
    }
}