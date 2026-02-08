package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la entidad Ejercicio.
 */
class EjercicioTest {

    @Test
    fun `ejercicio se crea con valores correctos`() {
        val ejercicio = Ejercicio(
            id = 1,
            grupoMuscular = "Pecho",
            nombre = "Press de Banca",
            descripcion = "Ejercicio para pectorales"
        )

        assertEquals(1L, ejercicio.id)
        assertEquals("Pecho", ejercicio.grupoMuscular)
        assertEquals("Press de Banca", ejercicio.nombre)
        assertEquals("Ejercicio para pectorales", ejercicio.descripcion)
    }

    @Test
    fun `ejercicio sin imagenUrl tiene valor null`() {
        val ejercicio = Ejercicio(
            id = 1,
            grupoMuscular = "Espalda",
            nombre = "Dominadas",
            descripcion = "Ejercicio de tracción"
        )

        assertNull(ejercicio.imagenUrl)
    }

    @Test
    fun `ejercicio con imagenUrl guarda valor correcto`() {
        val ejercicio = Ejercicio(
            id = 1,
            grupoMuscular = "Biceps",
            nombre = "Curl",
            descripcion = "Ejercicio de bíceps",
            imagenUrl = "https://example.com/curl.jpg"
        )

        assertEquals("https://example.com/curl.jpg", ejercicio.imagenUrl)
    }

    @Test
    fun `ejercicio id por defecto es 0`() {
        val ejercicio = Ejercicio(
            grupoMuscular = "Piernas",
            nombre = "Sentadilla",
            descripcion = "Ejercicio de piernas"
        )

        assertEquals(0L, ejercicio.id)
    }

    @Test
    fun `dos ejercicios con mismos datos son iguales`() {
        val ejercicio1 = Ejercicio(
            id = 1,
            grupoMuscular = "Hombros",
            nombre = "Press Militar",
            descripcion = "Ejercicio de hombros"
        )
        val ejercicio2 = Ejercicio(
            id = 1,
            grupoMuscular = "Hombros",
            nombre = "Press Militar",
            descripcion = "Ejercicio de hombros"
        )

        assertEquals(ejercicio1, ejercicio2)
    }

    @Test
    fun `dos ejercicios con diferentes ids son diferentes`() {
        val ejercicio1 = Ejercicio(
            id = 1,
            grupoMuscular = "Triceps",
            nombre = "Fondos",
            descripcion = "Ejercicio de tríceps"
        )
        val ejercicio2 = Ejercicio(
            id = 2,
            grupoMuscular = "Triceps",
            nombre = "Fondos",
            descripcion = "Ejercicio de tríceps"
        )

        assertNotEquals(ejercicio1, ejercicio2)
    }
}

