# GimnasioPro — Guía de Desarrollo

## 📋 Descripción del Proyecto
Aplicación Android para **guardar y visualizar rutinas de gimnasio y ejercicios**, orientada a uso personal en gimnasio.  
Desarrollo incremental con arquitectura limpia, persistencia con Room Database y funcionalidad completa de gestión de ejercicios personalizados.

---

## 🏗️ Arquitectura y Principios

### Clean Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION                             │
│      Activities / Adapters / ViewModels / DI Manual        │
├─────────────────────────────────────────────────────────────┤
│                       DOMAIN                                │
│            Use Cases / Repository Interfaces                │
├─────────────────────────────────────────────────────────────┤
│                        DATA                                 │
│       Room Database / DTOs / Repository Implementations     │
└─────────────────────────────────────────────────────────────┘
```

### Principios SOLID
| Principio | Aplicación en el proyecto |
|-----------|---------------------------|
| **S** - Single Responsibility | Cada UseCase tiene una única responsabilidad |
| **O** - Open/Closed | Entidades abiertas a extensión mediante herencia |
| **L** - Liskov Substitution | Interfaces de Repository intercambiables |
| **I** - Interface Segregation | Interfaces específicas por funcionalidad |
| **D** - Dependency Inversion | Domain define contratos; Data los implementa |

### TDD (Test-Driven Development)
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    RED      │ ──▶ │   GREEN     │ ──▶ │  REFACTOR   │
│ Escribir    │     │ Código      │     │ Mejorar     │
│ test que    │     │ mínimo para │     │ sin romper  │
│ falle       │     │ pasar test  │     │ tests       │
└─────────────┘     └─────────────┘     └─────────────┘
        ▲                                      │
        └──────────────────────────────────────┘
```

---

## 📁 Estructura de Carpetas

```
app/src/main/
├── java/com/example/gimnasiopro/
│   ├── data/                            # Capa de datos
│   │   ├── Ejercicio.kt                 # Entidad Room con @Entity
│   │   ├── EjercicioDao.kt              # DAO de Room
│   │   ├── EjercicioRepository.kt       # Implementación del repositorio
│   │   ├── EjerciciosIniciales.kt       # 160 ejercicios predefinidos
│   │   ├── DatabaseInitializer.kt       # Inicialización de BD
│   │   ├── RutinaEntity.kt              # Entidad de rutinas
│   │   ├── RutinaDao.kt                 # DAO de rutinas
│   │   ├── RutinaRepository.kt          # Repositorio de rutinas
│   │   ├── EjercicioEntrenamiento.kt    # Modelo para entrenamiento activo
│   │   └── SerieEntrenamiento.kt        # Modelo de series
│   │
│   ├── domain/                          # Capa de dominio
│   │   ├── model/                       # Modelos de dominio
│   │   │   ├── Trainer.kt              # Modelo de entrenador
│   │   │   └── User.kt                 # Modelo de usuario
│   │   ├── repository/                  # Interfaces de repositorios
│   │   │   └── TrainerRepository.kt    # Contrato de repositorio
│   │   └── usecase/                     # Casos de uso
│   │       └── RegisterTrainerUseCase.kt
│   │
│   ├── presentation/                    # Capa de presentación
│   │   └── trainer/
│   │       └── RegisterTrainerViewModel.kt
│   │
│   ├── di/                              # Inyección manual de dependencias
│   │   └── TrainerModule.kt
│   │
│   ├── components/                      # Componentes UI reutilizables
│   │   └── EjercicioAdapter.kt         # RecyclerView adapter
│   │
│   ├── MainActivity.kt                  # Activity principal
│   ├── EjerciciosActivity.kt           # Selector de grupos musculares
│   ├── ListaEjerciciosActivity.kt      # Lista de ejercicios + CRUD
│   ├── RutinasActivity.kt              # Gestión de rutinas
│   ├── ProgresoActivity.kt             # Seguimiento de progreso
│   ├── PersonalTrainerActivity.kt      # Acceso a entrenadores
│   ├── RegisterTrainerActivity.kt      # Registro de entrenadores
│   └── GimnasioproApplication.kt       # Application class
│
├── res/
│   ├── layout/                          # XML Layouts
│   │   ├── activity_main.xml
│   │   ├── activity_ejercicios.xml
│   │   ├── activity_lista_ejercicios.xml  # ✅ CON BOTONES + Y -
│   │   ├── dialog_agregar_ejercicio.xml   # ✅ NUEVO
│   │   ├── item_ejercicio.xml
│   │   └── ...
│   ├── values/
│   │   ├── colors.xml                   # Tema verde oscuro
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── drawable/
│       └── btn_rounded_green.xml        # Botón redondeado verde
│
├── test/java/com/example/gimnasiopro/       # Tests unitarios
│   ├── domain/
│   │   └── usecase/
│   │       └── RegisterTrainerUseCaseTest.kt
│   └── presentation/
│       └── RegisterTrainerViewModelTest.kt
│
└── androidTest/java/                    # Tests instrumentados

```

