package com.example.gimnasiopro.domain.model

/**
 * Modelo del entrenador personal.
 * HEREDA de User (datos comunes) + datos específicos de trainer.
 */
data class Trainer(
    override val userId: String = "",
    override val email: String = "",
    override val nombre: String = "",
    override val telefono: String = "",
    override val emailVerificado: Boolean = false,
    override val fechaRegistro: Long = System.currentTimeMillis(),

    // DATOS ESPECÍFICOS DE TRAINER
    val dni: String = "",              // DNI español (8 dígitos + letra)
    val poblacion: String = "",        // Población donde trabaja
    val municipio: String = "",        // Municipio
    val sobreMi: String = "",          // Descripción personal
    val fotoUrl: String = "",          // URL de la foto de perfil
    val certificadoUrl: String = "",   // URL del certificado de entrenador
    val tarifa: Double = 0.0,          // Tarifa por sesión en euros
    val verificado: Boolean = true,    // SIEMPRE true - perfil revisable pero acceso inmediato
    val numeroClientes: Int = 0,       // Contador de clientes
    val clientesActivos: List<String> = emptyList(), // IDs de clientes asignados

    // PREFERENCIAS DE CONEXIÓN
    val activo: Boolean = true,             // Si el trainer está activo/visible
    val aceptaSolicitudes: Boolean = true,  // Si acepta solicitudes de clientes
    val codigoInvitacion: String = ""       // Código único para invitar clientes
) : User(
    userId = userId,
    email = email,
    nombre = nombre,
    telefono = telefono,
    tipo = "trainer",  // SIEMPRE trainer
    emailVerificado = emailVerificado,
    fechaRegistro = fechaRegistro
) {
    /**
     * Convertir a Map para guardar en trainers/ (colección específica)
     */
    fun toTrainerMap(): Map<String, Any> {
        // Normalizar teléfono: solo dígitos sin prefijo de país
        val telefonoNorm = telefono.replace(Regex("[^0-9]"), "").let { digitos ->
            if (digitos.startsWith("34") && digitos.length > 9) digitos.removePrefix("34") else digitos
        }
        return hashMapOf(
            "userId" to userId,
            "email" to email,
            "nombre" to nombre,
            "telefono" to telefono,
            "telefonoNormalizado" to telefonoNorm,
            "dni" to dni,
            "poblacion" to poblacion,
            "municipio" to municipio,
            "sobreMi" to sobreMi,
            "fotoUrl" to fotoUrl,
            "certificadoUrl" to certificadoUrl,
            "tarifa" to tarifa,
            "verificado" to verificado,
            "emailVerificado" to emailVerificado,
            "numeroClientes" to numeroClientes,
            "clientesActivos" to clientesActivos,
            "fechaRegistro" to fechaRegistro,
            "activo" to activo,
            "aceptaSolicitudes" to aceptaSolicitudes,
            "codigoInvitacion" to codigoInvitacion
        )
    }

    companion object {
        /**
         * Crear Trainer desde Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): Trainer {
            return Trainer(
                userId = map["userId"] as? String ?: "",
                email = map["email"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                telefono = map["telefono"] as? String ?: "",
                dni = map["dni"] as? String ?: "",
                poblacion = map["poblacion"] as? String ?: "",
                municipio = map["municipio"] as? String ?: "",
                sobreMi = map["sobreMi"] as? String ?: "",
                fotoUrl = map["fotoUrl"] as? String ?: "",
                certificadoUrl = map["certificadoUrl"] as? String ?: "",
                tarifa = (map["tarifa"] as? Number)?.toDouble() ?: 0.0,
                verificado = map["verificado"] as? Boolean ?: true,  // SIEMPRE true por defecto
                emailVerificado = map["emailVerificado"] as? Boolean ?: false,
                numeroClientes = (map["numeroClientes"] as? Number)?.toInt() ?: 0,
                clientesActivos = @Suppress("UNCHECKED_CAST") (map["clientesActivos"] as? List<String>) ?: emptyList(),
                fechaRegistro = (map["fechaRegistro"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                aceptaSolicitudes = map["aceptaSolicitudes"] as? Boolean ?: true,
                codigoInvitacion = map["codigoInvitacion"] as? String ?: ""
            )
        }
    }
}