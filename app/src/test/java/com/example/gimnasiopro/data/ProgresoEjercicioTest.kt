package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la clase ProgresoEjercicio.
 */
class ProgresoEjercicioTest {

    @Test
    fun `progreso se crea con valores correctos`() {
        val progreso = ProgresoEjercicio(
            ejercicioId = 1L,
            pesoInicial = 40f,
            pesoActual = 60f,
            repeticionesIniciales = 8,
            repeticionesActuales = 12,
            porcentajeMejora = 50f
        )

        assertEquals(1L, progreso.ejercicioId)
        assertEquals(40f, progreso.pesoInicial, 0.001f)
        assertEquals(60f, progreso.pesoActual, 0.001f)
        assertEquals(8, progreso.repeticionesIniciales)
        assertEquals(12, progreso.repeticionesActuales)
        assertEquals(50f, progreso.porcentajeMejora, 0.001f)
    }

    @Test
    fun `calculo de mejora de peso es correcto`() {
        val pesoInicial = 50f
        val pesoActual = 75f

        val mejora = ((pesoActual - pesoInicial) / pesoInicial) * 100

        assertEquals(50f, mejora, 0.001f)
    }

    @Test
    fun `calculo de mejora de repeticiones es correcto`() {
        val repIniciales = 8
        val repActuales = 12

        val mejora = ((repActuales - repIniciales).toFloat() / repIniciales) * 100

        assertEquals(50f, mejora, 0.001f)
    }

    @Test
    fun `progreso sin mejora tiene porcentaje cero`() {
        val progreso = ProgresoEjercicio(
            ejercicioId = 1L,
            pesoInicial = 50f,
            pesoActual = 50f,
            repeticionesIniciales = 10,
            repeticionesActuales = 10,
            porcentajeMejora = 0f
        )

        assertEquals(0f, progreso.porcentajeMejora, 0.001f)
    }

    @Test
    fun `calculo de diferencia de peso es correcto`() {
        val pesoInicial = 40f
        val pesoActual = 55f

        val diferencia = pesoActual - pesoInicial

        assertEquals(15f, diferencia, 0.001f)
    }

    @Test
    fun `calculo de diferencia de repeticiones es correcto`() {
        val repIniciales = 6
        val repActuales = 10

        val diferencia = repActuales - repIniciales

        assertEquals(4, diferencia)
    }
}

