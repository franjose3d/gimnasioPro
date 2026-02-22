package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la entidad EjercicioEntrenamiento.
 */
class EjercicioEntrenamientoTest {

    private fun crearEjercicioBase() = Ejercicio(
        id = 1,
        grupoMuscular = "Pecho",
        nombre = "Press de Banca",
        descripcion = "Ejercicio de pecho"
    )

    @Test
    fun `ejercicioEntrenamiento se crea con valores por defecto`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        // Por defecto tiene 3 series
        assertEquals(3, entrenamiento.series.size)
        assertEquals(0f, entrenamiento.pesoKg, 0.001f)
        assertFalse(entrenamiento.completado)
    }

    @Test
    fun `ejercicioEntrenamiento se crea con series personalizadas`() {
        val ejercicio = crearEjercicioBase()
        val seriesPersonalizadas = mutableListOf(
            SerieEntrenamiento(pesoKg = 50f),
            SerieEntrenamiento(pesoKg = 55f),
            SerieEntrenamiento(pesoKg = 60f)
        )
        val entrenamiento = EjercicioEntrenamiento(
            ejercicio = ejercicio,
            series = seriesPersonalizadas,
            completado = true
        )

        assertEquals(3, entrenamiento.series.size)
        assertEquals(50f, entrenamiento.series[0].pesoKg, 0.001f)
        assertEquals(55f, entrenamiento.series[1].pesoKg, 0.001f)
        assertEquals(60f, entrenamiento.series[2].pesoKg, 0.001f)
        assertTrue(entrenamiento.completado)
    }

    @Test
    fun `ejercicioEntrenamiento permite modificar peso de serie`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        entrenamiento.series[0].pesoKg = 75.5f

        assertEquals(75.5f, entrenamiento.series[0].pesoKg, 0.001f)
    }

    @Test
    fun `ejercicioEntrenamiento pesoKg devuelve primera serie`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        entrenamiento.series[0].pesoKg = 100f

        assertEquals(100f, entrenamiento.pesoKg, 0.001f)
    }

    @Test
    fun `ejercicioEntrenamiento permite modificar completado`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        assertFalse(entrenamiento.completado)
        entrenamiento.completado = true
        assertTrue(entrenamiento.completado)
    }

    @Test
    fun `calculo de volumen total es correcto`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(
            ejercicio = ejercicio,
            series = mutableListOf(
                SerieEntrenamiento(pesoKg = 50f),
                SerieEntrenamiento(pesoKg = 50f),
                SerieEntrenamiento(pesoKg = 50f)
            )
        )

        val volumen = entrenamiento.calcularVolumenTotal()

        // Volumen = peso × repeticiones(10 por defecto) × 3 series = 50*10 + 50*10 + 50*10 = 1500
        assertEquals(1500f, volumen, 0.001f)
    }

    @Test
    fun `calculo de volumen con peso decimal`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(
            ejercicio = ejercicio,
            series = mutableListOf(
                SerieEntrenamiento(pesoKg = 22.5f),
                SerieEntrenamiento(pesoKg = 25f),
                SerieEntrenamiento(pesoKg = 27.5f)
            )
        )

        val volumen = entrenamiento.calcularVolumenTotal()

        // Volumen = (22.5*10) + (25*10) + (27.5*10) = 225 + 250 + 275 = 750
        assertEquals(750f, volumen, 0.001f)
    }

    @Test
    fun `series visibles se limita entre 1 y 6`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        entrenamiento.seriesVisibles = 0
        assertEquals(1, entrenamiento.seriesVisibles)

        entrenamiento.seriesVisibles = 10
        assertEquals(6, entrenamiento.seriesVisibles)

        entrenamiento.seriesVisibles = 5
        assertEquals(5, entrenamiento.seriesVisibles)
    }

    @Test
    fun `aumentar series visibles añade series`() {
        val ejercicio = crearEjercicioBase()
        val entrenamiento = EjercicioEntrenamiento(ejercicio = ejercicio)

        assertEquals(3, entrenamiento.series.size)

        entrenamiento.seriesVisibles = 5

        assertTrue(entrenamiento.series.size >= 5)
    }
}

