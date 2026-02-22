package com.example.gimnasiopro.data.firestore

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para el modelo ConexionTrainerCliente.
 * Verifica la creación, serialización (toMap) y estados de la conexión.
 */
class ConexionTrainerClienteTest {

    // ==================== CREACIÓN DE CONEXIÓN ====================

    @Test
    fun `crear conexion con valores por defecto`() {
        val conexion = ConexionTrainerCliente()

        assertEquals("", conexion.id)
        assertEquals("", conexion.trainerId)
        assertEquals("", conexion.clienteId)
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)
        assertEquals("cliente", conexion.solicitadoPor)
        assertNotNull(conexion.fechaSolicitud)
        assertNull(conexion.fechaRespuesta)
        assertEquals("", conexion.mensaje)
    }

    @Test
    fun `crear conexion cliente solicita trainer`() {
        val conexion = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente",
            mensaje = "Hola, me gustaría entrenar contigo"
        )

        assertEquals("conn-001", conexion.id)
        assertEquals("trainer-abc", conexion.trainerId)
        assertEquals("cliente-xyz", conexion.clienteId)
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)
        assertEquals("cliente", conexion.solicitadoPor)
        assertEquals("Hola, me gustaría entrenar contigo", conexion.mensaje)
    }

    @Test
    fun `crear conexion trainer invita cliente`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "trainer",
            mensaje = "Te invito a entrenar conmigo"
        )

        assertEquals("trainer", conexion.solicitadoPor)
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)
    }

    // ==================== ESTADOS DE CONEXIÓN ====================

    @Test
    fun `constantes de estado tienen valores correctos`() {
        assertEquals("pendiente", ConexionTrainerCliente.ESTADO_PENDIENTE)
        assertEquals("activa", ConexionTrainerCliente.ESTADO_ACTIVA)
        assertEquals("rechazada", ConexionTrainerCliente.ESTADO_RECHAZADA)
        assertEquals("finalizada", ConexionTrainerCliente.ESTADO_FINALIZADA)
    }

    @Test
    fun `conexion activa tiene estado correcto`() {
        val fechaRespuesta = Date()
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = fechaRespuesta
        )

        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, conexion.estado)
        assertNotNull(conexion.fechaRespuesta)
    }

    @Test
    fun `conexion rechazada tiene estado correcto`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_RECHAZADA, conexion.estado)
    }

    @Test
    fun `conexion finalizada tiene estado correcto`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_FINALIZADA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_FINALIZADA, conexion.estado)
    }

    // ==================== SERIALIZACIÓN toMap ====================

    @Test
    fun `toMap contiene todos los campos requeridos`() {
        val fecha = Date()
        val conexion = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente",
            fechaSolicitud = fecha,
            mensaje = "Mensaje de prueba"
        )

        val map = conexion.toMap()

        assertEquals("trainer-abc", map["trainerId"])
        assertEquals("cliente-xyz", map["clienteId"])
        assertEquals("pendiente", map["estado"])
        assertEquals("cliente", map["solicitadoPor"])
        assertEquals("Mensaje de prueba", map["mensaje"])
        assertNotNull(map["fechaSolicitud"])
    }

    @Test
    fun `toMap no incluye el id del documento`() {
        val conexion = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz"
        )

        val map = conexion.toMap()

        // El ID del documento es gestionado por Firestore, no debe ir en el map
        assertFalse(map.containsKey("id"))
    }

    @Test
    fun `toMap con fechaRespuesta null`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            fechaRespuesta = null
        )

        val map = conexion.toMap()

        assertNull(map["fechaRespuesta"])
    }

    @Test
    fun `toMap con fechaRespuesta no null`() {
        val fecha = Date()
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            fechaRespuesta = fecha
        )

        val map = conexion.toMap()

        assertNotNull(map["fechaRespuesta"])
    }

    // ==================== FLUJOS DE CONEXIÓN ====================

    @Test
    fun `flujo completo - cliente solicita, trainer acepta`() {
        // 1. Cliente crea solicitud
        val solicitud = ConexionTrainerCliente(
            trainerId = "trainer-001",
            clienteId = "cliente-001",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente",
            mensaje = "Quiero entrenar contigo"
        )
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, solicitud.estado)
        assertEquals("cliente", solicitud.solicitadoPor)

        // 2. Simulamos la aceptación (nuevo objeto con estado activa)
        val aceptada = solicitud.copy(
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )
        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, aceptada.estado)
        assertNotNull(aceptada.fechaRespuesta)
        // Los IDs se mantienen
        assertEquals("trainer-001", aceptada.trainerId)
        assertEquals("cliente-001", aceptada.clienteId)
    }

    @Test
    fun `flujo completo - trainer invita, cliente acepta`() {
        // 1. Trainer invita cliente
        val invitacion = ConexionTrainerCliente(
            trainerId = "trainer-002",
            clienteId = "cliente-002",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "trainer",
            mensaje = "Te invito a entrenar"
        )
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, invitacion.estado)
        assertEquals("trainer", invitacion.solicitadoPor)

        // 2. Cliente acepta
        val aceptada = invitacion.copy(
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )
        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, aceptada.estado)
    }

    @Test
    fun `flujo completo - solicitud rechazada`() {
        val solicitud = ConexionTrainerCliente(
            trainerId = "trainer-003",
            clienteId = "cliente-003",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente"
        )

        val rechazada = solicitud.copy(
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_RECHAZADA, rechazada.estado)
        assertNotNull(rechazada.fechaRespuesta)
    }

    @Test
    fun `flujo completo - conexion activa luego finalizada`() {
        val activa = ConexionTrainerCliente(
            trainerId = "trainer-004",
            clienteId = "cliente-004",
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )

        val finalizada = activa.copy(
            estado = ConexionTrainerCliente.ESTADO_FINALIZADA
        )

        assertEquals(ConexionTrainerCliente.ESTADO_FINALIZADA, finalizada.estado)
        // Los datos originales se mantienen
        assertEquals("trainer-004", finalizada.trainerId)
        assertEquals("cliente-004", finalizada.clienteId)
    }

    // ==================== VALIDACIONES DE DATOS ====================

    @Test
    fun `conexion con IDs vacios`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "",
            clienteId = ""
        )

        assertTrue(conexion.trainerId.isEmpty())
        assertTrue(conexion.clienteId.isEmpty())
    }

    @Test
    fun `conexion preserva mensaje largo`() {
        val mensajeLargo = "Este es un mensaje de prueba que tiene muchos caracteres para verificar que se guarda correctamente sin truncar nada en el modelo de datos"
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            mensaje = mensajeLargo
        )

        assertEquals(mensajeLargo, conexion.mensaje)
    }

    @Test
    fun `comparar dos conexiones iguales`() {
        val fecha = Date()
        val conexion1 = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            fechaSolicitud = fecha
        )
        val conexion2 = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            fechaSolicitud = fecha
        )

        assertEquals(conexion1, conexion2)
    }

    @Test
    fun `conexiones con diferente estado no son iguales`() {
        val fecha = Date()
        val pendiente = ConexionTrainerCliente(
            id = "conn-001",
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            fechaSolicitud = fecha
        )
        val activa = pendiente.copy(estado = ConexionTrainerCliente.ESTADO_ACTIVA)

        assertNotEquals(pendiente, activa)
    }

    // ==================== VERIFICACIÓN DE PERMISOS (lógica) ====================

    @Test
    fun `verificar que conexion activa indica permiso`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_ACTIVA
        )

        val tienePermiso = conexion.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertTrue(tienePermiso)
    }

    @Test
    fun `verificar que conexion pendiente no indica permiso`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE
        )

        val tienePermiso = conexion.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertFalse(tienePermiso)
    }

    @Test
    fun `verificar que conexion rechazada no indica permiso`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA
        )

        val tienePermiso = conexion.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertFalse(tienePermiso)
    }

    @Test
    fun `verificar que conexion finalizada no indica permiso`() {
        val conexion = ConexionTrainerCliente(
            trainerId = "trainer-abc",
            clienteId = "cliente-xyz",
            estado = ConexionTrainerCliente.ESTADO_FINALIZADA
        )

        val tienePermiso = conexion.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertFalse(tienePermiso)
    }
}

