package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la entidad Rutina.
 */
class RutinaTest {

    @Test
    fun `getEjercicioIdsList convierte IDs Int a Long correctamente`() {
        val rutina = Rutina(
            numeroRutina = 1,
            nombre = "Rutina Test",
            ejercicioIds = listOf(1, 2, 3, 4, 5)
        )

        val result = rutina.getEjercicioIdsList()

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result)
    }

    @Test
    fun `getEjercicioIdsList con lista vacia devuelve lista vacia`() {
        val rutina = Rutina(
            numeroRutina = 1,
            nombre = "Rutina Vacia",
            ejercicioIds = emptyList()
        )

        val result = rutina.getEjercicioIdsList()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `rutina por defecto tiene lista vacia de ejercicios`() {
        val rutina = Rutina(
            numeroRutina = 1,
            nombre = "Rutina Default"
        )

        assertTrue(rutina.ejercicioIds.isEmpty())
        assertTrue(rutina.getEjercicioIdsList().isEmpty())
    }

    @Test
    fun `rutina guarda numero correcto`() {
        val rutina = Rutina(
            numeroRutina = 5,
            nombre = "Rutina 5"
        )

        assertEquals(5, rutina.numeroRutina)
    }

    @Test
    fun `rutina guarda nombre correcto`() {
        val rutina = Rutina(
            numeroRutina = 1,
            nombre = "Mi Rutina Personalizada"
        )

        assertEquals("Mi Rutina Personalizada", rutina.nombre)
    }

    @Test
    fun `rutina tiene fechas de creacion y modificacion por defecto`() {
        val antes = System.currentTimeMillis()
        val rutina = Rutina(
            numeroRutina = 1,
            nombre = "Rutina Test"
        )
        val despues = System.currentTimeMillis()

        assertTrue(rutina.fechaCreacion >= antes)
        assertTrue(rutina.fechaCreacion <= despues)
        assertTrue(rutina.fechaModificacion >= antes)
        assertTrue(rutina.fechaModificacion <= despues)
    }
}

