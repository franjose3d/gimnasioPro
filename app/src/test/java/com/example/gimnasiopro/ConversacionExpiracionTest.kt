package com.example.gimnasiopro

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la lógica de expiración de conversaciones.
 *
 * Replica la misma aritmética de ConversacionViewModel.calcularExpiracion():
 *   expiresAt   = primerMensaje + 7 días
 *   msRestantes = expiresAt - ahora
 *   diasRestantes = if (msRestantes <= 0) -1 else (msRestantes / DAY_MS).toInt()
 *
 * No requiere dispositivo ni contexto Android.
 */
class ConversacionExpiracionTest {

    private val DAY_MS = 24 * 60 * 60 * 1000L

    /** Réplica de la lógica interna del ViewModel. */
    private fun calcularDiasRestantes(primerMensajeMs: Long, ahoraMs: Long): Int {
        val expiresAt   = primerMensajeMs + 7L * DAY_MS
        val msRestantes = expiresAt - ahoraMs
        return if (msRestantes <= 0) -1 else (msRestantes / DAY_MS).toInt()
    }

    // ── Casos básicos ────────────────────────────────────────────────────────

    @Test
    fun mensajeRecienEnviado_retorna7Dias() {
        val ahora = System.currentTimeMillis()
        assertEquals(7, calcularDiasRestantes(ahora, ahora))
    }

    @Test
    fun mensajeDe1DiaAtras_retorna6Dias() {
        val ahora = System.currentTimeMillis()
        assertEquals(6, calcularDiasRestantes(ahora - DAY_MS, ahora))
    }

    @Test
    fun mensajeDe3DiasAtras_retorna4Dias() {
        val ahora = System.currentTimeMillis()
        assertEquals(4, calcularDiasRestantes(ahora - 3 * DAY_MS, ahora))
    }

    @Test
    fun mensajeDe6DiasAtras_retorna1Dia() {
        val ahora = System.currentTimeMillis()
        assertEquals(1, calcularDiasRestantes(ahora - 6 * DAY_MS, ahora))
    }

    // ── Borde: menos de 24 h restantes (diasRestantes == 0 → "Expira hoy") ──

    @Test
    fun mensajeDe6DiasY12HorasAtras_retorna0DiasExpiraHoy() {
        val ahora = System.currentTimeMillis()
        val inicio = ahora - (6 * DAY_MS + 12 * 60 * 60 * 1000L)
        assertEquals(0, calcularDiasRestantes(inicio, ahora))
    }

    @Test
    fun mensajeDe6DiasY23HorasAtras_retorna0DiasExpiraHoy() {
        val ahora = System.currentTimeMillis()
        val inicio = ahora - (6 * DAY_MS + 23 * 60 * 60 * 1000L)
        assertEquals(0, calcularDiasRestantes(inicio, ahora))
    }

    // ── Expiración exacta y pasada ───────────────────────────────────────────

    @Test
    fun mensajeDeExactamente7Dias_retornaExpirado() {
        val ahora = System.currentTimeMillis()
        assertEquals(-1, calcularDiasRestantes(ahora - 7 * DAY_MS, ahora))
    }

    @Test
    fun mensajeDe8DiasAtras_retornaExpirado() {
        val ahora = System.currentTimeMillis()
        assertEquals(-1, calcularDiasRestantes(ahora - 8 * DAY_MS, ahora))
    }

    @Test
    fun mensajeDe30DiasAtras_retornaExpirado() {
        val ahora = System.currentTimeMillis()
        assertEquals(-1, calcularDiasRestantes(ahora - 30 * DAY_MS, ahora))
    }

    // ── Constante: la ventana de 7 días son exactamente 604 800 000 ms ───────

    @Test
    fun ventanaExpiracion_es7DiasEnMs() {
        assertEquals(604_800_000L, 7L * DAY_MS)
    }
}