### Decisión de Arquitectura: XML Layouts
Se usa **XML Layouts** en lugar de Jetpack Compose para:
- **Mejor separación de responsabilidades** (SOLID)
- **UI declarativa** en archivos XML separados
- **Lógica de negocio** en Activities/ViewModels
- **Compatibilidad** con herramientas de diseño visual

---

## 🎯 Hitos de Desarrollo

### ✅ Hito 0 — Configuración inicial
- [x] Crear proyecto Android
- [x] Configurar Room Database
- [x] Crear estructura de carpetas
- [x] Configurar colores y tema verde oscuro

### ✅ Hito 1 — Base de datos de ejercicios
- [x] Crear entidad `Ejercicio` con Room
- [x] Crear `EjercicioDao` con queries
- [x] Implementar `EjercicioRepository`
- [x] Cargar 160 ejercicios predefinidos desde `EjerciciosIniciales`
- [x] Organizados en 9 grupos musculares

### ✅ Hito 2 — UI de ejercicios
- [x] Pantalla de selección de grupos musculares (`EjerciciosActivity`)
- [x] Lista de ejercicios por grupo (`ListaEjerciciosActivity`)
- [x] Adaptador con checkboxes para selección múltiple
- [x] Navegación: MainActivity → Ejercicios → Grupo → Lista

### ✅ Hito 3 — Sistema de rutinas
- [x] Crear entidad `RutinaEntity` con Room
- [x] Crear `RutinaDao` y `RutinaRepository`
- [x] 10 rutinas predefinidas (Rutina 1 a Rutina 10)
- [x] Máximo 10 ejercicios por rutina
- [x] Selección y guardado de ejercicios en rutinas

### ✅ Hito 4 — CRUD de ejercicios personalizados ⭐ NUEVO
- [x] **Botón "+" para agregar ejercicios**
    - Campo nombre (obligatorio)
    - Campo descripción (obligatorio, máx 200 caracteres)
    - Guardar en grupo muscular actual
    - Validación client-side
- [x] **Botón "-" para eliminar ejercicios**
    - Lista de TODOS los ejercicios del grupo
    - Incluye predefinidos y personalizados
    - Confirmación antes de eliminar
    - Eliminación permanente
- [x] **Persistencia con Room**
    - Guardado automático en SQLite
    - Sobrevive al cierre de app
    - Actualización automática con Flow
- [x] **UI actualizada**
    - 2 botones grandes al final de la lista
    - Diálogo personalizado para agregar
    - Diálogo de selección para eliminar

### ✅ Hito 5 — Módulo de entrenadores (Clean Architecture)
- [x] Domain layer completo:
    - `Trainer.kt` model
    - `TrainerRepository.kt` interface
    - `RegisterTrainerUseCase.kt` con validaciones
- [x] Data layer:
    - `TrainerDTO.kt`
    - `FirebaseTrainerDataSource.kt`
    - `TrainerRepositoryImpl.kt`
- [x] Presentation layer:
    - `RegisterTrainerViewModel.kt` con LiveData
    - `RegisterTrainerActivity.kt` con validaciones
- [x] Tests unitarios (17 tests):
    - 7 tests en `RegisterTrainerUseCaseTest`
    - 4 tests en `TrainerRepositoryImplTest`
    - 6 tests en `RegisterTrainerViewModelTest`
- [x] DI manual con `TrainerModule`

### 🔲 Hito 6 — Mejoras pendientes
- [ ] Sistema de progreso/historial
- [ ] Exportar/importar rutinas
- [ ] Imágenes para ejercicios personalizados
- [ ] Editar ejercicios existentes (Update del CRUD)
- [ ] Búsqueda de ejercicios
- [ ] Filtros por grupo muscular
- [ ] Firebase Authentication para usuarios

---

## 📦 Dependencias Actuales

En `app/build.gradle.kts`:

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")

    // RecyclerView y CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Lifecycle y ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Firebase (para módulo de entrenadores)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Coil para cargar imágenes SVG
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

---

## 🎨 Tema y Colores

### Paleta de colores (`colors.xml`)
```xml
<color name="dark_green_background">#0A1F1A</color>
<color name="darker_green_header">#071712</color>
<color name="electric_green_button">#00FF88</color>
<color name="text_light_gray">#B0B0B0</color>
<color name="text_on_button">#000000</color>
<color name="card_background">#1A2F2A</color>
```

### Estilo visual
- Fondo verde oscuro
- Botones verde eléctrico redondeados
- Texto gris claro para descripciones
- Texto negro para botones

---

## 📊 Base de Datos (Room)

### Tablas principales

