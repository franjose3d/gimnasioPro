package com.example.gimnasiopro.utils

import android.view.View
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para BadgeHelper.
 *
 * Verifica la lógica del badge de notificaciones:
 * - Texto correcto según cantidad
 * - Visibilidad correcta según cantidad
 */
class BadgeHelperTest {

    @Test
    fun `obtenerTextoBadge devuelve numero cuando count es 1`() {
        // Arrange
        val count = 1

        // Act
        val resultado = BadgeHelper.obtenerTextoBadge(count)

        // Assert
        assertEquals("1", resultado)
    }

    @Test
    fun `obtenerTextoBadge devuelve numero cuando count es 5`() {
        val count = 5
        val resultado = BadgeHelper.obtenerTextoBadge(count)
        assertEquals("5", resultado)
    }

    @Test
    fun `obtenerTextoBadge devuelve 9 cuando count es 9`() {
        val count = 9
        val resultado = BadgeHelper.obtenerTextoBadge(count)
        assertEquals("9", resultado)
    }

    @Test
    fun `obtenerTextoBadge devuelve 9+ cuando count es 10`() {
        val count = 10
        val resultado = BadgeHelper.obtenerTextoBadge(count)
        assertEquals("9+", resultado)
    }

    @Test
    fun `obtenerTextoBadge devuelve 9+ cuando count es 50`() {
        val count = 50
        val resultado = BadgeHelper.obtenerTextoBadge(count)
        assertEquals("9+", resultado)
    }

    @Test
    fun `debeEstarVisible retorna false cuando count es 0`() {
        val count = 0
        val resultado = BadgeHelper.debeEstarVisible(count)
        assertFalse(resultado)
    }

    @Test
    fun `debeEstarVisible retorna true cuando count es 1`() {
        val count = 1
        val resultado = BadgeHelper.debeEstarVisible(count)
        assertTrue(resultado)
    }

    @Test
    fun `debeEstarVisible retorna true cuando count es mayor a 0`() {
        val count = 5
        val resultado = BadgeHelper.debeEstarVisible(count)
        assertTrue(resultado)
    }

    @Test
    fun `obtenerVisibilidad retorna GONE cuando count es 0`() {
        val count = 0
        val resultado = BadgeHelper.obtenerVisibilidad(count)
        assertEquals(View.GONE, resultado)
    }

    @Test
    fun `obtenerVisibilidad retorna VISIBLE cuando count es mayor a 0`() {
        val count = 3
        val resultado = BadgeHelper.obtenerVisibilidad(count)
        assertEquals(View.VISIBLE, resultado)
    }
}