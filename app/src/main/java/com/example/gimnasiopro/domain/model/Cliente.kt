package com.example.gimnasiopro.domain.model

/**
 * Modelo del cliente.
 * HEREDA de User (datos comunes) + datos específicos de cliente.
 *
 * Los clientes:
 * - Reciben rutinas de trainers
 * - Pueden contactar a trainers
 * - Visualizan sus ejercicios asignados
 */
data class Cliente(
    override val userId: String = "",
    override val email: String = "",
    override val nombre: String = "",
    override val telefono: String = "",
    override val emailVerificado: Boolean = false,
    override val fechaRegistro: Long = System.currentTimeMillis(),

    // DATOS ESPECÍFICOS DE CLIENTE
    val peso: Double = 0.0,            // Peso en kg
    val altura: Double = 0.0,          // Altura en cm
    val edad: Int = 0,                 // Edad
    val genero: String = "",           // "masculino", "femenino", "otro"
    val objetivo: String = "",         // "Pérdida de peso", "Ganancia muscular", etc.
    val nivel: String = "",            // "Principiante", "Intermedio", "Avanzado"
    val fotoUrl: String = "",          // URL de la foto de perfil
    val trainerId: String? = null,     // ID del trainer asignado (opcional)
    val activo: Boolean = true,        // Si está activo o dado de baja

    // PREFERENCIAS DE CONEXIÓN
    val aceptaSolicitudes: Boolean = true,  // Si acepta solicitudes de trainers
    val gruposIds: List<String> = emptyList() // IDs de grupos de entrenamiento
) : User(
    userId = userId,
    email = email,
    nombre = nombre,
    telefono = telefono,
    tipo = "cliente",  // SIEMPRE cliente
    emailVerificado = emailVerificado,
    fechaRegistro = fechaRegistro
) {
    /**
     * Convertir a Map para guardar en clientes/ (colección específica)
     */
    fun toClienteMap(): Map<String, Any> {
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
            "peso" to peso,
            "altura" to altura,
            "edad" to edad,
            "genero" to genero,
            "objetivo" to objetivo,
            "nivel" to nivel,
            "fotoUrl" to fotoUrl,
            "trainerId" to (trainerId ?: ""),
            "activo" to activo,
            "emailVerificado" to emailVerificado,
            "fechaRegistro" to fechaRegistro,
            "aceptaSolicitudes" to aceptaSolicitudes,
            "gruposIds" to gruposIds
        )
    }

    companion object {
        /**
         * Crear Cliente desde Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): Cliente {
            return Cliente(
                userId = map["userId"] as? String ?: "",
                email = map["email"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                telefono = map["telefono"] as? String ?: "",
                peso = (map["peso"] as? Number)?.toDouble() ?: 0.0,
                altura = (map["altura"] as? Number)?.toDouble() ?: 0.0,
                edad = (map["edad"] as? Number)?.toInt() ?: 0,
                genero = map["genero"] as? String ?: "",
                objetivo = map["objetivo"] as? String ?: "",
                nivel = map["nivel"] as? String ?: "",
                fotoUrl = map["fotoUrl"] as? String ?: "",
                trainerId = map["trainerId"] as? String,
                activo = map["activo"] as? Boolean ?: true,
                emailVerificado = map["emailVerificado"] as? Boolean ?: false,
                fechaRegistro = (map["fechaRegistro"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                aceptaSolicitudes = map["aceptaSolicitudes"] as? Boolean ?: true,
                gruposIds = @Suppress("UNCHECKED_CAST") (map["gruposIds"] as? List<String>) ?: emptyList()
            )
        }
    }
}
