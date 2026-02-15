package com.example.gimnasiopro.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper centralizado para manejar usuarios.
 *
 * ESTRUCTURA SIMPLIFICADA DE FIRESTORE:
 * - clientes/{userId} -> datos del cliente + subcolección "rutinas"
 * - trainers/{userId} -> datos del trainer + subcolección "rutinas"
 *
 * Ya no se necesita la colección "users" separada.
 */
object UserHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Resultado de búsqueda de usuario
     */
    data class UserInfo(
        val userId: String,
        val tipo: String, // "cliente" o "trainer"
        val nombre: String?,
        val email: String?,
        val documento: DocumentSnapshot?
    )

    /**
     * Obtiene la información del usuario actual.
     * Busca primero en clientes, luego en trainers.
     */
    suspend fun getCurrentUserInfo(): UserInfo? {
        val userId = auth.currentUser?.uid ?: return null
        return getUserInfo(userId)
    }

    /**
     * Obtiene la información de cualquier usuario por ID.
     * Busca primero en clientes, luego en trainers.
     */
    suspend fun getUserInfo(userId: String): UserInfo? {
        // Buscar en clientes primero
        try {
            val clienteDoc = firestore.collection("clientes").document(userId).get().await()
            if (clienteDoc.exists()) {
                return UserInfo(
                    userId = userId,
                    tipo = "cliente",
                    nombre = clienteDoc.getString("nombre"),
                    email = clienteDoc.getString("email"),
                    documento = clienteDoc
                )
            }
        } catch (_: Exception) { }

        // Si no está en clientes, buscar en trainers
        try {
            val trainerDoc = firestore.collection("trainers").document(userId).get().await()
            if (trainerDoc.exists()) {
                return UserInfo(
                    userId = userId,
                    tipo = "trainer",
                    nombre = trainerDoc.getString("nombre"),
                    email = trainerDoc.getString("email"),
                    documento = trainerDoc
                )
            }
        } catch (_: Exception) { }

        return null
    }

    /**
     * Obtiene el tipo de usuario (cliente o trainer).
     * Versión optimizada que no necesita la colección "users".
     */
    suspend fun getTipoUsuario(userId: String): String {
        return getUserInfo(userId)?.tipo ?: "cliente"
    }

    /**
     * Obtiene la colección correcta para un usuario.
     */
    fun getColeccionUsuario(tipoUsuario: String): String {
        return if (tipoUsuario == "trainer") "trainers" else "clientes"
    }

    /**
     * Obtiene la referencia al documento del usuario.
     */
    suspend fun getUserDocumentRef(userId: String): Pair<String, com.google.firebase.firestore.DocumentReference>? {
        val userInfo = getUserInfo(userId) ?: return null
        val coleccion = getColeccionUsuario(userInfo.tipo)
        return Pair(userInfo.tipo, firestore.collection(coleccion).document(userId))
    }
}

