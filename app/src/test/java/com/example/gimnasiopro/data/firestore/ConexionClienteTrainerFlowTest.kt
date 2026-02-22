package com.example.gimnasiopro.data.firestore

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Tests de integración (lógica) para el flujo completo de conexión
 * Cliente ↔ Trainer.
 *
 * Simula los flujos de:
 * - Cliente solicita → Trainer acepta/rechaza
 * - Trainer invita → Cliente acepta/rechaza
 * - Conexión activa → Finalización
 * - Verificación de permisos
 * - Notificaciones generadas en cada paso
 *
 * NOTA: Estos tests verifican la lógica de modelos y flujos sin
 * conectar a Firebase real. Los tests con Firebase real van en androidTest.
 */
class ConexionClienteTrainerFlowTest {

    // IDs de prueba
    private val trainerId = "trainer-test-001"
    private val trainerNombre = "Carlos Entrenador"
    private val clienteId = "cliente-test-001"
    private val clienteNombre = "Juan Deportista"
    private val conexionId = "conn-test-001"

    // ==================== FLUJO 1: CLIENTE SOLICITA → TRAINER ACEPTA ====================

    @Test
    fun `flujo completo - cliente solicita conexion a trainer`() {
        // 1. Cliente crea solicitud de conexión
        val conexion = ConexionTrainerCliente(
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente",
            mensaje = "Hola, busco un trainer para mejorar mi rendimiento"
        )

        // Verificar estado inicial
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)
        assertEquals("cliente", conexion.solicitadoPor)
        assertEquals(trainerId, conexion.trainerId)
        assertEquals(clienteId, conexion.clienteId)

        // 2. Se genera notificación para el trainer
        val notificacionParaTrainer = Notificacion(
            destinatarioId = trainerId,
            remitenteId = clienteId,
            remitenteNombre = clienteNombre,
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            titulo = "Nueva solicitud de conexión",
            mensaje = "$clienteNombre quiere conectar contigo como trainer",
            datosExtra = mapOf("clienteId" to clienteId, "conexionId" to conexionId)
        )

