# Migración a Firebase Firestore - Estructura Implementada

## 📁 Estructura de Datos en Firestore

### Opción B: Estructura Anidada (Implementada)

```
Firestore/
├── ejercicios/                    # Colección global (todos los usuarios)
│   └── {ejercicioId}
│       ├── id: String
│       ├── grupoMuscular: String
│       ├── nombre: String
│       ├── descripcion: String
│       ├── imagenUrl: String?
│       ├── creadoPor: String?
│       ├── esPredefinido: Boolean
│       └── fechaCreacion: Timestamp
│
└── users/                         # Por usuario
    └── {userId}/
        ├── rutinas/               # Subcolección
        │   └── {rutinaId}
        │       ├── nombre: String
        │       ├── ejercicioIds: List<String>
        │       ├── fechaCreacion: Timestamp
        │       ├── fechaModificacion: Timestamp
        │       └── creadoPor: String
        │
        ├── calendario/            # Subcolección
        │   └── {diaSemana} (1-7)
        │       ├── diaSemana: Int
        │       ├── rutinaId: String?
        │       ├── activo: Boolean
        │       └── fechaModificacion: Timestamp
        │
        ├── entrenamientos/        # Subcolección
        │   └── {entrenamientoId}
        │       ├── rutinaId: String?
        │       ├── fechaEntrenamiento: Timestamp
        │       ├── ejercicios: List<EjercicioEntrenamiento>
        │       ├── tiempoEntrenamientoMs: Long
        │       └── volumenTotal: Float
        │
        └── estadisticas/          # Subcolección
            └── {fecha} (YYYY-MM-DD)
                ├── fecha: String
                ├── fechaTimestamp: Timestamp
                ├── anio: Int
                ├── mes: Int
                ├── dia: Int
                ├── tiempoEntrenamientoMs: Long
                ├── numeroEntrenamientos: Int
                ├── ejerciciosCompletados: Int
                ├── volumenTotal: Float
                └── rutinasUsadas: List<String>
```

## 📦 Repositorios Implementados

### 1. EjercicioFirestoreRepository (Global)
- `getAllEjercicios()`: Obtener todos los ejercicios
- `getEjerciciosByGrupoMuscular()`: Filtrar por grupo muscular
- `getEjercicioById()`: Obtener ejercicio específico
- `searchEjercicios()`: Buscar por nombre
- `getAllGruposMusculares()`: Obtener grupos únicos
- `crearEjercicio()`: Crear nuevo ejercicio
- `actualizarEjercicio()`: Actualizar ejercicio
- `eliminarEjercicio()`: Eliminar ejercicio
- `migrarEjerciciosIniciales()`: Migrar desde Room

### 2. RutinaFirestoreRepository (Por usuario)
- `getAllRutinas()`: Obtener todas las rutinas del usuario
- `getRutinaById()`: Obtener rutina específica
- `crearRutina()`: Crear nueva rutina
- `actualizarRutina()`: Actualizar rutina
- `actualizarEjerciciosDeRutina()`: Actualizar solo ejercicios
- `agregarEjerciciosARutina()`: Añadir ejercicios a rutina existente
- `eliminarRutina()`: Eliminar rutina

### 3. CalendarioFirestoreRepository (Por usuario)
- `getCalendario()`: Obtener todo el calendario
- `getRutinaPorDia()`: Obtener rutina de un día
- `asignarRutinaADia()`: Asignar rutina a día
- `eliminarRutinaDeDia()`: Eliminar asignación
- `setDiaActivo()`: Activar/desactivar día

### 4. EntrenamientoFirestoreRepository (Por usuario)
- `getAllEntrenamientos()`: Obtener todos los entrenamientos
- `getEntrenamientosPorFecha()`: Filtrar por rango de fechas
- `getEntrenamientoById()`: Obtener entrenamiento específico
- `guardarEntrenamiento()`: Guardar nuevo entrenamiento
- `actualizarEntrenamiento()`: Actualizar entrenamiento
- `eliminarEntrenamiento()`: Eliminar entrenamiento
- `getUltimoEntrenamiento()`: Obtener el último entrenamiento

### 5. EstadisticaFirestoreRepository (Por usuario)
- `getEstadisticaPorFecha()`: Obtener estadística de un día
- `getEstadisticasPorMes()`: Obtener estadísticas de un mes
- `getEstadisticasPorAnio()`: Obtener estadísticas de un año
- `getEstadisticasPorRango()`: Obtener por rango de fechas
- `guardarEstadistica()`: Guardar estadística
- `agregarEntrenamientoAEstadistica()`: Actualizar estadística con entrenamiento

