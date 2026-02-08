# GimnasioPro — Guía de Desarrollo

## 📋 Descripción del Proyecto
Aplicación Android para **guardar y visualizar rutinas de gimnasio**, orientada a un gimnasio.  
Desarrollo incremental con posibilidad de pausar y retomar en cualquier momento.

---

## 🏗️ Arquitectura y Principios

### Arquitectura Hexagonal (Ports & Adapters)
```
┌─────────────────────────────────────────────────────────────┐
│                      ADAPTERS (UI)                          │
│         Activities / Fragments / ViewModels / DI            │
├─────────────────────────────────────────────────────────────┤
│                     APPLICATION                             │
│                  Casos de Uso (Use Cases)                   │
├─────────────────────────────────────────────────────────────┤
│                       DOMAIN                                │
│       Entidades / Value Objects / Repositorios (Interfaces) │
├─────────────────────────────────────────────────────────────┤
│                 ADAPTERS (Persistence)                      │
│              Room / In-Memory / API externa                 │
└─────────────────────────────────────────────────────────────┘
```

### Principios SOLID
| Principio | Aplicación en el proyecto |
|-----------|---------------------------|
| **S** - Single Responsibility | Cada clase tiene una única responsabilidad (1 UseCase = 1 acción) |
| **O** - Open/Closed | Entidades abiertas a extensión, cerradas a modificación |
| **L** - Liskov Substitution | Adapters intercambiables (InMemory ↔ Room) |
| **I** - Interface Segregation | Interfaces pequeñas y específicas en `domain` |
| **D** - Dependency Inversion | Domain define interfaces; Adapters las implementan |

### DDD (Domain-Driven Design)
- **Entidades**: objetos con identidad (`Routine`, `Exercise`)
- **Value Objects**: objetos inmutables sin identidad (`RoutineName`, `ExerciseSet`)
- **Agregados**: agrupaciones con raíz (`Routine` como raíz, `Exercise` como parte)
- **Repositorios**: contratos de persistencia definidos en el dominio

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
│   ├── domain/                          # Núcleo de negocio (sin dependencias Android)
│   │   ├── model/                       # Entidades y Value Objects
│   │   │   ├── Routine.kt
│   │   │   ├── Exercise.kt
│   │   │   └── ExerciseSet.kt
│   │   └── repository/                  # Interfaces (puertos)
│   │       └── RoutineRepository.kt
│   │
│   ├── application/                     # Casos de uso
│   │   ├── SaveRoutineUseCase.kt
│   │   ├── GetAllRoutinesUseCase.kt
│   │   ├── GetRoutineByIdUseCase.kt
│   │   └── DeleteRoutineUseCase.kt
│   │
│   └── adapters/
│       ├── persistence/                 # Implementaciones de repositorios
│       │   ├── InMemoryRoutineRepository.kt
│       │   └── room/
│       │       ├── RoutineDao.kt
│       │       ├── RoutineEntity.kt
│       │       ├── GymDatabase.kt
│       │       └── RoomRoutineRepository.kt
│       │
│       └── android/                     # UI y componentes Android
│           ├── di/                      # Inyección de dependencias
│           ├── viewmodel/
│           │   ├── RoutineListViewModel.kt
│           │   └── RoutineDetailViewModel.kt
│           └── activity/
│               └── MainActivity.kt
│
├── res/
│   ├── layout/                          # XML Layouts (Separación UI - SOLID)
│   │   ├── activity_main.xml
│   │   ├── activity_exercises.xml
│   │   ├── activity_routines.xml
│   │   └── item_routine.xml
│   ├── values/
│   │   ├── colors.xml                   # Colores del tema
│   │   ├── strings.xml                  # Textos externalizados
│   │   └── themes.xml
│   └── drawable/

app/src/test/java/com/example/gimnasiopro/       # Tests unitarios
├── domain/
├── application/
└── adapters/

