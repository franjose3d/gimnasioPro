package com.example.gimnasiopro.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la entidad RegistroEntrenamiento.
 */
class RegistroEntrenamientoTest {

    @Test
    fun `registro se crea con valores correctos`() {
        val registro = RegistroEntrenamiento(
            id = 1,
            rutinaId = 3,
            ejercicioId = 10,
            repeticiones = 12,
            pesoKg = 50f,
            completado = true,
            fechaEntrenamiento = 1234567890L
        )

        assertEquals(1L, registro.id)
        assertEquals(3, registro.rutinaId)
        assertEquals(10L, registro.ejercicioId)
        assertEquals(12, registro.repeticiones)
        assertEquals(50f, registro.pesoKg, 0.001f)
        assertTrue(registro.completado)
        assertEquals(1234567890L, registro.fechaEntrenamiento)
    }

    @Test
    fun `registro id por defecto es 0`() {
        val registro = RegistroEntrenamiento(
            rutinaId = 1,
            ejercicioId = 1,
            repeticiones = 10,
            pesoKg = 20f
        )

        assertEquals(0L, registro.id)
    }

    @Test
    fun `registro completado por defecto es false`() {
        val registro = RegistroEntrenamiento(
            rutinaId = 1,
            ejercicioId = 1,
            repeticiones = 10,
            pesoKg = 20f
        )

        assertFalse(registro.completado)
    }

    @Test
    fun `registro fechaEntrenamiento tiene valor por defecto`() {
        val antes = System.currentTimeMillis()
        val registro = RegistroEntrenamiento(
            rutinaId = 1,
            ejercicioId = 1,
            repeticiones = 10,
            pesoKg = 20f
        )
        val despues = System.currentTimeMillis()

        assertTrue(registro.fechaEntrenamiento >= antes)
        assertTrue(registro.fechaEntrenamiento <= despues)
    }

    @Test
    fun `calculo de volumen del registro es correcto`() {
        val registro = RegistroEntrenamiento(
            rutinaId = 1,
            ejercicioId = 1,
            repeticiones = 10,
            pesoKg = 60f
        )

        val volumen = registro.pesoKg * registro.repeticiones

        assertEquals(600f, volumen, 0.001f)
    }
}

