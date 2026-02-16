package com.example.gimnasiopro.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para ejercicios en Firestore.
 *
 * NUEVA ESTRUCTURA JERÁRQUICA:
 * gruposMusculares/
 *   ├── Pectorales/
 *   │     ├── nombre: "Pectorales"
 *   │     ├── orden: 1
 *   │     └── ejercicios/ (subcolección)
 *   │           ├── ejercicio1 {nombre, descripcion, creadoPor...}
 *   │           └── ejercicio2 {...}
 *   ├── Espalda/
 *   │     └── ejercicios/
 *   └── ...
 */
class EjercicioFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val gruposCollection = firestore.collection("gruposMusculares")

    // Lista de grupos musculares con su orden
    private val gruposMusculares = listOf(
        "Pectorales" to 1,
        "Espalda" to 2,
        "Hombros" to 3,
        "Bíceps y Antebrazo" to 4,
        "Tríceps" to 5,
        "Abdominales" to 6,
        "Piernas" to 7,
        "Glúteos" to 8,
        "Gemelos" to 9
    )

    /**
     * Inicializar los grupos musculares en Firestore (solo una vez)
     */
    suspend fun inicializarGruposMusculares(): Result<Unit> {
        return try {
            gruposMusculares.forEach { (nombre, orden) ->
                val docRef = gruposCollection.document(nombre)
                val doc = docRef.get().await()
                if (!doc.exists()) {
                    docRef.set(mapOf(
                        "nombre" to nombre,
                        "orden" to orden
                    )).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener todos los grupos musculares
     */
    fun getAllGruposMusculares(): Flow<List<String>> = flow {
        val snapshot = gruposCollection
            .get()
            .await()
        
        val grupos = snapshot.documents
            .mapNotNull { doc ->
                val nombre = doc.getString("nombre")
                val orden = doc.getLong("orden")?.toInt() ?: 99
                if (nombre != null) Pair(nombre, orden) else null
            }
            .sortedBy { it.second }
            .map { it.first }

        emit(grupos)
    }

    /**
     * Obtener ejercicios por grupo muscular (desde subcolección)
     */
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<EjercicioFirestore>> = flow {
        val snapshot = gruposCollection
            .document(grupoMuscular)
            .collection("ejercicios")
            .get()
            .await()
        
        val ejercicios = snapshot.documents
            .mapNotNull { doc -> EjercicioFirestore.fromDocument(doc)?.copy(grupoMuscular = grupoMuscular) }
            .sortedBy { it.nombre }
        emit(ejercicios)
    }

    /**
     * Obtener todos los ejercicios (de todos los grupos)
     */
    fun getAllEjercicios(): Flow<List<EjercicioFirestore>> = flow {
        val todosEjercicios = mutableListOf<EjercicioFirestore>()

        gruposMusculares.forEach { (grupoNombre, _) ->
            val snapshot = gruposCollection
                .document(grupoNombre)
                .collection("ejercicios")
                .get()
                .await()

            val ejerciciosGrupo = snapshot.documents
                .mapNotNull { doc -> EjercicioFirestore.fromDocument(doc)?.copy(grupoMuscular = grupoNombre) }

            todosEjercicios.addAll(ejerciciosGrupo)
        }

        emit(todosEjercicios.sortedWith(compareBy({ it.grupoMuscular }, { it.nombre })))
    }

    /**
     * Obtener un ejercicio por ID y grupo muscular
     */
    suspend fun getEjercicioById(grupoMuscular: String, ejercicioId: String): EjercicioFirestore? {
        return try {
            val document = gruposCollection
                .document(grupoMuscular)
                .collection("ejercicios")
                .document(ejercicioId)
                .get()
                .await()
            EjercicioFirestore.fromDocument(document)?.copy(grupoMuscular = grupoMuscular)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Buscar ejercicios por nombre (en todos los grupos)
     */
    fun searchEjercicios(query: String): Flow<List<EjercicioFirestore>> = flow {
        val todosEjercicios = mutableListOf<EjercicioFirestore>()
        val queryLower = query.lowercase()

        gruposMusculares.forEach { (grupoNombre, _) ->
            val snapshot = gruposCollection
                .document(grupoNombre)
                .collection("ejercicios")
                .get()
                .await()

            val ejerciciosFiltrados = snapshot.documents
                .mapNotNull { doc -> EjercicioFirestore.fromDocument(doc)?.copy(grupoMuscular = grupoNombre) }
                .filter {
                    it.nombre.lowercase().contains(queryLower) ||
                    it.descripcion.lowercase().contains(queryLower)
                }

            todosEjercicios.addAll(ejerciciosFiltrados)
        }

        emit(todosEjercicios.sortedBy { it.nombre })
    }

    /**
     * Crear un nuevo ejercicio en el grupo muscular correspondiente.
     * IMPORTANTE: Verifica si ya existe un ejercicio con el mismo nombre antes de crearlo
     * para evitar duplicados.
     *
     * @param ejercicio El ejercicio a crear
     * @param forzarCreacion Si es true, no verifica duplicados (solo para trainers desde UI)
     * @return ID del ejercicio creado, o ID del existente si ya estaba
     */
    suspend fun crearEjercicio(ejercicio: EjercicioFirestore, forzarCreacion: Boolean = false): Result<String> {
        return try {
            // Asegurar que el grupo existe
            val grupoDoc = gruposCollection.document(ejercicio.grupoMuscular)
            val grupoExists = grupoDoc.get().await().exists()
            if (!grupoExists) {
                // Crear el grupo si no existe
                val orden = gruposMusculares.find { it.first == ejercicio.grupoMuscular }?.second ?: 99
                grupoDoc.set(mapOf(
                    "nombre" to ejercicio.grupoMuscular,
                    "orden" to orden
                )).await()
            }

            // Verificar si ya existe un ejercicio con el mismo nombre (evitar duplicados)
            if (!forzarCreacion) {
                val existentes = grupoDoc
                    .collection("ejercicios")
                    .whereEqualTo("nombre", ejercicio.nombre)
                    .get()
                    .await()

                if (!existentes.isEmpty) {
                    // Ya existe, devolver el ID del existente
                    val existenteId = existentes.documents.first().id
                    android.util.Log.d("EjercicioRepo", "Ejercicio '${ejercicio.nombre}' ya existe con ID: $existenteId")
                    return Result.success(existenteId)
                }
            }

            // Crear el ejercicio en la subcolección
            val docRef = grupoDoc
                .collection("ejercicios")
                .add(ejercicio.toMap())
                .await()

            // Actualizar el documento con su ID
            docRef.update("id", docRef.id).await()

            android.util.Log.d("EjercicioRepo", "Ejercicio '${ejercicio.nombre}' creado con ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar un ejercicio existente
     */
    suspend fun actualizarEjercicio(ejercicio: EjercicioFirestore): Result<Unit> {
        return try {
            if (ejercicio.id.isEmpty()) {
                return Result.failure(IllegalArgumentException("El ejercicio debe tener un ID"))
            }
            gruposCollection
                .document(ejercicio.grupoMuscular)
                .collection("ejercicios")
                .document(ejercicio.id)
                .update(ejercicio.toMap().filterValues { it != null } as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar un ejercicio
     */
    suspend fun eliminarEjercicio(grupoMuscular: String, ejercicioId: String): Result<Unit> {
        return try {
            gruposCollection
                .document(grupoMuscular)
                .collection("ejercicios")
                .document(ejercicioId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar ejercicios iniciales desde Room a la nueva estructura
     */
    suspend fun migrarEjerciciosIniciales(ejercicios: List<com.example.gimnasiopro.data.Ejercicio>): Result<Int> {
        return try {
            // Primero inicializar grupos
            inicializarGruposMusculares()

            var count = 0
            ejercicios.forEach { ejercicio ->
                val ejercicioFirestore = EjercicioFirestore(
                    id = "", // Se generará automáticamente
                    grupoMuscular = ejercicio.grupoMuscular,
                    nombre = ejercicio.nombre,
                    descripcion = ejercicio.descripcion,
                    imagenUrl = ejercicio.imagenUrl,
                    esPredefinido = true,
                    fechaCreacion = java.util.Date()
                )

                // Verificar si ya existe (por nombre)
                val existentes = gruposCollection
                    .document(ejercicio.grupoMuscular)
                    .collection("ejercicios")
                    .whereEqualTo("nombre", ejercicio.nombre)
                    .get()
                    .await()

                if (existentes.isEmpty) {
                    crearEjercicio(ejercicioFirestore)
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Migrar ejercicios desde la colección antigua 'ejercicios' (plana)
     * a la nueva estructura jerárquica 'gruposMusculares/[grupo]/ejercicios'
     *
     * Esta función lee todos los ejercicios de la colección antigua y los
     * copia a las subcolecciones correspondientes.
     */
    suspend fun migrarDesdeColeccionAntigua(): Result<Int> {
        return try {
            // Primero inicializar los grupos musculares
            inicializarGruposMusculares()

            // Leer todos los ejercicios de la colección antigua
            val coleccionAntigua = firestore.collection("ejercicios")
            val snapshot = coleccionAntigua.get().await()

            var count = 0
            var errores = 0

            snapshot.documents.forEach { doc ->
                try {
                    val grupoMuscular = doc.getString("grupoMuscular") ?: return@forEach
                    val nombre = doc.getString("nombre") ?: return@forEach
                    val descripcion = doc.getString("descripcion") ?: ""
                    val imagenUrl = doc.getString("imagenUrl")
                    val creadoPor = doc.getString("creadoPor")
                    val esPredefinido = doc.getBoolean("esPredefinido") ?: true

                    // Verificar si ya existe en la nueva estructura
                    val existentes = gruposCollection
                        .document(grupoMuscular)
                        .collection("ejercicios")
                        .whereEqualTo("nombre", nombre)
                        .get()
                        .await()

                    if (existentes.isEmpty) {
                        // Crear en la nueva estructura
                        val ejercicioNuevo = EjercicioFirestore(
                            id = "",
                            grupoMuscular = grupoMuscular,
                            nombre = nombre,
                            descripcion = descripcion,
                            imagenUrl = imagenUrl,
                            creadoPor = creadoPor,
                            esPredefinido = esPredefinido,
                            fechaCreacion = java.util.Date()
                        )
                        crearEjercicio(ejercicioNuevo)
                        count++
                    }
                } catch (e: Exception) {
                    errores++
                }
            }

            android.util.Log.d("EjercicioRepo", "Migración completada: $count ejercicios migrados, $errores errores")
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar ejercicios duplicados en la nueva estructura.
     * Mantiene solo el primero de cada nombre (case-insensitive) por grupo muscular.
     *
     * @return Número de duplicados eliminados
     */
    suspend fun eliminarDuplicados(): Result<Int> {
        return try {
            var totalEliminados = 0

            android.util.Log.d("EjercicioRepo", "🧹 Iniciando limpieza de duplicados...")

            gruposMusculares.forEach { (grupoNombre, _) ->
                try {
                    val snapshot = gruposCollection
                        .document(grupoNombre)
                        .collection("ejercicios")
                        .get()
                        .await()

                    android.util.Log.d("EjercicioRepo", "Procesando grupo: $grupoNombre (${snapshot.size()} ejercicios)")

                    // Agrupar ejercicios por nombre normalizado
                    val ejerciciosPorNombre = mutableMapOf<String, MutableList<com.google.firebase.firestore.DocumentSnapshot>>()

                    snapshot.documents.forEach { doc ->
                        val nombre = doc.getString("nombre")?.lowercase()?.trim() ?: return@forEach
                        if (ejerciciosPorNombre[nombre] == null) {
                            ejerciciosPorNombre[nombre] = mutableListOf()
                        }
                        ejerciciosPorNombre[nombre]!!.add(doc)
                    }

                    // Eliminar duplicados (mantener solo el primero)
                    ejerciciosPorNombre.forEach { (nombre, docs) ->
                        if (docs.size > 1) {
                            android.util.Log.d("EjercicioRepo", "⚠️ Encontrados ${docs.size} duplicados de '$nombre' en $grupoNombre - eliminando ${docs.size - 1}")
                            // Eliminar todos excepto el primero
                            docs.drop(1).forEach { doc ->
                                try {
                                    doc.reference.delete().await()
                                    totalEliminados++
                                    android.util.Log.d("EjercicioRepo", "🗑️ Eliminado duplicado: $nombre (ID: ${doc.id})")
                                } catch (e: Exception) {
                                    android.util.Log.e("EjercicioRepo", "Error eliminando ${doc.id}: ${e.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EjercicioRepo", "Error procesando grupo $grupoNombre: ${e.message}")
                }
            }

            android.util.Log.d("EjercicioRepo", "✅ Limpieza completada: $totalEliminados ejercicios duplicados eliminados")
            Result.success(totalEliminados)
        } catch (e: Exception) {
            android.util.Log.e("EjercicioRepo", "❌ Error eliminando duplicados: ${e.message}")
            Result.failure(e)
        }
    }
}
