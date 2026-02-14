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
    val tarifa: Double = 0.0,          // Tarifa por sesión en euros
    val verificado: Boolean = false,   // Aprobado por admin
    val numeroClientes: Int = 0,       // Contador de clientes
    val clientesActivos: List<String> = emptyList() // IDs de clientes asignados
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
        return hashMapOf(
            "userId" to userId,
            "email" to email,
            "nombre" to nombre,
            "telefono" to telefono,
            "dni" to dni,
            "poblacion" to poblacion,
            "municipio" to municipio,
            "sobreMi" to sobreMi,
            "fotoUrl" to fotoUrl,
            "tarifa" to tarifa,
            "verificado" to verificado,
            "emailVerificado" to emailVerificado,
            "numeroClientes" to numeroClientes,
            "clientesActivos" to clientesActivos,
            "fechaRegistro" to fechaRegistro
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
                tarifa = (map["tarifa"] as? Number)?.toDouble() ?: 0.0,
                verificado = map["verificado"] as? Boolean ?: false,
                emailVerificado = map["emailVerificado"] as? Boolean ?: false,
                numeroClientes = (map["numeroClientes"] as? Number)?.toInt() ?: 0,
                clientesActivos = (map["clientesActivos"] as? List<String>) ?: emptyList(),
                fechaRegistro = (map["fechaRegistro"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}