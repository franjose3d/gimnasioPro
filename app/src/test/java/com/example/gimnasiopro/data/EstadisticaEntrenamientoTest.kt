package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la entidad EstadisticaEntrenamiento.
 */
class EstadisticaEntrenamientoTest {

    @Test
    fun `estadistica se crea con valores correctos`() {
        val estadistica = EstadisticaEntrenamiento(
            id = 1,
            fecha = 1234567890L,
            anio = 2026,
            mes = 2,
            dia = 1,
            tiempoEntrenamientoMs = 3600000L, // 1 hora
            numeroEntrenamientos = 2,
            ejerciciosCompletados = 8,
            volumenTotal = 5000f,
            rutinasUsadas = "1,3"
        )

        assertEquals(1L, estadistica.id)
        assertEquals(1234567890L, estadistica.fecha)
        assertEquals(2026, estadistica.anio)
        assertEquals(2, estadistica.mes)
        assertEquals(1, estadistica.dia)
        assertEquals(3600000L, estadistica.tiempoEntrenamientoMs)
        assertEquals(2, estadistica.numeroEntrenamientos)
        assertEquals(8, estadistica.ejerciciosCompletados)
        assertEquals(5000f, estadistica.volumenTotal, 0.001f)
        assertEquals("1,3", estadistica.rutinasUsadas)
    }

    @Test
    fun `estadistica valores por defecto son correctos`() {
        val estadistica = EstadisticaEntrenamiento(
            fecha = 0L,
            anio = 2026,
            mes = 1,
            dia = 1,
            tiempoEntrenamientoMs = 0L
        )

        assertEquals(0L, estadistica.id)
        assertEquals(1, estadistica.numeroEntrenamientos)
        assertEquals(0, estadistica.ejerciciosCompletados)
        assertEquals(0f, estadistica.volumenTotal, 0.001f)
        assertEquals("", estadistica.rutinasUsadas)
    }

    @Test
    fun `estadistica copy actualiza tiempo correctamente`() {
        val original = EstadisticaEntrenamiento(
            fecha = 0L,
            anio = 2026,
            mes = 1,
            dia = 1,
            tiempoEntrenamientoMs = 1800000L // 30 min
        )

        val actualizada = original.copy(
            tiempoEntrenamientoMs = original.tiempoEntrenamientoMs + 1800000L
        )

        assertEquals(3600000L, actualizada.tiempoEntrenamientoMs) // 1 hora
    }

    @Test
    fun `estadistica copy incrementa entrenamientos correctamente`() {
        val original = EstadisticaEntrenamiento(
            fecha = 0L,
            anio = 2026,
            mes = 1,
            dia = 1,
            tiempoEntrenamientoMs = 0L,
            numeroEntrenamientos = 1
        )

        val actualizada = original.copy(
            numeroEntrenamientos = original.numeroEntrenamientos + 1
        )

        assertEquals(2, actualizada.numeroEntrenamientos)
    }

    @Test
    fun `estadistica copy acumula volumen correctamente`() {
        val original = EstadisticaEntrenamiento(
            fecha = 0L,
            anio = 2026,
            mes = 1,
            dia = 1,
            tiempoEntrenamientoMs = 0L,
            volumenTotal = 2500f
        )

        val actualizada = original.copy(
            volumenTotal = original.volumenTotal + 1500f
        )

        assertEquals(4000f, actualizada.volumenTotal, 0.001f)
    }
}

