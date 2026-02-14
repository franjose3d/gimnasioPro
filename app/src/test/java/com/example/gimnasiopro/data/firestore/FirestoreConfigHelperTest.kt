package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

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

    private class FakeServicePermissionDenied : FirestoreService {
        override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
            throw FirebaseFirestoreException("Permission denied", Code.PERMISSION_DENIED)
        }

        override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
            throw FirebaseFirestoreException("Permission denied", Code.PERMISSION_DENIED)
        }
    }

    private class FakeServiceTimeout : FirestoreService {
        override suspend fun getCollectionSize(collectionPath: String, timeoutMs: Long): Long {
            throw CancellationException("Timed out")
        }

        override suspend fun getSubcollectionSize(documentPath: String, subcollectionName: String, timeoutMs: Long): Long {
            throw CancellationException("Timed out")
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
    fun `verificarConfiguracion_permissionDenied_returns_reglasFalse`() = runTest {
        val helper = FirestoreConfigHelper(FakeServicePermissionDenied())
        val res = helper.verificarConfiguracion()

        assertFalse(res.conexionOk)
        assertFalse(res.reglasOk)
        assertEquals(0L, res.ejerciciosCount)
        assertTrue(res.mensaje.contains("Reglas") || res.mensaje.contains("security") || res.mensaje.contains("Error"))
    }

    @Test
    fun `verificarConfiguracion_timeout_returns_timeout_message`() = runTest {
        val helper = FirestoreConfigHelper(FakeServiceTimeout())
        val res = helper.verificarConfiguracion()

        assertFalse(res.conexionOk)
        assertEquals(0L, res.ejerciciosCount)
        assertTrue(res.mensaje.contains("tiempo") || res.mensaje.contains("timeout") || res.mensaje.contains("Operación"))
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
