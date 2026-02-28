package com.example.gimnasiopro.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gimnasiopro.data.local.MensajeDao
import com.example.gimnasiopro.data.local.MensajeLocal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests para MensajeDao.
 *
 * Usa base de datos Room en memoria para tests rápidos.
 * Verifica:
 * - Inserción de mensajes
 * - Contador de mensajes no leídos
 * - Marcar como leído
 * - Filtros correctos (solo mensajes recibidos)
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
    manifest = Config.NONE  // ← Ignora el AndroidManifest
)
class MensajeDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: GymDatabase
    private lateinit var mensajeDao: MensajeDao

    @Before
    fun setup() {
        // Crear base de datos en memoria (se borra después de cada test)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        mensajeDao = database.mensajeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertarMensaje guarda mensaje correctamente`() = runBlocking {
        // Arrange
        val mensaje = MensajeLocal(
            id = "msg1",
            conversacionId = "conv1",
            remitenteId = "user1",
            destinatarioId = "user2",
            texto = "Hola!",
            fechaEnvio = System.currentTimeMillis(),
            leido = false,
            entregado = true,
            esRemitente = false
        )

        // Act
        mensajeDao.insertarMensaje(mensaje)

        // Assert
        val mensajes = mensajeDao.getMensajesPorConversacion("conv1").first()
        assertEquals(1, mensajes.size)
        assertEquals("Hola!", mensajes[0].texto)
    }

    @Test
    fun `contarNoLeidos retorna 0 cuando no hay mensajes`() = runBlocking {
        // Act
        val count = mensajeDao.contarNoLeidos("conv1").first()

        // Assert
        assertEquals(0, count)
    }

    @Test
    fun `contarNoLeidos retorna cantidad correcta de mensajes no leidos`() = runBlocking {
        // Arrange
        val mensaje1 = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        val mensaje2 = crearMensaje("msg2", "conv1", leido = false, esRemitente = false)
        val mensaje3 = crearMensaje("msg3", "conv1", leido = true, esRemitente = false) // Leído

        mensajeDao.insertarMensaje(mensaje1)
        mensajeDao.insertarMensaje(mensaje2)
        mensajeDao.insertarMensaje(mensaje3)

        // Act
        val count = mensajeDao.contarNoLeidos("conv1").first()

        // Assert
        assertEquals(2, count)
    }

    @Test
    fun `contarNoLeidos solo cuenta mensajes recibidos no enviados`() = runBlocking {
        // Arrange
        val mensajeRecibido = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        val mensajeEnviado = crearMensaje("msg2", "conv1", leido = false, esRemitente = true)

        mensajeDao.insertarMensaje(mensajeRecibido)
        mensajeDao.insertarMensaje(mensajeEnviado)

        // Act
        val count = mensajeDao.contarNoLeidos("conv1").first()

        // Assert
        assertEquals(1, count) // Solo cuenta el recibido
    }

    @Test
    fun `contarTotalNoLeidos suma mensajes de todas las conversaciones`() = runBlocking {
        // Arrange
        val msg1 = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        val msg2 = crearMensaje("msg2", "conv2", leido = false, esRemitente = false)
        val msg3 = crearMensaje("msg3", "conv3", leido = false, esRemitente = false)

        mensajeDao.insertarMensaje(msg1)
        mensajeDao.insertarMensaje(msg2)
        mensajeDao.insertarMensaje(msg3)

        // Act
        val count = mensajeDao.contarTotalNoLeidos().first()

        // Assert
        assertEquals(3, count)
    }

    @Test
    fun `marcarComoLeido actualiza estado del mensaje`() = runBlocking {
        // Arrange
        val mensaje = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        mensajeDao.insertarMensaje(mensaje)

        // Act
        mensajeDao.marcarComoLeido("msg1")

        // Assert
        val mensajes = mensajeDao.getMensajesPorConversacion("conv1").first()
        assertTrue(mensajes[0].leido)
    }

    @Test
    fun `marcarTodosComoLeidos actualiza todos los mensajes de la conversacion`() = runBlocking {
        // Arrange
        val msg1 = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        val msg2 = crearMensaje("msg2", "conv1", leido = false, esRemitente = false)
        val msg3 = crearMensaje("msg3", "conv2", leido = false, esRemitente = false) // Otra conversación

        mensajeDao.insertarMensaje(msg1)
        mensajeDao.insertarMensaje(msg2)
        mensajeDao.insertarMensaje(msg3)

        // Act
        mensajeDao.marcarTodosComoLeidos("conv1")

        // Assert
        val countConv1 = mensajeDao.contarNoLeidos("conv1").first()
        val countConv2 = mensajeDao.contarNoLeidos("conv2").first()

        assertEquals(0, countConv1) // Conv1 todos leídos
        assertEquals(1, countConv2) // Conv2 sigue con no leído
    }

    @Test
    fun `eliminarConversacion borra todos los mensajes de esa conversacion`() = runBlocking {
        // Arrange
        val msg1 = crearMensaje("msg1", "conv1", leido = false, esRemitente = false)
        val msg2 = crearMensaje("msg2", "conv1", leido = false, esRemitente = false)
        val msg3 = crearMensaje("msg3", "conv2", leido = false, esRemitente = false)

        mensajeDao.insertarMensaje(msg1)
        mensajeDao.insertarMensaje(msg2)
        mensajeDao.insertarMensaje(msg3)

        // Act
        mensajeDao.eliminarConversacion("conv1")

        // Assert
        val mensajesConv1 = mensajeDao.getMensajesPorConversacion("conv1").first()
        val mensajesConv2 = mensajeDao.getMensajesPorConversacion("conv2").first()

        assertEquals(0, mensajesConv1.size)
        assertEquals(1, mensajesConv2.size)
    }

    // Helper para crear mensajes de prueba
    private fun crearMensaje(
        id: String,
        conversacionId: String,
        leido: Boolean = false,
        esRemitente: Boolean = false
    ): MensajeLocal {
        return MensajeLocal(
            id = id,
            conversacionId = conversacionId,
            remitenteId = "user1",
            destinatarioId = "user2",
            texto = "Mensaje de prueba",
            fechaEnvio = System.currentTimeMillis(),
            leido = leido,
            entregado = true,
            esRemitente = esRemitente
        )
    }
}