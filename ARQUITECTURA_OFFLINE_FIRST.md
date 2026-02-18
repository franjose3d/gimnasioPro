# Arquitectura Offline-First - GimnasioPro

## Resumen

La aplicación GimnasioPro usa una arquitectura **offline-first** que permite:
- Funcionar **completamente sin conexión a internet**
- Guardar todos los datos localmente (Room)
- Sincronizar con Firebase **solo cuando es necesario**

## Principios Clave

### 1. Room es la fuente principal de datos
- Todos los datos se leen desde Room (base de datos local)
- La aplicación **SIEMPRE funciona sin internet**
- Firebase es solo para persistencia en la nube y sincronización entre dispositivos

### 2. Firebase se sincroniza en momentos específicos
- **Crear/editar rutinas**: Sincronización inmediata
- **Durante entrenamiento**: SOLO Room (no Firebase)
- **Al finalizar entrenamiento**: Sincronización con Firebase

### 3. El SyncManager controla la sincronización

```kotlin
// Iniciar modo entrenamiento (solo local)
SyncManager.startTrainingMode()

// Finalizar entrenamiento y sincronizar
SyncManager.endTrainingMode()
SyncManager.syncCompleted()

// Verificar estado
SyncManager.isSyncEnabled      // ¿Se puede sincronizar?
SyncManager.isTrainingMode     // ¿Estamos en entrenamiento?
```

## Flujo de Datos

### Entrenamiento (Flujo Offline)

```
┌─────────────────┐
│  Inicio         │
│  Entrenamiento  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ SyncManager.    │
│ startTrainingMode()│
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│  MODO ENTRENAMIENTO (Solo Room)    │
│                                     │
│  - Cargar ejercicios desde Room    │
│  - Modificar pesos/reps            │
│  - NO sincroniza con Firebase      │
│  - Funciona 100% offline           │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Botón          │
│  "Finalizar"    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 1. Guardar en   │
│    Room (local) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. SyncManager. │
│ endTrainingMode()│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. Sincronizar  │
│    con Firebase │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. SyncManager. │
│ syncCompleted() │
└─────────────────┘
```

### Crear/Editar Rutinas (Flujo Normal)

```
┌─────────────────┐
│  Usuario crea   │
│  o edita rutina │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 1. Guardar en   │
│    Room (local) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. Sincronizar  │
│    con Firebase │
│   (inmediato)   │
└─────────────────┘
```

## Estructura de Datos

### Room (Local - Teléfono)
```
GymDatabase
├── ejercicios        (30 ejercicios predefinidos)
├── rutinas           (1-10 rutinas del usuario)
├── registros         (historial de entrenamientos)
├── estadisticas      (métricas: tiempo, volumen, etc.)
└── rutinas_dia       (asignación día→rutina)
```

### Firebase (Remoto - Nube)
```
Firestore
├── clientes/
│   └── {userId}/
│       ├── rutinas/      (rutinas del cliente)
│       └── estadisticas/ (estadísticas del cliente)
├── trainers/
│   └── {userId}/
│       ├── rutinas/      (rutinas del trainer)
│       └── estadisticas/ (estadísticas del trainer)
└── ejercicios/           (ejercicios globales)
```

## Repositorios Híbridos

### EjercicioRepositoryHibrido
- Lee de Room primero (caché inmediato)
- Sincroniza con Firestore en background
- En modo entrenamiento: solo Room

### RutinaRepositoryHibrido
- Guarda en Room + Firebase (si sync habilitada)
- En modo entrenamiento: solo Room

### EstadisticaRepositoryHibrido
- SIEMPRE guarda en Room primero
- Sincroniza con Firestore al finalizar entrenamiento

## Cuándo se sincroniza con Firebase

| Operación | Sincronización |
|-----------|----------------|
| Crear rutina | ✅ Inmediata |
| Editar rutina | ✅ Inmediata |
| Agregar ejercicio a rutina | ✅ Inmediata |
| Durante entrenamiento | ❌ Solo local |
| Modificar peso/reps | ❌ Solo local |
| Finalizar entrenamiento | ✅ Al pulsar botón |
| Abrir app | ✅ Sincroniza en background |
| Login | ✅ Migra rutinas locales |

## Beneficios

1. **Sin pérdida de datos**: Los cambios se guardan localmente inmediatamente
2. **Funciona sin internet**: Todo el entrenamiento funciona offline
3. **Rendimiento óptimo**: No hay latencia de red durante el entrenamiento
4. **Sincronización inteligente**: Solo cuando es necesario
5. **Batería optimizada**: Menos conexiones a internet

## Manejo de Conflictos

- Room es la **fuente de verdad** durante el entrenamiento
- Firebase es la **fuente de verdad** para datos compartidos (trainer-cliente)
- Al sincronizar, se usa la fecha de modificación más reciente