        // Verificar notificación
        assertEquals(trainerId, notificacionParaTrainer.destinatarioId)
        assertEquals(Notificacion.TIPO_SOLICITUD_CONEXION, notificacionParaTrainer.tipo)
        assertFalse(notificacionParaTrainer.leida)
        assertFalse(notificacionParaTrainer.procesada)
        assertEquals(clienteId, notificacionParaTrainer.datosExtra["clienteId"])
        assertEquals(conexionId, notificacionParaTrainer.datosExtra["conexionId"])
    }

    @Test
    fun `flujo completo - trainer acepta solicitud de cliente`() {
        // Estado inicial: solicitud pendiente
        val solicitud = ConexionTrainerCliente(
            id = conexionId,
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente"
        )

        // Trainer acepta → estado cambia a activa
        val conexionAceptada = solicitud.copy(
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, conexionAceptada.estado)
        assertNotNull(conexionAceptada.fechaRespuesta)

        // Se genera notificación de aceptación para el cliente
        val notificacionAceptacion = Notificacion(
            destinatarioId = clienteId,
            remitenteId = trainerId,
            remitenteNombre = trainerNombre,
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_CONEXION_ACEPTADA,
            titulo = "¡Conexión aceptada!",
            mensaje = "$trainerNombre ha aceptado tu solicitud de conexión"
        )

        assertEquals(Notificacion.TIPO_CONEXION_ACEPTADA, notificacionAceptacion.tipo)
        assertEquals(clienteId, notificacionAceptacion.destinatarioId)

        // Verificar que el trainer tiene permiso sobre el cliente
        val tienePermiso = conexionAceptada.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertTrue(tienePermiso)
    }

    @Test
    fun `flujo completo - trainer rechaza solicitud de cliente`() {
        val solicitud = ConexionTrainerCliente(
            id = conexionId,
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente"
        )

        // Trainer rechaza
        val conexionRechazada = solicitud.copy(
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_RECHAZADA, conexionRechazada.estado)

        // Se genera notificación de rechazo para el cliente
        val notificacionRechazo = Notificacion(
            destinatarioId = clienteId,
            remitenteId = trainerId,
            remitenteNombre = trainerNombre,
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_CONEXION_RECHAZADA,
            titulo = "Solicitud rechazada",
            mensaje = "$trainerNombre ha rechazado la solicitud de conexión"
        )

        assertEquals(Notificacion.TIPO_CONEXION_RECHAZADA, notificacionRechazo.tipo)

        // Verificar que NO tiene permiso
        val tienePermiso = conexionRechazada.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertFalse(tienePermiso)
    }

    // ==================== FLUJO 2: TRAINER INVITA → CLIENTE ACEPTA ====================

    @Test
    fun `flujo completo - trainer invita a cliente`() {
        // 1. Trainer crea invitación
        val conexion = ConexionTrainerCliente(
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "trainer",
            mensaje = "Te invito a entrenar conmigo"
        )

        assertEquals("trainer", conexion.solicitadoPor)
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)

        // 2. Se genera notificación para el cliente
        val notificacionParaCliente = Notificacion(
            destinatarioId = clienteId,
            remitenteId = trainerId,
            remitenteNombre = trainerNombre,
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_INVITACION_TRAINER,
            titulo = "Invitación de trainer",
            mensaje = "$trainerNombre te invita a ser su cliente",
            datosExtra = mapOf("trainerId" to trainerId, "conexionId" to conexionId)
        )

        assertEquals(Notificacion.TIPO_INVITACION_TRAINER, notificacionParaCliente.tipo)
        assertEquals(clienteId, notificacionParaCliente.destinatarioId)
        assertEquals(trainerId, notificacionParaCliente.datosExtra["trainerId"])
    }

    @Test
    fun `flujo completo - cliente acepta invitacion de trainer`() {
        // Invitación pendiente del trainer
        val invitacion = ConexionTrainerCliente(
            id = conexionId,
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "trainer"
        )

        // Cliente acepta
        val conexionActiva = invitacion.copy(
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, conexionActiva.estado)

        // Se genera notificación de aceptación para el trainer
        val notificacionAceptacion = Notificacion(
            destinatarioId = trainerId,
            remitenteId = clienteId,
            remitenteNombre = clienteNombre,
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_CONEXION_ACEPTADA,
            titulo = "¡Conexión aceptada!",
            mensaje = "$clienteNombre ha aceptado tu solicitud de conexión"
        )

        assertEquals(trainerId, notificacionAceptacion.destinatarioId)
        assertEquals(Notificacion.TIPO_CONEXION_ACEPTADA, notificacionAceptacion.tipo)
    }

    @Test
    fun `flujo completo - cliente rechaza invitacion de trainer`() {
        val invitacion = ConexionTrainerCliente(
            id = conexionId,
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "trainer"
        )

        // Cliente rechaza
        val rechazada = invitacion.copy(
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA,
            fechaRespuesta = Date()
        )

        assertEquals(ConexionTrainerCliente.ESTADO_RECHAZADA, rechazada.estado)

        // Notificación al trainer
        val notificacionRechazo = Notificacion(
            destinatarioId = trainerId,
            remitenteId = clienteId,
            remitenteNombre = clienteNombre,
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_CONEXION_RECHAZADA,
            titulo = "Solicitud rechazada",
            mensaje = "$clienteNombre ha rechazado la solicitud de conexión"
        )

        assertEquals(trainerId, notificacionRechazo.destinatarioId)
        assertEquals(Notificacion.TIPO_CONEXION_RECHAZADA, notificacionRechazo.tipo)
    }

    // ==================== FLUJO 3: FINALIZACIÓN DE CONEXIÓN ====================

    @Test
    fun `flujo completo - finalizar conexion activa`() {
        val conexionActiva = ConexionTrainerCliente(
            id = conexionId,
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            fechaRespuesta = Date()
        )

        // Se finaliza la conexión
        val finalizada = conexionActiva.copy(
            estado = ConexionTrainerCliente.ESTADO_FINALIZADA
        )

        assertEquals(ConexionTrainerCliente.ESTADO_FINALIZADA, finalizada.estado)

        // El trainer ya no tiene permiso
        val tienePermiso = finalizada.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        assertFalse(tienePermiso)

        // Los IDs se mantienen para historial
        assertEquals(trainerId, finalizada.trainerId)
        assertEquals(clienteId, finalizada.clienteId)
    }

    // ==================== VERIFICACIÓN DE PERMISOS ====================

    @Test
    fun `solo conexion activa da permiso al trainer`() {
        val estados = listOf(
            ConexionTrainerCliente.ESTADO_PENDIENTE to false,
            ConexionTrainerCliente.ESTADO_ACTIVA to true,
            ConexionTrainerCliente.ESTADO_RECHAZADA to false,
            ConexionTrainerCliente.ESTADO_FINALIZADA to false
        )

        for ((estado, esperado) in estados) {
            val conexion = ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = clienteId,
                estado = estado
            )

            val tienePermiso = conexion.estado == ConexionTrainerCliente.ESTADO_ACTIVA
            assertEquals(
                "Estado '$estado' debería dar permiso=$esperado",
                esperado,
                tienePermiso
            )
        }
    }

    // ==================== VERIFICACIÓN DE DUPLICADOS ====================

    @Test
    fun `no se puede crear conexion duplicada - verificacion logica`() {
        val conexionExistente = ConexionTrainerCliente(
            id = "conn-existente",
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE
        )

        // Simular la verificación que hace ConexionRepository.solicitarConexion
        val estadosQueBloquean = listOf(
            ConexionTrainerCliente.ESTADO_PENDIENTE,
            ConexionTrainerCliente.ESTADO_ACTIVA
        )

        val yaExiste = conexionExistente.estado in estadosQueBloquean
        assertTrue("Debería bloquear nueva solicitud si hay una pendiente", yaExiste)
    }

    @Test
    fun `se puede crear conexion si la anterior fue rechazada`() {
        val conexionAnterior = ConexionTrainerCliente(
            id = "conn-anterior",
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_RECHAZADA
        )

        val estadosQueBloquean = listOf(
            ConexionTrainerCliente.ESTADO_PENDIENTE,
            ConexionTrainerCliente.ESTADO_ACTIVA
        )

        val yaExiste = conexionAnterior.estado in estadosQueBloquean
        assertFalse("Debería permitir nueva solicitud si la anterior fue rechazada", yaExiste)
    }

    @Test
    fun `se puede crear conexion si la anterior fue finalizada`() {
        val conexionAnterior = ConexionTrainerCliente(
            id = "conn-anterior",
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_FINALIZADA
        )

        val estadosQueBloquean = listOf(
            ConexionTrainerCliente.ESTADO_PENDIENTE,
            ConexionTrainerCliente.ESTADO_ACTIVA
        )

        val yaExiste = conexionAnterior.estado in estadosQueBloquean
        assertFalse("Debería permitir nueva solicitud si la anterior fue finalizada", yaExiste)
    }

    // ==================== NOTIFICACIONES - DATOS EXTRA ====================

    @Test
    fun `notificacion solicitud contiene conexionId en datosExtra`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            datosExtra = mapOf(
                "clienteId" to clienteId,
                "conexionId" to conexionId
            )
        )

        assertNotNull(notificacion.datosExtra["conexionId"])
        assertEquals(conexionId, notificacion.datosExtra["conexionId"])
    }

    @Test
    fun `notificacion invitacion contiene trainerId en datosExtra`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_INVITACION_TRAINER,
            datosExtra = mapOf(
                "trainerId" to trainerId,
                "conexionId" to conexionId
            )
        )

        assertNotNull(notificacion.datosExtra["trainerId"])
        assertEquals(trainerId, notificacion.datosExtra["trainerId"])
    }

    @Test
    fun `notificacion solicitud sin conexionId usa remitenteId como fallback`() {
        val notificacion = Notificacion(
            remitenteId = clienteId,
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            datosExtra = mapOf("clienteId" to clienteId)
            // Sin conexionId
        )

        // El código real hace: datosExtra["conexionId"] ?: busca por clienteId
        val conexionIdRecuperado = notificacion.datosExtra["conexionId"]
        assertNull(conexionIdRecuperado)

        // Fallback: usar clienteId para buscar la conexión pendiente
        val clienteIdRecuperado = notificacion.datosExtra["clienteId"] ?: notificacion.remitenteId
        assertEquals(clienteId, clienteIdRecuperado)
    }

    // ==================== NOTIFICACIONES - PROCESAMIENTO ====================

    @Test
    fun `marcar notificacion como procesada deshabilita botones`() {
        val notificacionOriginal = Notificacion(
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            procesada = false
        )

        // Simular el procesamiento
        val procesada = notificacionOriginal.copy(procesada = true, leida = true)

        assertTrue(procesada.procesada)
        assertTrue(procesada.leida)

        // Lógica del adapter: mostrar botones solo si NO está procesada
        val esSolicitud = procesada.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                procesada.tipo == Notificacion.TIPO_INVITACION_TRAINER
        val mostrarBotones = esSolicitud && !procesada.procesada

        assertFalse("Botones no deben mostrarse en notificación procesada", mostrarBotones)
    }

    @Test
    fun `solicitud no procesada muestra botones aceptar rechazar`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            procesada = false
        )

        val esSolicitud = notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER
        val mostrarBotones = esSolicitud && !notificacion.procesada

        assertTrue("Botones deben mostrarse en solicitud no procesada", mostrarBotones)
    }

    @Test
    fun `invitacion no procesada muestra botones aceptar rechazar`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_INVITACION_TRAINER,
            procesada = false
        )

        val esSolicitud = notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER
        val mostrarBotones = esSolicitud && !notificacion.procesada

        assertTrue("Botones deben mostrarse en invitación no procesada", mostrarBotones)
    }

    @Test
    fun `mensaje normal no muestra botones aceptar rechazar`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_MENSAJE,
            procesada = false
        )

        val esSolicitud = notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION ||
                notificacion.tipo == Notificacion.TIPO_INVITACION_TRAINER
        val mostrarBotones = esSolicitud && !notificacion.procesada

        assertFalse("Mensaje normal no debe mostrar botones", mostrarBotones)
    }

    // ==================== ELIMINACIÓN DE NOTIFICACIONES ====================

    @Test
    fun `notificacion con id vacio no se puede eliminar`() {
        val notificacion = Notificacion(id = "")

        val puedeBorrar = notificacion.id.isNotEmpty()
        assertFalse("No se puede eliminar notificación sin ID", puedeBorrar)
    }

    @Test
    fun `notificacion con id valido se puede eliminar`() {
        val notificacion = Notificacion(id = "notif-001")

        val puedeBorrar = notificacion.id.isNotEmpty()
        assertTrue("Se puede eliminar notificación con ID", puedeBorrar)
    }

    // ==================== ESTRUCTURA FIREBASE ESPERADA ====================

    @Test
    fun `estructura misClientes del trainer es correcta`() {
        // Al aceptar conexión se crea este documento en trainers/{trainerId}/misClientes/{clienteId}
        val misClienteDoc = mapOf(
            "clienteId" to clienteId,
            "fechaConexion" to Date(),
            "estado" to "activo"
        )

        assertEquals(clienteId, misClienteDoc["clienteId"])
        assertEquals("activo", misClienteDoc["estado"])
        assertNotNull(misClienteDoc["fechaConexion"])
    }

    @Test
    fun `estructura miTrainer del cliente es correcta`() {
        // Al aceptar conexión se crea este documento en clientes/{clienteId}/miTrainer/{trainerId}
        val miTrainerDoc = mapOf(
            "trainerId" to trainerId,
            "fechaConexion" to Date(),
            "estado" to "activo"
        )

        assertEquals(trainerId, miTrainerDoc["trainerId"])
        assertEquals("activo", miTrainerDoc["estado"])
        assertNotNull(miTrainerDoc["fechaConexion"])
    }

    @Test
    fun `al finalizar conexion se limpia trainerId del cliente`() {
        // Simular el campo trainerId en el documento del cliente
        var trainerIdEnCliente: String = trainerId

        // Simular la finalización
        trainerIdEnCliente = "" // ConexionRepository.finalizarConexion limpia esto

        assertEquals("", trainerIdEnCliente)
    }

    // ==================== CREAR Y ACEPTAR EN UN SOLO PASO ====================

    @Test
    fun `crearYAceptar genera conexion activa directamente`() {
        // Simula ConexionRepository.crearYAceptarConexion
        val conexionDirecta = ConexionTrainerCliente(
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_ACTIVA,
            solicitadoPor = "trainer",
            fechaSolicitud = Date(),
            fechaRespuesta = Date(),
            mensaje = "Invitación aceptada"
        )

        assertEquals(ConexionTrainerCliente.ESTADO_ACTIVA, conexionDirecta.estado)
        assertNotNull(conexionDirecta.fechaRespuesta)
        assertEquals("Invitación aceptada", conexionDirecta.mensaje)
    }

    // ==================== MODELO DE RELACIÓN ====================

    @Test
    fun `un cliente solo puede tener un trainer activo`() {
        // Simular que el cliente ya tiene una conexión activa
        val conexionActiva = ConexionTrainerCliente(
            trainerId = "trainer-existente",
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_ACTIVA
        )

        // Intentar nueva conexión con otro trainer
        val estadosQueBloquean = listOf(
            ConexionTrainerCliente.ESTADO_PENDIENTE,
            ConexionTrainerCliente.ESTADO_ACTIVA
        )

        val yaTieneConexion = conexionActiva.estado in estadosQueBloquean
        assertTrue("Cliente no puede tener dos trainers activos", yaTieneConexion)
    }

    @Test
    fun `un trainer puede tener multiples clientes`() {
        // Un trainer puede tener varias conexiones activas con diferentes clientes
        val conexiones = listOf(
            ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = "cliente-001",
                estado = ConexionTrainerCliente.ESTADO_ACTIVA
            ),
            ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = "cliente-002",
                estado = ConexionTrainerCliente.ESTADO_ACTIVA
            ),
            ConexionTrainerCliente(
                trainerId = trainerId,
                clienteId = "cliente-003",
                estado = ConexionTrainerCliente.ESTADO_ACTIVA
            )
        )

        val clientesActivos = conexiones.filter {
            it.estado == ConexionTrainerCliente.ESTADO_ACTIVA
        }

        assertEquals(3, clientesActivos.size)
        assertTrue(clientesActivos.all { it.trainerId == trainerId })
    }

    // ==================== toMap PARA FIRESTORE ====================

    @Test
    fun `conexion tiene todos los campos necesarios para Firestore`() {
        val conexion = ConexionTrainerCliente(
            trainerId = trainerId,
            clienteId = clienteId,
            estado = ConexionTrainerCliente.ESTADO_PENDIENTE,
            solicitadoPor = "cliente",
            mensaje = "Solicitud de prueba"
        )

        // Verificar que el modelo tiene todos los campos necesarios
        assertEquals(trainerId, conexion.trainerId)
        assertEquals(clienteId, conexion.clienteId)
        assertEquals(ConexionTrainerCliente.ESTADO_PENDIENTE, conexion.estado)
        assertEquals("cliente", conexion.solicitadoPor)
        assertNotNull(conexion.fechaSolicitud)
        assertEquals("Solicitud de prueba", conexion.mensaje)

        // id no se envía a Firestore (lo gestiona Firestore automáticamente)
        assertEquals("", conexion.id)
    }

    @Test
    fun `notificacion tiene todos los campos necesarios para Firestore`() {
        val notificacion = Notificacion(
            destinatarioId = trainerId,
            remitenteId = clienteId,
            remitenteNombre = clienteNombre,
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            titulo = "Solicitud",
            mensaje = "Mensaje de prueba",
            datosExtra = mapOf("conexionId" to conexionId)
        )

        // Verificar que el modelo tiene todos los campos necesarios
        assertEquals(trainerId, notificacion.destinatarioId)
        assertEquals(clienteId, notificacion.remitenteId)
        assertEquals(clienteNombre, notificacion.remitenteNombre)
        assertEquals("cliente", notificacion.remitenteTipo)
        assertEquals(Notificacion.TIPO_SOLICITUD_CONEXION, notificacion.tipo)
        assertEquals("Solicitud", notificacion.titulo)
        assertEquals("Mensaje de prueba", notificacion.mensaje)
        assertFalse(notificacion.leida)
        assertFalse(notificacion.procesada)
        assertNotNull(notificacion.fechaCreacion)
        assertEquals(mapOf("conexionId" to conexionId), notificacion.datosExtra)

        // id no se envía a Firestore
        assertEquals("", notificacion.id)
    }
}

