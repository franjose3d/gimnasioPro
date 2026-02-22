package com.example.gimnasiopro.data.firestore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreConfigHelperTest {

    private class FakeServiceSuccess : FirestoreService {
        override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
            return 5L
        }

        override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
            return when (subcollectionName) {
                "rutinas" -> 2L
                "calendario" -> 0L
                "entrenamientos" -> 1L
                "estadisticas" -> 0L
                else -> 0L
            }
        }
    }

    /**
     * Simula un error de conexión (IOException).
     */
    private class FakeServiceConnectionError : FirestoreService {
        override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
            throw IOException("Connection error")
        }

        override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
            throw IOException("Connection error")
        }
    }

    /**
     * Simula un error genérico inesperado.
     */
    private class FakeServiceGenericError : FirestoreService {
        override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
            throw RuntimeException("Unexpected error")
        }

        override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
            throw RuntimeException("Unexpected error")
        }
    }

    @Test
    fun `verificarConfiguracion_success_returns_ok`() = runTest {
        val helper = FirestoreConfigHelper(FakeServiceSuccess())
        val res = helper.verificarConfiguracion()

        assertTrue(res.conexionOk)
        assertTrue(res.reglasOk)
        assertEquals(5L, res.ejerciciosCount)
    }

    @Test
    fun `verificarConfiguracion_connectionError_returns_connectionFalse`() = runTest {
        val helper = FirestoreConfigHelper(FakeServiceConnectionError())
        val res = helper.verificarConfiguracion()

        assertFalse(res.conexionOk)
        assertEquals(0L, res.ejerciciosCount)
        assertTrue(res.mensaje.contains("conexión") || res.mensaje.contains("Error"))
    }


    @Test
    fun `verificarConfiguracion_genericError_returns_error`() = runTest {
        val helper = FirestoreConfigHelper(FakeServiceGenericError())
        val res = helper.verificarConfiguracion()

        assertFalse(res.conexionOk)
        assertEquals(0L, res.ejerciciosCount)
        assertTrue(res.mensaje.contains("Error") || res.mensaje.contains("inesperado"))
    }

    @Test
    fun `verificarEstructuraUsuario_partial_structure_reflected_in_result`() = runTest {
        val helper = FirestoreConfigHelper(FakeServiceSuccess())
        val res = helper.verificarEstructuraUsuario("user123")

        assertTrue(res.rutinasOk)
        assertFalse(res.calendarioOk)
        assertTrue(res.entrenamientosOk)
        assertFalse(res.estadisticasOk)
    }
}