## 🔄 Migración desde Room

### FirestoreMigrationHelper

El helper proporciona métodos para migrar datos desde Room:

```kotlin
val migrationHelper = FirestoreMigrationHelper(
    ejercicioRepository,
    rutinaRepository,
    registroEntrenamientoRepository,
    rutinaDiaSemanaRepository,
    estadisticaRepository
)

// Migrar ejercicios (solo una vez, global)
migrationHelper.migrarEjercicios()

// Migrar datos de un usuario
val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
migrationHelper.migrarTodosLosDatosUsuario(userId)
```

## 📝 Ejemplo de Uso

### Obtener ejercicios por grupo muscular

```kotlin
val ejercicioRepo = EjercicioFirestoreRepository()
ejercicioRepo.getEjerciciosByGrupoMuscular("Pectorales")
    .collect { ejercicios ->
        // Usar ejercicios
    }
```

### Crear una rutina para un usuario

```kotlin
val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
val rutinaRepo = RutinaFirestoreRepository(userId)

val nuevaRutina = RutinaFirestore(
    rutinaId = "",
    nombre = "Rutina Push",
    ejercicioIds = listOf("ejercicio1", "ejercicio2", "ejercicio3"),
    fechaCreacion = Date(),
    fechaModificacion = Date(),
    creadoPor = userId
)

rutinaRepo.crearRutina(nuevaRutina).fold(
    onSuccess = { rutinaId ->
        // Rutina creada con éxito
    },
    onFailure = { error ->
        // Manejar error
    }
)
```

### Guardar un entrenamiento

```kotlin
val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
val entrenamientoRepo = EntrenamientoFirestoreRepository(userId)
val estadisticaRepo = EstadisticaFirestoreRepository(userId)

val entrenamiento = EntrenamientoFirestore(
    entrenamientoId = "",
    rutinaId = "rutina123",
    fechaEntrenamiento = Date(),
    ejercicios = listOf(
        EjercicioEntrenamientoFirestore(
            ejercicioId = "ejercicio1",
            series = listOf(
                SerieFirestore(pesoKg = 80f, repeticiones = 10, completado = true),
                SerieFirestore(pesoKg = 80f, repeticiones = 10, completado = true)
            ),
            completado = true
        )
    ),
    tiempoEntrenamientoMs = 3600000, // 1 hora
    volumenTotal = 1600f
)

entrenamientoRepo.guardarEntrenamiento(entrenamiento).fold(
    onSuccess = { entrenamientoId ->
        // Actualizar estadísticas
        estadisticaRepo.agregarEntrenamientoAEstadistica(
            fecha = Date(),
            tiempoMs = 3600000,
            ejerciciosCompletados = 1,
            volumenTotal = 1600f,
            rutinaId = "rutina123"
        )
    },
    onFailure = { error ->
        // Manejar error
    }
)
```

## 🔒 Reglas de Seguridad Firestore (Recomendadas)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Ejercicios globales - lectura para todos, escritura solo para autenticados
    match /ejercicios/{ejercicioId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      (resource == null || resource.data.creadoPor == request.auth.uid);
    }
    
    // Datos del usuario - solo el propio usuario puede acceder
    match /users/{userId} {
      allow read, write: if request.auth != null && userId == request.auth.uid;
      
      match /rutinas/{rutinaId} {
        allow read, write: if request.auth != null && userId == request.auth.uid;
      }
      
      match /calendario/{diaSemana} {
        allow read, write: if request.auth != null && userId == request.auth.uid;
      }
      
      match /entrenamientos/{entrenamientoId} {
        allow read, write: if request.auth != null && userId == request.auth.uid;
      }
      
      match /estadisticas/{fecha} {
        allow read, write: if request.auth != null && userId == request.auth.uid;
      }
    }
  }
}
```

## 📊 Índices Necesarios en Firestore

Crear estos índices en Firebase Console:

1. **ejercicios**:
   - `grupoMuscular` (Ascending), `nombre` (Ascending)

2. **users/{userId}/entrenamientos**:
   - `fechaEntrenamiento` (Descending)

3. **users/{userId}/estadisticas**:
   - `anio` (Ascending), `mes` (Ascending)
   - `fechaTimestamp` (Ascending)

## ⚠️ Notas Importantes

1. **IDs de Ejercicios**: En Firestore se usan `String` en lugar de `Long`
2. **Conversión de IDs**: Al migrar, convertir IDs numéricos a String
3. **Fechas**: Usar `Timestamp` de Firestore en lugar de `Long`
4. **Migración**: Ejecutar migración solo una vez por usuario
5. **Sincronización**: Considerar mantener Room como cache offline