#### **ejercicios**
```kotlin
@Entity(tableName = "ejercicios")
data class Ejercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grupoMuscular: String,  // "Pectorales", "Espalda", etc.
    val nombre: String,
    val descripcion: String,
    val imagenUrl: String? = null
)
```

**Datos iniciales:**
- 160 ejercicios predefinidos
- Organizados en 9 grupos musculares:
    - Pectorales (14 ejercicios)
    - Espalda (26 ejercicios)
    - Hombros (21 ejercicios)
    - Bíceps y Antebrazo (16 ejercicios)
    - Tríceps (15 ejercicios)
    - Abdominales (12 ejercicios)
    - Piernas (54 ejercicios)
    - Glúteos (28 ejercicios)
    - Gemelos (2 ejercicios)

#### **rutinas**
```kotlin
@Entity(tableName = "rutinas")
data class RutinaEntity(
    @PrimaryKey
    val numero: Int,              // 1 a 10
    val nombre: String,           // "Rutina 1", "Rutina 2", etc.
    val ejercicioIds: List<Int>   // Máximo 10 IDs
)
```

---

## 🧪 Testing

### Tests Unitarios Implementados

#### **RegisterTrainerUseCaseTest** (7 tests)
```kotlin
✅ register trainer with valid data should succeed
✅ register trainer with blank name should fail
✅ register trainer with invalid email should fail
✅ register trainer with blank certification should fail
✅ register trainer with negative experience should fail
✅ register trainer with negative rate should fail
✅ register trainer with blank biography should fail
```

#### **TrainerRepositoryImplTest** (4 tests)
```kotlin
✅ save trainer should call data source
✅ get trainer by id should return trainer when exists
✅ get trainer by id should return null when not exists
✅ get all trainers should return list
```

#### **RegisterTrainerViewModelTest** (6 tests)
```kotlin
✅ initial state should be idle
✅ register trainer with valid data should update to success
✅ register trainer with invalid data should update to error
✅ register trainer should show loading state
✅ error state should contain error message
✅ success state should contain success message
```

### Comandos de Test
```powershell
# Tests unitarios
.\gradlew.bat testDebugUnitTest

# Tests instrumentados
.\gradlew.bat connectedDebugAndroidTest

# Test específico
.\gradlew.bat testDebugUnitTest --tests "*.RegisterTrainerUseCaseTest"
```

---

## 📝 Reglas del Proyecto

### ✅ HACER
1. **Escribir tests ANTES del código** (TDD cuando sea posible)
2. **Commits pequeños y descriptivos**
3. **Validaciones en múltiples capas** (client-side + use case)
4. **Usar Flow para datos reactivos**
5. **Separar responsabilidades** (Activity maneja UI, ViewModel maneja lógica)
6. **Documentar código complejo** con comentarios KDoc

### ❌ NO HACER
1. No poner lógica de negocio en Activities
2. No ignorar validaciones de entrada
3. No hacer operaciones de BD en el hilo principal
4. No hardcodear strings (usar `strings.xml`)
5. No mezclar responsabilidades en una clase

---

## 🔄 Funcionalidad CRUD de Ejercicios

### **CREATE (Agregar ejercicio)**
```kotlin
Ubicación: ListaEjerciciosActivity.kt
Trigger: Botón "+" al final de la lista

Flujo:
1. Usuario presiona "+"
2. Se abre dialog_agregar_ejercicio.xml
3. Usuario ingresa nombre y descripción (máx 200 chars)
4. Validaciones:
   - Nombre no vacío
   - Descripción no vacía
5. Se crea objeto Ejercicio:
   Ejercicio(
       grupoMuscular = grupoMuscularActual,
       nombre = nombre,
       descripcion = descripcion,
       imagenUrl = null
   )
6. ejercicioRepository.insertEjercicio(nuevoEjercicio)
7. La lista se actualiza automáticamente (Flow)
```

### **READ (Leer ejercicios)**
```kotlin
Ubicación: ListaEjerciciosActivity.kt

Flujo:
1. loadEjercicios(grupoMuscular)
2. ejercicioRepository.getEjerciciosByGrupoMuscular(grupoMuscular)
3. Flow emite lista cada vez que hay cambios
4. adapter.submitList(ejercicios)
5. RecyclerView se actualiza automáticamente
```

### **DELETE (Eliminar ejercicio)**
```kotlin
Ubicación: ListaEjerciciosActivity.kt
Trigger: Botón "-" al final de la lista

Flujo:
1. Usuario presiona "-"
2. Se cargan TODOS los ejercicios del grupo
3. Se muestra AlertDialog con lista de nombres
4. Usuario selecciona ejercicio
5. Se muestra confirmación: "¿Eliminar 'X'?"
6. Si confirma:
   - ejercicioRepository.deleteEjercicio(ejercicio)
   - La lista se actualiza automáticamente (Flow)
```

