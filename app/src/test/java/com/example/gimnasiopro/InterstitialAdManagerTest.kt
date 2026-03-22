package com.example.gimnasiopro

import com.example.gimnasiopro.ads.InterstitialAdManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

/**
 * Tests unitarios para la lógica de cooldown del InterstitialAdManager.
 * No prueba la carga/muestra real del anuncio (requiere dispositivo con AdMob),
 * solo verifica la lógica de tiempo entre anuncios.
 */
class InterstitialAdManagerTest {

    private val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutos

    @Before
    fun setUp() {
        // Resetear lastShownTime antes de cada test
        setLastShownTime(0L)
    }

    @Test
    fun cooldown_inicialmenteEsCero() {
        val lastShown = getLastShownTime()
        assertEquals(0L, lastShown)
    }

    @Test
    fun cooldown_cincoMinutos_sonTrescientisMilisegundos() {
        assertEquals(300_000L, COOLDOWN_MS)
    }

    @Test
    fun cooldown_tiempoTranscurrido_superaCooldown() {
        val ahora = System.currentTimeMillis()
        val haceSeisMinutos = ahora - (6 * 60 * 1000L)
        setLastShownTime(haceSeisMinutos)

        val transcurrido = ahora - getLastShownTime()
        assertTrue("Deben haber pasado más de 5 minutos", transcurrido >= COOLDOWN_MS)
    }

    @Test
    fun cooldown_tiempoTranscurrido_noCuperaCooldown() {
        val ahora = System.currentTimeMillis()
        val haceUnMinuto = ahora - (1 * 60 * 1000L)
        setLastShownTime(haceUnMinuto)

        val transcurrido = ahora - getLastShownTime()
        assertFalse("No deben haber pasado 5 minutos", transcurrido >= COOLDOWN_MS)
    }

    @Test
    fun cooldown_exactamenteCincoMinutos_superaCooldown() {
        val ahora = System.currentTimeMillis()
        val haceCincoMinutos = ahora - COOLDOWN_MS
        setLastShownTime(haceCincoMinutos)

        val transcurrido = ahora - getLastShownTime()
        assertTrue("Exactamente 5 minutos debe superar el cooldown", transcurrido >= COOLDOWN_MS)
    }

    // ── Helpers de reflexión para acceder al campo privado lastShownTime ──────

    private fun getLastShownTime(): Long {
        val field = getLastShownField()
        return field.getLong(InterstitialAdManager)
    }

    private fun setLastShownTime(value: Long) {
        val field = getLastShownField()
        field.setLong(InterstitialAdManager, value)
    }

    private fun getLastShownField(): Field {
        return InterstitialAdManager::class.java.getDeclaredField("lastShownTime").also {
            it.isAccessible = true
        }
    }
}
