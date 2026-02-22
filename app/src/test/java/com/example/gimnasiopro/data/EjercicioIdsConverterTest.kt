package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para el convertidor de IDs de ejercicios.
 */
class EjercicioIdsConverterTest {

    private val converter = EjercicioIdsConverter()

    @Test
    fun `fromList convierte lista vacia a string vacio`() {
        val result = converter.fromList(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `fromList convierte lista con un elemento correctamente`() {
        val result = converter.fromList(listOf(5))
        assertEquals("5", result)
    }

    @Test
    fun `fromList convierte lista con multiples elementos correctamente`() {
        val result = converter.fromList(listOf(1, 2, 3, 4, 5))
        assertEquals("1,2,3,4,5", result)
    }

    @Test
    fun `toList convierte string vacio a lista vacia`() {
        val result = converter.toList("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toList convierte string con un numero a lista con un elemento`() {
        val result = converter.toList("5")
        assertEquals(listOf(5), result)
    }

    @Test
    fun `toList convierte string con multiples numeros correctamente`() {
        val result = converter.toList("1,2,3,4,5")
        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun `toList ignora valores no numericos`() {
        val result = converter.toList("1,abc,3,def,5")
        assertEquals(listOf(1, 3, 5), result)
    }

    @Test
    fun `conversion ida y vuelta mantiene los datos`() {
        val original = listOf(10, 20, 30, 40, 50)
        val asString = converter.fromList(original)
        val backToList = converter.toList(asString)
        assertEquals(original, backToList)
    }
}

