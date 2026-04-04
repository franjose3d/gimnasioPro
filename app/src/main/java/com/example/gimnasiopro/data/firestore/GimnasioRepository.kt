package com.example.gimnasiopro.data.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GimnasioRepository {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    companion object { private const val TAG = "GimnasioRepository" }

    /** Busca un gimnasio activo por su codigoAcceso. Devuelve null si no existe. */
    suspend fun buscarPorCodigo(codigo: String): GimnasioFirestore? {
        val codigoLimpio = codigo.trim()
        if (codigoLimpio.isBlank()) return null
        return try {
            val query = firestore.collection("gimnasios")
                .whereEqualTo("codigoAcceso", codigoLimpio)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .await()
            if (query.isEmpty) null
            else {
                val doc = query.documents.first()
                doc.toGimnasio()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando gimnasio por código: ${e.message}")
            null
        }
    }

    /** Obtiene un gimnasio por su ID de documento. */
    suspend fun getGimnasio(gimnasioId: String): GimnasioFirestore? {
        if (gimnasioId.isBlank()) return null
        return try {
            val doc = firestore.collection("gimnasios").document(gimnasioId).get().await()
            if (!doc.exists()) null else doc.toGimnasio()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo gimnasio: ${e.message}")
            null
        }
    }

    /** Escribe gimnasioId en el documento del usuario autenticado (cliente o trainer). */
    suspend fun vincularGimnasio(gimnasioId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val userInfo = UserHelper.getCurrentUserInfo() ?: return false
            val coleccion = UserHelper.getColeccionUsuario(userInfo.tipo)
            firestore.collection(coleccion).document(userId)
                .update("gimnasioId", gimnasioId)
                .await()
            Log.d(TAG, "✅ Gimnasio vinculado: $gimnasioId → $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error vinculando gimnasio: ${e.message}")
            false
        }
    }

    /** Elimina el campo gimnasioId del documento del usuario autenticado. */
    suspend fun desvincularGimnasio(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val userInfo = UserHelper.getCurrentUserInfo() ?: return false
            val coleccion = UserHelper.getColeccionUsuario(userInfo.tipo)
            firestore.collection(coleccion).document(userId)
                .update("gimnasioId", FieldValue.delete())
                .await()
            Log.d(TAG, "✅ Gimnasio desvinculado → $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error desvinculando gimnasio: ${e.message}")
            false
        }
    }

    /** Carga el gimnasio vinculado al usuario actual, o null si no hay ninguno. */
    suspend fun getGimnasioActual(): GimnasioFirestore? {
        return try {
            val userInfo = UserHelper.getCurrentUserInfo() ?: return null
            val gimnasioId = userInfo.documento?.getString("gimnasioId") ?: return null
            getGimnasio(gimnasioId)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo gimnasio actual: ${e.message}")
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toGimnasio() = GimnasioFirestore(
        id        = id,
        nombre    = getString("nombre") ?: "",
        direccion = getString("direccion") ?: "",
        mapsQuery = getString("mapsQuery") ?: "",
        logoUrl   = getString("logoUrl") ?: "",
        webUrl    = getString("webUrl") ?: "",
        activo    = getBoolean("activo") ?: true
    )
}
