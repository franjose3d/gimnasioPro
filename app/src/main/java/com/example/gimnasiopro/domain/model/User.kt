package com.example.gimnasiopro.domain.model

/**
 * Clase base para TODOS los usuarios de la app.
 *
 * Tipos:
 * - Trainer: Entrenador personal
 * - Cliente: Usuario que recibe rutinas
 */
open class User(
    open val userId: String = "",
    open val email: String = "",
    open val nombre: String = "",
    open val telefono: String = "",
    open val tipo: String = "",           // "trainer" o "cliente"
    open val emailVerificado: Boolean = false,
    open val fechaRegistro: Long = System.currentTimeMillis()
) {
    /**
     * Convertir a Map para guardar en users/ (colección común)
     */
    fun toUserMap(): Map<String, Any> {
        return hashMapOf(
            "userId" to userId,
            "email" to email,
            "nombre" to nombre,
            "telefono" to telefono,
            "tipo" to tipo,
            "emailVerificado" to emailVerificado,
            "fechaRegistro" to fechaRegistro
        )
    }

    companion object {
        /**
         * Crear User desde Map de Firestore
         */
        fun fromMap(map: Map<String, Any>): User {
            return User(
                userId = map["userId"] as? String ?: "",
                email = map["email"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                telefono = map["telefono"] as? String ?: "",
                tipo = map["tipo"] as? String ?: "",
                emailVerificado = map["emailVerificado"] as? Boolean ?: false,
                fechaRegistro = (map["fechaRegistro"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