### **UPDATE (Pendiente)**
```kotlin
// TODO: Implementar en futuro hito
// - Diálogo similar a agregar pero pre-rellenado
// - ejercicioRepository.updateEjercicio(ejercicio)
```

---

## 🚀 Cómo Retomar el Desarrollo

1. **Leer este archivo** para recordar el contexto
2. **Ver la lista de hitos** y encontrar el siguiente pendiente
3. **Ejecutar los tests** para verificar el estado:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
4. **Revisar código reciente** en los archivos modificados
5. **Continuar con el siguiente feature** del hito actual
6. **Actualizar checkboxes** al completar tareas

---

## 📚 Ejemplos de Código

### Entidad Room
```kotlin
@Entity(tableName = "ejercicios")
data class Ejercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grupoMuscular: String,
    val nombre: String,
    val descripcion: String,
    val imagenUrl: String? = null
)
```

### DAO
```kotlin
@Dao
interface EjercicioDao {
    @Query("SELECT * FROM ejercicios WHERE grupoMuscular = :grupoMuscular")
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>>

    @Insert
    suspend fun insertEjercicio(ejercicio: Ejercicio): Long

    @Delete
    suspend fun deleteEjercicio(ejercicio: Ejercicio)
}
```

### Repository
```kotlin
class EjercicioRepository(private val ejercicioDao: EjercicioDao) {
    fun getEjerciciosByGrupoMuscular(grupoMuscular: String): Flow<List<Ejercicio>> {
        return ejercicioDao.getEjerciciosByGrupoMuscular(grupoMuscular)
    }

    suspend fun insertEjercicio(ejercicio: Ejercicio): Long {
        return ejercicioDao.insertEjercicio(ejercicio)
    }

    suspend fun deleteEjercicio(ejercicio: Ejercicio) {
        ejercicioDao.deleteEjercicio(ejercicio)
    }
}
```

### Activity con Flow
```kotlin
lifecycleScope.launch {
    ejercicioRepository.getEjerciciosByGrupoMuscular(grupoMuscular)
        .collectLatest { ejercicios ->
            adapter.submitList(ejercicios)
        }
}
```

---

## 📅 Historial de Cambios

| Fecha | Hito | Descripción |
|-------|------|-------------|
| 2026-01-31 | 0 | Creación del proyecto y configuración inicial |
| 2026-02-03 | 1-3 | Implementación de base de datos, ejercicios y rutinas |
| 2026-02-05 | 5 | Módulo de entrenadores con Clean Architecture y TDD |
| 2026-02-10 | 4 | **CRUD de ejercicios personalizados implementado** |
|  |  | - Botón "+" para agregar ejercicios con validaciones |
|  |  | - Botón "-" para eliminar con confirmación |
|  |  | - Persistencia completa con Room |
|  |  | - Actualización automática con Flow |
|  |  | - Límite de 200 caracteres en descripción |
|  |  | - Diálogo personalizado con tema verde |

---

## 🎯 Próximos Pasos Sugeridos

1. **Sistema de progreso**
    - Historial de entrenamientos
    - Gráficas de evolución
    - Registro de pesos y repeticiones

2. **Mejoras en ejercicios**
    - Implementar UPDATE (editar ejercicios)
    - Añadir imágenes personalizadas
    - Búsqueda global de ejercicios

3. **Mejoras en rutinas**
    - Renombrar rutinas personalizadas
    - Duplicar rutinas
    - Compartir rutinas

4. **Autenticación**
    - Firebase Authentication
    - Perfiles de usuario
    - Sincronización en la nube

---

> **💡 Consejo**: Este archivo es tu punto de entrada cada vez que retomes el proyecto.  
> Mantén actualizados los checkboxes y el historial de cambios.  
> El código sigue Clean Architecture, SOLID y TDD donde sea práctico.

## 🔗 Archivos Clave del Proyecto

### Core
- `GimnasioproApplication.kt` - Inicialización global
- `DatabaseInitializer.kt` - Setup de BD con datos iniciales

### Ejercicios
- `ListaEjerciciosActivity.kt` - **⭐ CRUD COMPLETO**
- `activity_lista_ejercicios.xml` - **⭐ BOTONES + Y -**
- `dialog_agregar_ejercicio.xml` - **⭐ NUEVO**
- `EjercicioAdapter.kt` - RecyclerView adapter
- `EjerciciosIniciales.kt` - 160 ejercicios predefinidos

### Entrenadores
- `RegisterTrainerActivity.kt` - Registro con validaciones
- `RegisterTrainerViewModel.kt` - LiveData + UiState
- `RegisterTrainerUseCase.kt` - Lógica de negocio

### Base de datos
- `EjercicioRepository.kt` - CRUD de ejercicios
- `RutinaRepository.kt` - Gestión de rutinas