app/src/androidTest/java/com/example/gimnasiopro/ # Tests instrumentados
```

### Decisión de Arquitectura: XML Layouts vs Jetpack Compose
Se usa **XML Layouts** en lugar de Jetpack Compose para:
- **Mejor separación de responsabilidades** (SOLID - Single Responsibility)
- **UI definida declarativamente** en archivos XML separados
- **Lógica de negocio** en Activities/Fragments/ViewModels
- **Facilita testing** al desacoplar vista de lógica

---

## 🎯 Hitos de Desarrollo

### ✅ Hito 0 — Configuración inicial
- [x] Crear proyecto Android
- [ ] Configurar dependencias de test (JUnit, MockK)
- [ ] Crear estructura de carpetas
- [ ] Verificar que los tests base pasan

### 🔲 Hito 1 — Dominio mínimo (TDD)
- [ ] Crear entidad `Routine` con tests
- [ ] Crear interfaz `RoutineRepository`
- [ ] Implementar `SaveRoutineUseCase` con tests
- [ ] Implementar `GetAllRoutinesUseCase` con tests

### 🔲 Hito 2 — Adapter In-Memory
- [ ] Implementar `InMemoryRoutineRepository`
- [ ] Tests de integración para el repositorio

### 🔲 Hito 3 — Persistencia con Room
- [ ] Configurar Room
- [ ] Crear `RoutineEntity` y `RoutineDao`
- [ ] Implementar `RoomRoutineRepository`
- [ ] Tests instrumentados de persistencia

### 🔲 Hito 4 — UI Lista de Rutinas
- [ ] Crear `RoutineListViewModel` con tests
- [ ] Pantalla de lista de rutinas (Compose)
- [ ] Navegación básica

### 🔲 Hito 5 — UI Crear/Editar Rutina
- [ ] Crear `RoutineDetailViewModel` con tests
- [ ] Pantalla de detalle/edición
- [ ] Validaciones de formulario

### 🔲 Hito 6 — Ejercicios dentro de rutinas
- [ ] Entidad `Exercise` y `ExerciseSet`
- [ ] Relación Routine → Exercise
- [ ] UI para agregar ejercicios

### 🔲 Hito 7 — Mejoras y pulido
- [ ] Búsqueda y filtros
- [ ] Export/Import de rutinas
- [ ] Temas y personalización

---

## 📦 Dependencias Requeridas

Añadir en `app/build.gradle.kts`:

```kotlin
dependencies {
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0") // Para testing de Flows

    // Room (Hito 3+)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt para DI (opcional, recomendado)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
```

---

## 🧪 Convenciones de Testing

### Nomenclatura
```kotlin
class SaveRoutineUseCaseTest {
    @Test
    fun `execute should save routine when name is valid`() { }

    @Test
    fun `execute should throw exception when name is blank`() { }
}
```

### Patrón AAA (Arrange-Act-Assert)
```kotlin
@Test
fun `execute should call repository save`() {
    // Arrange
    val routine = Routine(name = "Push Day")
    val repository = mockk<RoutineRepository>(relaxed = true)
    val useCase = SaveRoutineUseCase(repository)

    // Act
    useCase.execute(routine)

    // Assert
    verify { repository.save(routine) }
}
```

### Comandos de Test
```powershell
# Ejecutar tests unitarios
.\gradlew.bat testDebugUnitTest

# Ejecutar tests instrumentados (requiere emulador/dispositivo)
.\gradlew.bat connectedDebugAndroidTest

# Ejecutar un test específico
.\gradlew.bat testDebugUnitTest --tests "*.SaveRoutineUseCaseTest"
```

---

## 📝 Reglas del Proyecto

### ✅ HACER
1. **Escribir el test ANTES del código** (TDD)
2. **Un commit por test pasado** (commits pequeños y frecuentes)
3. **Interfaces en `domain`**, implementaciones en `adapters`
4. **Casos de uso con un único método** `execute()` o `invoke()`
5. **Validar con `get_errors`** después de cada cambio
6. **Mantener el dominio libre de dependencias Android**

### ❌ NO HACER
1. No poner lógica de negocio en ViewModels o Activities
2. No importar clases de Android en `domain/` o `application/`
3. No saltar el ciclo TDD (Red → Green → Refactor)
4. No hacer commits con tests fallando
5. No mezclar responsabilidades en una misma clase

---

## 🔄 Cómo Retomar el Desarrollo

1. **Revisar este archivo** para recordar el contexto
2. **Ver la lista de hitos** y encontrar el siguiente pendiente
3. **Ejecutar los tests** para verificar el estado actual:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
4. **Continuar con el siguiente test** del hito actual
5. **Actualizar los checkboxes** de este archivo al completar tareas

---

## 📚 Ejemplos de Código Base

### Entidad de Dominio
```kotlin
// domain/model/Routine.kt
package com.example.gimnasiopro.domain.model

data class Routine(
    val id: String? = null,
    val name: String,
    val description: String = "",
    val exercises: List<Exercise> = emptyList()
) {
    init {
        require(name.isNotBlank()) { "Routine name cannot be blank" }
    }
}
```

### Repositorio (Puerto)
```kotlin
// domain/repository/RoutineRepository.kt
package com.example.gimnasiopro.domain.repository

import com.example.gimnasiopro.domain.model.Routine

interface RoutineRepository {
    suspend fun save(routine: Routine)
    suspend fun findAll(): List<Routine>
    suspend fun findById(id: String): Routine?
    suspend fun delete(id: String)
}
```

### Caso de Uso
```kotlin
// application/SaveRoutineUseCase.kt
package com.example.gimnasiopro.application

import com.example.gimnasiopro.domain.model.Routine
import com.example.gimnasiopro.domain.repository.RoutineRepository

class SaveRoutineUseCase(private val repository: RoutineRepository) {
    suspend fun execute(routine: Routine) {
        repository.save(routine)
    }
}
```

### Test del Caso de Uso
```kotlin
// test/.../application/SaveRoutineUseCaseTest.kt
package com.example.gimnasiopro.application

import com.example.gimnasiopro.domain.model.Routine
import com.example.gimnasiopro.domain.repository.RoutineRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SaveRoutineUseCaseTest {
    private lateinit var repository: RoutineRepository
    private lateinit var useCase: SaveRoutineUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = SaveRoutineUseCase(repository)
    }

    @Test
    fun `execute should call repository save with given routine`() = runTest {
        // Arrange
        val routine = Routine(name = "Full Body")

        // Act
        useCase.execute(routine)

        // Assert
        coVerify { repository.save(routine) }
    }
}
```

---

## 📅 Historial de Cambios

| Fecha | Hito | Descripción |
|-------|------|-------------|
| 2026-01-31 | 0 | Creación del proyecto y documentación inicial |

---

> **💡 Tip**: Usa este archivo como punto de entrada cada vez que retomes el proyecto.  
> Mantén actualizados los checkboxes y el historial de cambios.

