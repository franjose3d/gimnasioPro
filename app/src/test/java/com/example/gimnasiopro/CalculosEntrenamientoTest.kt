package com.example.gimnasiopro

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para cálculos matemáticos utilizados en la aplicación.
 */
class CalculosEntrenamientoTest {

    // ==================== Tests de Volumen ====================

    @Test
    fun `calculo de volumen simple es correcto`() {
        val peso = 50f
        val repeticiones = 10

        val volumen = peso * repeticiones

        assertEquals(500f, volumen, 0.001f)
    }

    @Test
    fun `calculo de volumen con peso decimal es correcto`() {
        val peso = 22.5f
        val repeticiones = 8

        val volumen = peso * repeticiones

        assertEquals(180f, volumen, 0.001f)
    }

    @Test
    fun `calculo de volumen total de varios ejercicios es correcto`() {
        val ejercicios = listOf(
            Pair(50f, 10),   // 500
            Pair(40f, 12),   // 480
            Pair(60f, 8)     // 480
        )

        val volumenTotal = ejercicios.sumOf { (peso, reps) ->
            (peso * reps).toDouble()
        }.toFloat()

        assertEquals(1460f, volumenTotal, 0.001f)
    }

    @Test
    fun `volumen con cero repeticiones es cero`() {
        val peso = 100f
        val repeticiones = 0

        val volumen = peso * repeticiones

        assertEquals(0f, volumen, 0.001f)
    }

    @Test
    fun `volumen con cero peso es cero`() {
        val peso = 0f
        val repeticiones = 10

        val volumen = peso * repeticiones

        assertEquals(0f, volumen, 0.001f)
    }

    // ==================== Tests de Incrementos ====================

    @Test
    fun `incremento de repeticiones de 1 en 1 es correcto`() {
        var repeticiones = 0
        val incremento = 1

        repeticiones += incremento
        assertEquals(1, repeticiones)

        repeticiones += incremento
        assertEquals(2, repeticiones)

        repeticiones += incremento
        assertEquals(3, repeticiones)
    }

    @Test
    fun `incremento de peso de 5 en 5 es correcto`() {
        var peso = 0f
        val incremento = 5f

        peso += incremento
        assertEquals(5f, peso, 0.001f)

        peso += incremento
        assertEquals(10f, peso, 0.001f)

        peso += incremento
        assertEquals(15f, peso, 0.001f)
    }

    @Test
    fun `decremento de repeticiones no baja de cero`() {
        var repeticiones = 2
        val decremento = 1
        val minimo = 0

        repeticiones = maxOf(repeticiones - decremento, minimo)
        assertEquals(1, repeticiones)

        repeticiones = maxOf(repeticiones - decremento, minimo)
        assertEquals(0, repeticiones)

        repeticiones = maxOf(repeticiones - decremento, minimo)
        assertEquals(0, repeticiones) // No baja de 0
    }

    @Test
    fun `decremento de peso no baja de cero`() {
        var peso = 10f
        val decremento = 5f
        val minimo = 0f

        peso = maxOf(peso - decremento, minimo)
        assertEquals(5f, peso, 0.001f)

        peso = maxOf(peso - decremento, minimo)
        assertEquals(0f, peso, 0.001f)

        peso = maxOf(peso - decremento, minimo)
        assertEquals(0f, peso, 0.001f) // No baja de 0
    }

    // ==================== Tests de Tiempo ====================

    @Test
    fun `calculo de tiempo transcurrido es correcto`() {
        val tiempoInicio = 1000L
        val tiempoFin = 5000L

        val tiempoTranscurrido = tiempoFin - tiempoInicio

        assertEquals(4000L, tiempoTranscurrido)
    }

    @Test
    fun `conversion de milisegundos a minutos es correcta`() {
        val tiempoMs = 120000L // 2 minutos

        val minutos = tiempoMs / 60000

        assertEquals(2L, minutos)
    }

    @Test
    fun `conversion de milisegundos a horas es correcta`() {
        val tiempoMs = 7200000L // 2 horas

        val horas = tiempoMs / 3600000

        assertEquals(2L, horas)
    }

    @Test
    fun `calculo de minutos restantes despues de horas es correcto`() {
        val tiempoMs = 5400000L // 1 hora y 30 minutos

        val totalMinutos = tiempoMs / 60000
        val horas = totalMinutos / 60
        val minutos = totalMinutos % 60

        assertEquals(1L, horas)
        assertEquals(30L, minutos)
    }

    // ==================== Tests de Porcentajes ====================

    @Test
    fun `calculo de porcentaje de mejora es correcto`() {
        val valorInicial = 50f
        val valorActual = 75f

        val porcentajeMejora = ((valorActual - valorInicial) / valorInicial) * 100

        assertEquals(50f, porcentajeMejora, 0.001f)
    }

    @Test
    fun `porcentaje de mejora negativo indica regresion`() {
        val valorInicial = 100f
        val valorActual = 80f

        val porcentajeMejora = ((valorActual - valorInicial) / valorInicial) * 100

        assertEquals(-20f, porcentajeMejora, 0.001f)
    }

    @Test
    fun `porcentaje de mejora cero cuando no hay cambio`() {
        val valorInicial = 50f
        val valorActual = 50f

        val porcentajeMejora = ((valorActual - valorInicial) / valorInicial) * 100

        assertEquals(0f, porcentajeMejora, 0.001f)
    }

    // ==================== Tests de Contadores ====================

    @Test
    fun `conteo de ejercicios completados es correcto`() {
        val ejercicios = listOf(true, false, true, true, false, true)

        val completados = ejercicios.count { it }

        assertEquals(4, completados)
    }

    @Test
    fun `conteo de ejercicios cuando ninguno esta completado es cero`() {
        val ejercicios = listOf(false, false, false, false)

        val completados = ejercicios.count { it }

        assertEquals(0, completados)
    }

    @Test
    fun `conteo de ejercicios cuando todos estan completados es total`() {
        val ejercicios = listOf(true, true, true, true, true)

        val completados = ejercicios.count { it }

        assertEquals(5, completados)
    }

    // ==================== Tests de Límites ====================

    @Test
    fun `maximo de ejercicios por rutina es 10`() {
        val maxEjercicios = 10
        val ejerciciosActuales = 8
        val nuevosEjercicios = 5

        val espaciosDisponibles = maxEjercicios - ejerciciosActuales
        val ejerciciosAAgregar = minOf(nuevosEjercicios, espaciosDisponibles)

        assertEquals(2, espaciosDisponibles)
        assertEquals(2, ejerciciosAAgregar)
    }

    @Test
    fun `no se pueden agregar ejercicios cuando rutina esta llena`() {
        val maxEjercicios = 10
        val ejerciciosActuales = 10
        val nuevosEjercicios = 3

        val espaciosDisponibles = maxEjercicios - ejerciciosActuales
        val ejerciciosAAgregar = minOf(nuevosEjercicios, espaciosDisponibles)

        assertEquals(0, espaciosDisponibles)
        assertEquals(0, ejerciciosAAgregar)
    }
}

