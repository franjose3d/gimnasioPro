package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la función de formateo de tiempo.
 */
class EstadisticaRepositoryTest {

    @Test
    fun `formatearTiempo con 0 milisegundos devuelve 0h 0min`() {
        val result = EstadisticaRepository.formatearTiempo(0)
        assertEquals("0h 0min", result)
    }

    @Test
    fun `formatearTiempo con 1 minuto devuelve 0h 1min`() {
        val unMinutoMs = 60000L
        val result = EstadisticaRepository.formatearTiempo(unMinutoMs)
        assertEquals("0h 1min", result)
    }

    @Test
    fun `formatearTiempo con 30 minutos devuelve 0h 30min`() {
        val treintaMinutosMs = 30 * 60000L
        val result = EstadisticaRepository.formatearTiempo(treintaMinutosMs)
        assertEquals("0h 30min", result)
    }

    @Test
    fun `formatearTiempo con 1 hora devuelve 1h 0min`() {
        val unaHoraMs = 60 * 60000L
        val result = EstadisticaRepository.formatearTiempo(unaHoraMs)
        assertEquals("1h 0min", result)
    }

    @Test
    fun `formatearTiempo con 1 hora y 30 minutos devuelve 1h 30min`() {
        val tiempoMs = 90 * 60000L
        val result = EstadisticaRepository.formatearTiempo(tiempoMs)
        assertEquals("1h 30min", result)
    }

    @Test
    fun `formatearTiempo con 2 horas y 45 minutos devuelve 2h 45min`() {
        val tiempoMs = 165 * 60000L
        val result = EstadisticaRepository.formatearTiempo(tiempoMs)
        assertEquals("2h 45min", result)
    }

    @Test
    fun `formatearTiempo con 10 horas devuelve 10h 0min`() {
        val tiempoMs = 600 * 60000L
        val result = EstadisticaRepository.formatearTiempo(tiempoMs)
        assertEquals("10h 0min", result)
    }

    @Test
    fun `formatearTiempo ignora milisegundos extra`() {
        // 1 minuto + 30 segundos = debería dar 1 minuto (trunca)
        val tiempoMs = 90000L // 1.5 minutos
        val result = EstadisticaRepository.formatearTiempo(tiempoMs)
        assertEquals("0h 1min", result)
    }

    @Test
    fun `formatearTiempo con tiempo grande funciona correctamente`() {
        // 25 horas y 59 minutos
        val tiempoMs = (25 * 60 + 59) * 60000L
        val result = EstadisticaRepository.formatearTiempo(tiempoMs)
        assertEquals("25h 59min", result)
    }
}

