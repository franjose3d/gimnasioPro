# 🔥 Configuración de Firebase Firestore

## 📋 Pasos para Configurar Firestore

### 1. Subir Reglas de Seguridad

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto
3. Ve a **Firestore Database** → **Reglas**
4. Copia el contenido de `firestore.rules` (en la raíz del proyecto)
5. Pega las reglas en la consola
6. Haz clic en **Publicar**

### 2. Crear Índices

1. Ve a **Firestore Database** → **Índices**
2. Haz clic en **Agregar índice**
3. Crea los siguientes índices:

#### Índice 1: Ejercicios por grupo muscular
- **Colección**: `ejercicios`
- **Campos**:
  - `grupoMuscular` (Ascendente)
  - `nombre` (Ascendente)

#### Índice 2: Entrenamientos por fecha
- **Colección**: `entrenamientos` (subcolección de `users/{userId}`)
- **Campos**:
  - `fechaEntrenamiento` (Descendente)

#### Índice 3: Estadísticas por año y mes
- **Colección**: `estadisticas` (subcolección de `users/{userId}`)
- **Campos**:
  - `anio` (Ascendente)
  - `mes` (Ascendente)

#### Índice 4: Estadísticas por fecha
- **Colección**: `estadisticas` (subcolección de `users/{userId}`)
- **Campos**:
  - `fechaTimestamp` (Ascendente)

**Alternativa**: Puedes importar el archivo `firestore.indexes.json` directamente desde Firebase Console.

### 3. Verificar Configuración

La app automáticamente:
- ✅ Migra ejercicios iniciales al iniciar (una sola vez)
- ✅ Migra datos del usuario al hacer login (una sola vez por usuario)
- ✅ Sincroniza datos en background

## 🔍 Verificar que Todo Funciona

### Verificar Migración de Ejercicios

1. Abre la app
2. Revisa los logs: deberías ver `✅ Ejercicios migrados a Firestore exitosamente`
3. En Firebase Console → Firestore → `ejercicios/` deberías ver los ejercicios

### Verificar Migración de Usuario

1. Inicia sesión con un usuario
2. Revisa los logs: deberías ver `✅ Migración completada para usuario {userId}`
3. En Firebase Console → Firestore → `users/{userId}/` deberías ver:
   - `rutinas/`
   - `calendario/`
   - `entrenamientos/`
   - `estadisticas/`

## 🛠️ Troubleshooting

### Error: "Missing or insufficient permissions"
- **Solución**: Verifica que las reglas de seguridad estén publicadas correctamente

### Error: "Index required"
- **Solución**: Crea los índices necesarios en Firebase Console

### Los ejercicios no se migran
- **Solución**: Verifica que `google-services.json` esté configurado correctamente
- Verifica los logs de la aplicación

### Los datos del usuario no se migran
- **Solución**: Verifica que el usuario esté autenticado
- Revisa los logs para ver errores específicos

## 📊 Estructura Final en Firestore

```
ejercicios/                    # ~160 ejercicios predefinidos
  └── {ejercicioId}/

users/{userId}/                # Datos por usuario
  ├── rutinas/
  │   └── {rutinaId}/
  ├── calendario/
  │   └── {diaSemana}/
  ├── entrenamientos/
  │   └── {entrenamientoId}/
  └── estadisticas/
      └── {fecha}/
```

## 🔄 Migración Manual (Si es Necesario)

Si necesitas forzar la migración manualmente:

```kotlin
// En cualquier Activity o Fragment
val app = application as GimnasioproApplication

// Migrar ejercicios
lifecycleScope.launch {
    app.firestoreInitializer.forzarRemigracionEjercicios()
}

// Migrar datos del usuario
val migrationHelper = UserDataMigrationHelper(
    context = this,
    rutinaRepository = app.rutinaRepository,
    registroEntrenamientoRepository = app.registroEntrenamientoRepository,
    rutinaDiaSemanaRepository = app.rutinaDiaSemanaRepository,
    estadisticaRepository = app.estadisticaRepository
)

lifecycleScope.launch {
    migrationHelper.forzarRemigracion()
}
```

## ✅ Checklist de Configuración

- [ ] Reglas de seguridad publicadas en Firebase Console
- [ ] Índices creados en Firebase Console
- [ ] `google-services.json` configurado correctamente
- [ ] Ejercicios migrados (verificar en Firebase Console)
- [ ] Probar login y verificar migración de datos de usuario
- [ ] Verificar que los datos aparecen en Firestore

## 🎯 Próximos Pasos

1. **Integrar en UI**: Reemplazar llamadas a Room por Firestore gradualmente
2. **Mantener Room como cache**: Usar repositorio híbrido durante transición
3. **Testing**: Probar offline/online, sincronización, etc.
4. **Optimizar**: Ajustar índices según uso real
