# Implementación de Rutinas en Firebase

## Resumen

Se ha implementado un sistema híbrido para rutinas que combina:
- **Room (local)**: Para funcionamiento offline y rendimiento
- **Firebase (remoto)**: Para sincronización con trainers y backup

## Estructura en Firebase

```
Firebase/
├── rutinas/
│     └── [rutinaId]/
│           ├── rutinaId: "auto-generado"
│           ├── nombre: "Rutina 1"
│           ├── propietarioId: "userId"          ← Dueño de la rutina
│           ├── creadoPorId: "userId"            ← Quién la creó
│           ├── creadoPorTipo: "cliente|trainer"
│           ├── compartidaConTrainer: true       ← Si el trainer puede verla
│           ├── trainerId: "trainerId"           ← Trainer asignado (opcional)
│           ├── ejercicioIds: ["1", "5", "12"]   ← IDs de ejercicios
│           ├── activa: true
│           ├── fechaCreacion: Timestamp
│           └── fechaModificacion: Timestamp
│
├── conexiones/
│     └── [conexionId]/
│           ├── trainerId: "userId"
│           ├── clienteId: "userId"
│           ├── estado: "pendiente|activa|rechazada|finalizada"
│           ├── solicitadoPor: "cliente|trainer"
│           ├── fechaSolicitud: Timestamp
│           ├── fechaRespuesta: Timestamp
│           └── mensaje: "Mensaje opcional"
```

## Archivos creados/modificados

### Nuevos archivos:

1. **`RutinaFirestore.kt`** - Modelo de rutina para Firebase
2. **`RutinaFirestoreRepository.kt`** - Repositorio de rutinas en Firebase (CRUD básico)
3. **`RutinaRepositoryHibrido.kt`** - Combina Room + Firebase
4. **`ConexionTrainerCliente.kt`** - Modelo de conexión
5. **`ConexionRepository.kt`** - Repositorio de conexiones

### Archivos modificados:

1. **`GimnasioproApplication.kt`** - Añadidos repositorios híbridos
2. **`ListaEjerciciosActivity.kt`** - Usa repositorio híbrido de rutinas
3. **`LoginActivity.kt`** - Migra rutinas a Firebase al iniciar sesión

## Flujo de sincronización

### Al guardar una rutina:
1. Se guarda en Room (local)
2. Se sincroniza automáticamente con Firebase
3. El trainer conectado podrá verla

### Al iniciar sesión:
1. Se migran todas las rutinas locales a Firebase
2. Las rutinas con ejercicios se sincronizan

### Permisos de trainer:
- Solo puede ver/editar rutinas de clientes con conexión **activa**
- La verificación se hace consultando la colección `conexiones`

## Uso del repositorio híbrido

```kotlin
// Obtener repositorio
val rutinaRepo = (application as GimnasioproApplication).rutinaRepositoryHibrido

// Agregar ejercicios (se sincroniza automáticamente)
rutinaRepo.agregarEjerciciosARutina(numeroRutina, ejercicioIds)

// Migrar rutinas a Firebase
rutinaRepo.migrarRutinasAFirebase()

// Descargar rutinas de Firebase
rutinaRepo.descargarRutinasDeFirebase()

// Para trainers: ver rutinas de un cliente
rutinaRepo.getRutinasDeCliente(clienteId, trainerId)

// Para trainers: crear rutina para cliente
rutinaRepo.crearRutinaParaCliente(trainerId, clienteId, nombre, ejercicioIds)
```

## Próximos pasos sugeridos

1. **Pantalla de "Mis Clientes"** para trainers
2. **Pantalla de "Buscar Trainer"** para clientes
3. **Notificaciones** cuando el trainer modifica una rutina
4. **Historial de cambios** en rutinas

## Estados de conexión

| Estado | Descripción |
|--------|-------------|
| `pendiente` | Cliente solicitó, trainer no ha respondido |
| `activa` | Trainer aceptó, conexión activa |
| `rechazada` | Trainer rechazó la solicitud |
| `finalizada` | Cualquiera de los dos finalizó la conexión |

