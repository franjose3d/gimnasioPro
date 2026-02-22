package com.example.gimnasiopro.data.firestore

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Tests unitarios para el modelo Notificacion.
 * Verifica la creación, serialización (toMap), tipos y expiración
 * de las notificaciones del sistema de conexión cliente-trainer.
 */
class NotificacionTest {

    // ==================== CREACIÓN DE NOTIFICACIÓN ====================

    @Test
    fun `crear notificacion con valores por defecto`() {
        val notificacion = Notificacion()

        assertEquals("", notificacion.id)
        assertEquals("", notificacion.destinatarioId)
        assertEquals("", notificacion.remitenteId)
        assertEquals("", notificacion.remitenteNombre)
        assertEquals("", notificacion.remitenteTipo)
        assertEquals(Notificacion.TIPO_MENSAJE, notificacion.tipo)
        assertEquals("", notificacion.titulo)
        assertEquals("", notificacion.mensaje)
        assertFalse(notificacion.leida)
        assertFalse(notificacion.procesada)
        assertNotNull(notificacion.fechaCreacion)
        assertNull(notificacion.fechaExpiracion)
        assertTrue(notificacion.datosExtra.isEmpty())
    }

    // ==================== TIPOS DE NOTIFICACIÓN ====================

    @Test
    fun `constantes de tipo tienen valores correctos`() {
        assertEquals("solicitud_conexion", Notificacion.TIPO_SOLICITUD_CONEXION)
        assertEquals("invitacion_trainer", Notificacion.TIPO_INVITACION_TRAINER)
        assertEquals("mensaje", Notificacion.TIPO_MENSAJE)
        assertEquals("rutina_actualizada", Notificacion.TIPO_RUTINA_ACTUALIZADA)
        assertEquals("conexion_aceptada", Notificacion.TIPO_CONEXION_ACEPTADA)
        assertEquals("conexion_rechazada", Notificacion.TIPO_CONEXION_RECHAZADA)
        assertEquals("sistema", Notificacion.TIPO_SISTEMA)
    }

    // ==================== SOLICITUD DE CONEXIÓN (Cliente → Trainer) ====================

    @Test
    fun `crear notificacion de solicitud conexion cliente a trainer`() {
        val notificacion = Notificacion(
            id = "notif-001",
            destinatarioId = "trainer-abc",
            remitenteId = "cliente-xyz",
            remitenteNombre = "Juan García",
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            titulo = "Nueva solicitud de conexión",
            mensaje = "Juan García quiere conectar contigo como trainer",
            datosExtra = mapOf("clienteId" to "cliente-xyz", "conexionId" to "conn-001")
        )

        assertEquals("trainer-abc", notificacion.destinatarioId)
        assertEquals("cliente-xyz", notificacion.remitenteId)
        assertEquals("cliente", notificacion.remitenteTipo)
        assertEquals(Notificacion.TIPO_SOLICITUD_CONEXION, notificacion.tipo)
        assertEquals("cliente-xyz", notificacion.datosExtra["clienteId"])
        assertEquals("conn-001", notificacion.datosExtra["conexionId"])
        assertFalse(notificacion.procesada)
    }

    // ==================== INVITACIÓN DE TRAINER (Trainer → Cliente) ====================

    @Test
    fun `crear notificacion de invitacion trainer a cliente`() {
        val notificacion = Notificacion(
            destinatarioId = "cliente-xyz",
            remitenteId = "trainer-abc",
            remitenteNombre = "Carlos Trainer",
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_INVITACION_TRAINER,
            titulo = "Invitación de trainer",
            mensaje = "Carlos Trainer te invita a ser su cliente",
            datosExtra = mapOf("trainerId" to "trainer-abc", "conexionId" to "conn-002")
        )

        assertEquals("cliente-xyz", notificacion.destinatarioId)
        assertEquals("trainer-abc", notificacion.remitenteId)
        assertEquals("trainer", notificacion.remitenteTipo)
        assertEquals(Notificacion.TIPO_INVITACION_TRAINER, notificacion.tipo)
        assertEquals("trainer-abc", notificacion.datosExtra["trainerId"])
        assertEquals("conn-002", notificacion.datosExtra["conexionId"])
    }

    // ==================== RESPUESTA A CONEXIÓN ====================

    @Test
    fun `crear notificacion de conexion aceptada`() {
        val notificacion = Notificacion(
            destinatarioId = "cliente-xyz",
            remitenteId = "trainer-abc",
            remitenteNombre = "Carlos Trainer",
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_CONEXION_ACEPTADA,
            titulo = "¡Conexión aceptada!",
            mensaje = "Carlos Trainer ha aceptado tu solicitud de conexión"
        )

        assertEquals(Notificacion.TIPO_CONEXION_ACEPTADA, notificacion.tipo)
        assertTrue(notificacion.titulo.contains("aceptada"))
    }

    @Test
    fun `crear notificacion de conexion rechazada`() {
        val notificacion = Notificacion(
            destinatarioId = "cliente-xyz",
            remitenteId = "trainer-abc",
            remitenteNombre = "Carlos Trainer",
            remitenteTipo = "trainer",
            tipo = Notificacion.TIPO_CONEXION_RECHAZADA,
            titulo = "Solicitud rechazada",
            mensaje = "Carlos Trainer ha rechazado la solicitud de conexión"
        )

        assertEquals(Notificacion.TIPO_CONEXION_RECHAZADA, notificacion.tipo)
        assertTrue(notificacion.titulo.contains("rechazada"))
    }

    // ==================== SERIALIZACIÓN toMap ====================

