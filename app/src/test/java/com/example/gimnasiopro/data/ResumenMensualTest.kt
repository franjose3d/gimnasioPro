package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la clase ResumenMensual.
 */
class ResumenMensualTest {

    @Test
    fun `resumen mensual se crea con valores correctos`() {
        val resumen = ResumenMensual(
            mes = 2,
            tiempoTotal = 7200000L, // 2 horas
            entrenamientos = 5,
            volumen = 15000f
        )

        assertEquals(2, resumen.mes)
        assertEquals(7200000L, resumen.tiempoTotal)
        assertEquals(5, resumen.entrenamientos)
        assertEquals(15000f, resumen.volumen, 0.001f)
    }

    @Test
    fun `promedio de tiempo por entrenamiento es correcto`() {
        val resumen = ResumenMensual(
            mes = 1,
            tiempoTotal = 3600000L, // 1 hora
            entrenamientos = 4,
            volumen = 10000f
        )

        val promedio = if (resumen.entrenamientos > 0) {
            resumen.tiempoTotal / resumen.entrenamientos
        } else 0L

        assertEquals(900000L, promedio) // 15 minutos por entrenamiento
    }

    @Test
    fun `promedio de volumen por entrenamiento es correcto`() {
        val resumen = ResumenMensual(
            mes = 1,
            tiempoTotal = 0L,
            entrenamientos = 5,
            volumen = 25000f
        )

        val promedio = if (resumen.entrenamientos > 0) {
            resumen.volumen / resumen.entrenamientos
        } else 0f

        assertEquals(5000f, promedio, 0.001f)
    }

    @Test
    fun `resumen con cero entrenamientos maneja division correctamente`() {
        val resumen = ResumenMensual(
            mes = 1,
            tiempoTotal = 0L,
            entrenamientos = 0,
            volumen = 0f
        )

        val promedioTiempo = if (resumen.entrenamientos > 0) {
            resumen.tiempoTotal / resumen.entrenamientos
        } else 0L

        val promedioVolumen = if (resumen.entrenamientos > 0) {
            resumen.volumen / resumen.entrenamientos
        } else 0f

        assertEquals(0L, promedioTiempo)
        assertEquals(0f, promedioVolumen, 0.001f)
    }

    @Test
    fun `conversion de tiempo total a horas es correcta`() {
        val resumen = ResumenMensual(
            mes = 1,
            tiempoTotal = 10800000L, // 3 horas
            entrenamientos = 6,
            volumen = 0f
        )

        val horas = resumen.tiempoTotal / 3600000.0

        assertEquals(3.0, horas, 0.001)
    }

    @Test
    fun `mes del resumen esta en rango valido`() {
        for (mes in 1..12) {
            val resumen = ResumenMensual(
                mes = mes,
                tiempoTotal = 0L,
                entrenamientos = 0,
                volumen = 0f
            )

            assertTrue(resumen.mes in 1..12)
        }
    }
}

