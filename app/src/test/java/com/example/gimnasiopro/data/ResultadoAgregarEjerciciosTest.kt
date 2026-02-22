package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la clase ResultadoAgregarEjercicios.
 */
class ResultadoAgregarEjerciciosTest {

    @Test
    fun `resultado exitoso tiene valores correctos`() {
        val resultado = RutinaRepository.ResultadoAgregarEjercicios(
            exito = true,
            ejerciciosAgregados = 3,
            ejerciciosYaExistentes = 1,
            ejerciciosNoAgregados = 0,
            totalActual = 5,
            mensaje = "3 ejercicio(s) añadido(s). 1 ya estaba(n) en la rutina. Total en rutina: 5/10"
        )

        assertTrue(resultado.exito)
        assertEquals(3, resultado.ejerciciosAgregados)
        assertEquals(1, resultado.ejerciciosYaExistentes)
        assertEquals(0, resultado.ejerciciosNoAgregados)
        assertEquals(5, resultado.totalActual)
    }

    @Test
    fun `resultado fallido cuando rutina esta llena`() {
        val resultado = RutinaRepository.ResultadoAgregarEjercicios(
            exito = false,
            ejerciciosAgregados = 0,
            ejerciciosYaExistentes = 0,
            ejerciciosNoAgregados = 5,
            totalActual = 10,
            mensaje = "La rutina ya tiene 10 ejercicios. No se pueden añadir más."
        )

        assertFalse(resultado.exito)
        assertEquals(0, resultado.ejerciciosAgregados)
        assertEquals(5, resultado.ejerciciosNoAgregados)
        assertEquals(10, resultado.totalActual)
    }

    @Test
    fun `resultado con ejercicios parcialmente agregados`() {
        val resultado = RutinaRepository.ResultadoAgregarEjercicios(
            exito = true,
            ejerciciosAgregados = 2,
            ejerciciosYaExistentes = 0,
            ejerciciosNoAgregados = 3,
            totalActual = 10,
            mensaje = "2 ejercicio(s) añadido(s). 3 no se pudo(ieron) añadir (límite de 10). Total en rutina: 10/10"
        )

        assertTrue(resultado.exito)
        assertEquals(2, resultado.ejerciciosAgregados)
        assertEquals(3, resultado.ejerciciosNoAgregados)
        assertEquals(10, resultado.totalActual)
    }

    @Test
    fun `resultado solo con ejercicios ya existentes es exito`() {
        val resultado = RutinaRepository.ResultadoAgregarEjercicios(
            exito = true,
            ejerciciosAgregados = 0,
            ejerciciosYaExistentes = 3,
            ejerciciosNoAgregados = 0,
            totalActual = 5,
            mensaje = "3 ya estaba(n) en la rutina. Total en rutina: 5/10"
        )

        assertTrue(resultado.exito)
        assertEquals(0, resultado.ejerciciosAgregados)
        assertEquals(3, resultado.ejerciciosYaExistentes)
    }

    @Test
    fun `maximo de ejercicios por rutina es constante`() {
        assertEquals(10, RutinaRepository.MAX_EJERCICIOS_POR_RUTINA)
    }
}

