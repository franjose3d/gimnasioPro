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
        val telefono: String?,
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
                    telefono = clienteDoc.getString("telefono"),
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
                    telefono = trainerDoc.getString("telefono"),
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
            val telefono = currentUser?.phoneNumber ?: ""
            val nombre = currentUser?.displayName ?: ""

            val datosBasicos = mutableMapOf<String, Any>(
                "userId" to userId,
                "email" to email,
                "tipo" to "cliente",
                "activo" to true,
                "fechaCreacion" to com.google.firebase.Timestamp.now()
            )
            if (telefono.isNotEmpty()) {
                datosBasicos["telefono"] = telefono
                // Normalizar teléfono para búsquedas eficientes
                val telefonoNorm = telefono.replace(Regex("[^0-9]"), "").let { digitos ->
                    if (digitos.startsWith("34") && digitos.length > 9) digitos.removePrefix("34") else digitos
                }
                datosBasicos["telefonoNormalizado"] = telefonoNorm
            }
            if (nombre.isNotEmpty()) datosBasicos["nombre"] = nombre

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

    /**
     * Busca un usuario por email en clientes y trainers.
     * Devuelve el userId si lo encuentra, null si no.
     *
     * OPTIMIZADO: Usa solo queries indexadas (whereEqualTo + limit).
     * NO recorre todos los documentos → ahorra lecturas en capa gratuita.
     * El email se busca en formato lowercase (debe guardarse normalizado).
     */
    suspend fun getUserIdByEmail(email: String): String? {
        Log.d(TAG, "🔍 Buscando usuario por email: $email")

        val emailLimpio = email.trim().lowercase()
        if (emailLimpio.isEmpty()) {
            Log.w(TAG, "⚠️ Email vacío, no se puede buscar")
            return null
        }

        // Query directa con whereEqualTo (1 lectura por colección máx.)
        // Buscamos con email normalizado y con el formato original
        val variantes = setOf(emailLimpio, email.trim())

        for (coleccion in listOf("clientes", "trainers")) {
            for (emailVariante in variantes) {
                try {
                    val query = firestore.collection(coleccion)
                        .whereEqualTo("email", emailVariante)
                        .limit(1)
                        .get()
                        .await()

                    if (!query.isEmpty) {
                        val userId = query.documents.first().id
                        Log.d(TAG, "✅ Encontrado en $coleccion por email '$emailVariante': $userId")
                        return userId
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error query $coleccion email '$emailVariante': ${e.message}")
                }
            }
        }

        Log.w(TAG, "⚠️ Usuario con email '$emailLimpio' NO encontrado en ninguna colección")
        return null
    }

    /**
     * Busca un usuario por teléfono en clientes y trainers.
     * Devuelve el userId si lo encuentra, null si no.
     *
     * OPTIMIZADO: Usa queries indexadas (whereEqualTo + limit).
     * NO recorre todos los documentos → ahorra lecturas en capa gratuita.
     * Busca por campo 'telefonoNormalizado' (solo dígitos sin prefijo)
     * y por campo 'telefono' con variantes comunes.
     */
    suspend fun getUserIdByPhone(telefono: String): String? {
        Log.d(TAG, "🔍 Buscando usuario por teléfono: '$telefono'")

        if (telefono.isBlank()) {
            Log.w(TAG, "⚠️ Teléfono vacío, no se puede buscar")
            return null
        }

        // Extraer solo dígitos para normalizar
        val soloDigitos = telefono.replace(Regex("[^0-9]"), "")

        // Determinar el número sin prefijo de país
        val sinPrefijo = if (soloDigitos.startsWith("34") && soloDigitos.length > 9) {
            soloDigitos.removePrefix("34")
        } else {
            soloDigitos
        }
        Log.d(TAG, "🔍 Teléfono normalizado (sin prefijo): '$sinPrefijo'")

        // Generar variantes mínimas (máx 4-5 para no gastar lecturas)
        val variantes = mutableSetOf<String>()
        variantes.add(sinPrefijo)                    // Solo dígitos sin prefijo
        variantes.add("+34$sinPrefijo")              // Formato internacional
        variantes.add("34$sinPrefijo")               // Con prefijo sin +
        variantes.add(telefono.trim())               // Original

        // Buscar primero por campo normalizado (1 lectura por colección)
        for (coleccion in listOf("clientes", "trainers")) {
            try {
                val query = firestore.collection(coleccion)
                    .whereEqualTo("telefonoNormalizado", sinPrefijo)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    val userId = query.documents.first().id
                    Log.d(TAG, "✅ Encontrado en $coleccion por telefonoNormalizado '$sinPrefijo': $userId")
                    return userId
                }
            } catch (e: Exception) {
                Log.d(TAG, "⚠️ Campo telefonoNormalizado no disponible en $coleccion, buscando por variantes")
            }
        }

        // Buscar por variantes del campo 'telefono' (respaldo)
        for (variante in variantes) {
            for (coleccion in listOf("clientes", "trainers")) {
                try {
                    val query = firestore.collection(coleccion)
                        .whereEqualTo("telefono", variante)
                        .limit(1)
                        .get()
                        .await()

                    if (!query.isEmpty) {
                        val userId = query.documents.first().id
                        Log.d(TAG, "✅ Encontrado en $coleccion por tel '$variante': $userId")
                        return userId
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error query $coleccion tel '$variante': ${e.message}")
                }
            }
        }


        Log.w(TAG, "⚠️ Usuario con teléfono '$telefono' NO encontrado (sinPrefijo='$sinPrefijo')")
        return null
    }

    /**
     * Busca un usuario por email O teléfono.
     * Intenta primero por email si contiene @, sino por teléfono.
     * Devuelve el userId si lo encuentra, null si no.
     */
    suspend fun getUserIdByEmailOrPhone(valor: String): String? {
        val valorLimpio = valor.trim()

        return if (valorLimpio.contains("@")) {
            // Es un email
            getUserIdByEmail(valorLimpio)
        } else {
            // Es un teléfono
            getUserIdByPhone(valorLimpio)
        }
    }

    /**
     * Obtiene lista de trainers disponibles (activos y que aceptan solicitudes).
     *
     * OPTIMIZADO: Usa query con whereEqualTo para filtrar en Firestore
     * en lugar de descargar todos y filtrar en código.
     * Si no hay campo 'activo', Firestore los excluye, así que hacemos
     * una sola query sin filtro y filtramos localmente solo los pocos resultados.
     */
    suspend fun getTrainersDisponibles(): List<UserInfo> {
        Log.d(TAG, "🔍 Buscando trainers disponibles...")

        return try {
            // Query con filtro: solo trainers activos
            // Nota: trainers sin campo 'activo' no aparecerán,
            // pero es mejor que descargar toda la colección
            val trainersQuery = firestore.collection("trainers")
                .whereEqualTo("activo", true)
                .get()
                .await()

            val trainers = trainersQuery.documents.mapNotNull { doc ->

                // Verificar si acepta solicitudes (por defecto sí)
                val aceptaSolicitudes = doc.getBoolean("aceptaSolicitudes") ?: true
                if (!aceptaSolicitudes) return@mapNotNull null

                UserInfo(
                    userId = doc.id,
                    tipo = "trainer",
                    nombre = doc.getString("nombre"),
                    email = doc.getString("email"),
                    telefono = doc.getString("telefono"),
                    documento = doc
                )
            }

            Log.d(TAG, "✅ Encontrados ${trainers.size} trainers disponibles")
            trainers
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando trainers: ${e.message}")
            emptyList()
        }
    }

    /**
     * Actualiza la preferencia de aceptar solicitudes de un trainer.
     */
    suspend fun actualizarAceptaSolicitudes(trainerId: String, acepta: Boolean): Boolean {
        return try {
            firestore.collection("trainers")
                .document(trainerId)
                .update("aceptaSolicitudes", acepta)
                .await()
            Log.d(TAG, "✅ Preferencia aceptaSolicitudes actualizada: $acepta")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando preferencia: ${e.message}")
            false
        }
    }
}
