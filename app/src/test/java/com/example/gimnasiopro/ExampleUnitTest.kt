package com.example.gimnasiopro

import org.junit.Test

import org.junit.Assert.*

/**
 * Tests unitarios básicos de sanidad para verificar la configuración.
 */
class ExampleUnitTest {

    @Test
    fun `configuracion de tests funciona correctamente`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `constantes de la app estan definidas`() {
        // Verificar que las constantes importantes están definidas
        assertTrue(com.example.gimnasiopro.data.RutinaRepository.MAX_EJERCICIOS_POR_RUTINA > 0)
    }
}