package com.example.gimnasiopro.data.firestore

import android.util.Log
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

    private const val TAG = "UserHelper"
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
        Log.d(TAG, "🔍 Buscando usuario: $userId")

        // Buscar en clientes primero
        try {
            val clienteDoc = firestore.collection("clientes").document(userId).get().await()
            if (clienteDoc.exists()) {
                Log.d(TAG, "✅ Usuario $userId encontrado como CLIENTE")
                return UserInfo(
                    userId = userId,
                    tipo = "cliente",
                    nombre = clienteDoc.getString("nombre"),
                    email = clienteDoc.getString("email"),
                    documento = clienteDoc
                )
            } else {
                Log.d(TAG, "📄 Documento clientes/$userId NO existe")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando en clientes: ${e.message}")
        }

        // Si no está en clientes, buscar en trainers
        try {
            val trainerDoc = firestore.collection("trainers").document(userId).get().await()
            if (trainerDoc.exists()) {
                Log.d(TAG, "✅ Usuario $userId encontrado como TRAINER")
                return UserInfo(
                    userId = userId,
                    tipo = "trainer",
                    nombre = trainerDoc.getString("nombre"),
                    email = trainerDoc.getString("email"),
                    documento = trainerDoc
                )
            } else {
                Log.d(TAG, "📄 Documento trainers/$userId NO existe")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando en trainers: ${e.message}")
        }

        Log.w(TAG, "⚠️ Usuario $userId NO encontrado en ninguna colección")
        return null
    }

    /**
     * Obtiene el tipo de usuario (cliente o trainer).
     * Si el usuario no existe en ninguna colección, intenta crear el documento de cliente.
     */
    suspend fun getTipoUsuario(userId: String): String {
        val userInfo = getUserInfo(userId)
        if (userInfo != null) {
            Log.d(TAG, "📋 Tipo de usuario $userId: ${userInfo.tipo}")
            return userInfo.tipo
        }

        // Usuario no encontrado - intentar crear documento de cliente
        Log.w(TAG, "⚠️ Usuario $userId no encontrado, intentando crear documento de cliente...")

        val created = crearDocumentoClienteBasico(userId)
        if (created) {
            Log.d(TAG, "✅ Documento de cliente creado para $userId")
            return "cliente"
        }

        Log.e(TAG, "❌ No se pudo determinar ni crear tipo de usuario para $userId")
        return "cliente" // Default para que no falle completamente
    }

    /**
     * Crea un documento básico de cliente si no existe.
     * Esto asegura que las subcolecciones (rutinas, estadisticas) puedan crearse.
     */
    private suspend fun crearDocumentoClienteBasico(userId: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            val email = currentUser?.email ?: ""

            val datosBasicos = mapOf(
                "userId" to userId,
                "email" to email,
                "tipo" to "cliente",
                "activo" to true,
                "fechaCreacion" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("clientes")
                .document(userId)
                .set(datosBasicos, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Documento básico de cliente creado: clientes/$userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando documento de cliente: ${e.message}")
            false
        }
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