    @Test
    fun `toMap contiene todos los campos requeridos`() {
        val notificacion = Notificacion(
            id = "notif-001",
            destinatarioId = "trainer-abc",
            remitenteId = "cliente-xyz",
            remitenteNombre = "Juan García",
            remitenteTipo = "cliente",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            titulo = "Nueva solicitud",
            mensaje = "Quiero entrenar contigo",
            datosExtra = mapOf("clienteId" to "cliente-xyz")
        )

        val map = notificacion.toMap()

        assertEquals("trainer-abc", map["destinatarioId"])
        assertEquals("cliente-xyz", map["remitenteId"])
        assertEquals("Juan García", map["remitenteNombre"])
        assertEquals("cliente", map["remitenteTipo"])
        assertEquals(Notificacion.TIPO_SOLICITUD_CONEXION, map["tipo"])
        assertEquals("Nueva solicitud", map["titulo"])
        assertEquals("Quiero entrenar contigo", map["mensaje"])
        assertEquals(false, map["leida"])
        assertEquals(false, map["procesada"])
        assertNotNull(map["fechaCreacion"])
        @Suppress("UNCHECKED_CAST")
        val extras = map["datosExtra"] as Map<String, String>
        assertEquals("cliente-xyz", extras["clienteId"])
    }

    @Test
    fun `toMap no incluye el id del documento`() {
        val notificacion = Notificacion(id = "notif-001")

        val map = notificacion.toMap()

        assertFalse(map.containsKey("id"))
    }

    @Test
    fun `toMap trunca mensaje a 200 caracteres`() {
        val mensajeLargo = "A".repeat(300)
        val notificacion = Notificacion(mensaje = mensajeLargo)

        val map = notificacion.toMap()

        val mensajeGuardado = map["mensaje"] as String
        assertEquals(200, mensajeGuardado.length)
    }

    @Test
    fun `toMap con fechaExpiracion null`() {
        val notificacion = Notificacion(fechaExpiracion = null)

        val map = notificacion.toMap()

        assertNull(map["fechaExpiracion"])
    }

    @Test
    fun `toMap con fechaExpiracion no null`() {
        val fecha = Date()
        val notificacion = Notificacion(fechaExpiracion = fecha)

        val map = notificacion.toMap()

        assertNotNull(map["fechaExpiracion"])
    }

    @Test
    fun `toMap con datosExtra vacios`() {
        val notificacion = Notificacion(datosExtra = emptyMap())

        val map = notificacion.toMap()

        @Suppress("UNCHECKED_CAST")
        val extras = map["datosExtra"] as Map<String, String>
        assertTrue(extras.isEmpty())
    }

    // ==================== EXPIRACIÓN DE MENSAJES ====================

    @Test
    fun `duracion de mensaje es 7 dias en milisegundos`() {
        val sieteDiasMs = 7L * 24 * 60 * 60 * 1000
        assertEquals(sieteDiasMs, Notificacion.DURACION_MENSAJE_MS)
    }

    @Test
    fun `calcular fecha expiracion es 7 dias en el futuro`() {
        val antes = System.currentTimeMillis()
        val fechaExpiracion = Notificacion.calcularFechaExpiracion()
        val despues = System.currentTimeMillis()

        val sieteDiasMs = Notificacion.DURACION_MENSAJE_MS

        assertTrue(fechaExpiracion.time >= antes + sieteDiasMs)
        assertTrue(fechaExpiracion.time <= despues + sieteDiasMs)
    }

    @Test
    fun `mensaje con expiracion futura no esta expirado`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_MENSAJE,
            fechaExpiracion = Notificacion.calcularFechaExpiracion()
        )

        assertTrue(notificacion.fechaExpiracion!!.after(Date()))
    }

    @Test
    fun `mensaje con expiracion pasada esta expirado`() {
        val fechaPasada = Date(System.currentTimeMillis() - 1000) // 1 segundo atrás
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_MENSAJE,
            fechaExpiracion = fechaPasada
        )

        assertFalse(notificacion.fechaExpiracion!!.after(Date()))
    }

    // ==================== ESTADOS DE LECTURA ====================

    @Test
    fun `notificacion nueva no esta leida ni procesada`() {
        val notificacion = Notificacion(
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION
        )

        assertFalse(notificacion.leida)
        assertFalse(notificacion.procesada)
    }

    @Test
    fun `notificacion marcada como leida`() {
        val notificacion = Notificacion(leida = true)

        assertTrue(notificacion.leida)
    }

    @Test
    fun `notificacion procesada tambien esta leida`() {
        val notificacion = Notificacion(
            leida = true,
            procesada = true
        )

        assertTrue(notificacion.leida)
        assertTrue(notificacion.procesada)
    }

    // ==================== COMPARACIÓN ====================

    @Test
    fun `dos notificaciones iguales son iguales`() {
        val fecha = Date()
        val notif1 = Notificacion(
            id = "notif-001",
            destinatarioId = "user-1",
            remitenteId = "user-2",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            fechaCreacion = fecha
        )
        val notif2 = Notificacion(
            id = "notif-001",
            destinatarioId = "user-1",
            remitenteId = "user-2",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            fechaCreacion = fecha
        )

        assertEquals(notif1, notif2)
    }

    @Test
    fun `notificaciones con diferente tipo no son iguales`() {
        val fecha = Date()
        val solicitud = Notificacion(
            id = "notif-001",
            tipo = Notificacion.TIPO_SOLICITUD_CONEXION,
            fechaCreacion = fecha
        )
        val invitacion = Notificacion(
            id = "notif-001",
            tipo = Notificacion.TIPO_INVITACION_TRAINER,
            fechaCreacion = fecha
        )

        assertNotEquals(solicitud, invitacion)
    }
}

