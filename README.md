# 🏋️ GimnasioPro

Aplicación Android para gestión de rutinas de gimnasio, ejercicios y seguimiento de progreso .
Con conexión entre trainer y cliente .

---

## 📋 Descripción del Proyecto

GimnasioPro es una aplicación móvil para **entrenar de forma inteligente**, permitiendo:
- ✅ Crear y gestionar rutinas personalizadas
- ✅ Biblioteca de 160+ ejercicios predefinidos
- ✅ Seguimiento de progreso y estadísticas
- ✅ Conexión entre trainers y clientes
- ✅ Funcionamiento 100% offline con sincronización en la nube
- ✅ Sistema de mensajería interno entre trainer-cliente

Adicionalmente, el proyecto cuenta con un portal web complementario para la presentación de servicios llamado **mmdevs**, desarrollado bajo los siguientes parámetros:
- **Tecnologías:** kotlin, firebase, room, recyclerview
- **Diseño:** SOLID, responsive (One-page layout), incluye animaciones (Parallax, Scroll Reveal)
- **Estructura Modularizada:** Separación de estructura (XML), estilos (SVG) y lógica (JS).

---

## 🏗️ Arquitectura

### Clean Architecture + Offline-First

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION                             │
│      Activities / Adapters / ViewModels / DI Manual        │
├─────────────────────────────────────────────────────────────┤
│                       DOMAIN                                │
│            Use Cases / Repository Interfaces                │
├─────────────────────────────────────────────────────────────┤
│                        DATA                                 │
│   Room (Cache Local) + Firestore (Remoto) + Híbrido        │
└─────────────────────────────────────────────────────────────┘
```

### Principios SOLID

| Principio | Aplicación |
|-----------|------------|
| **S** - Single Responsibility | Cada UseCase tiene una única responsabilidad |
| **O** - Open/Closed | Entidades abiertas a extensión mediante herencia |
| **L** - Liskov Substitution | Interfaces de Repository intercambiables |
| **I** - Interface Segregation | Interfaces específicas por funcionalidad |
| **D** - Dependency Inversion | Domain define contratos; Data los implementa |



## 📦 Tecnologías Utilizadas

- **Lenguaje:** Kotlin
- **Base de datos local:** Room Database
- **Base de datos remota:** Firebase Firestore
- **Autenticación:** Firebase Auth
- **Mensajería push:** Firebase Cloud Messaging (FCM)
- **UI:** XML Layouts (Android Views)
- **Arquitectura:** Clean Architecture + MVVM
- **Gestión de dependencias:** Gradle con Version Catalog
- **Testing:** JUnit, Mockito

---

## 🔥 Sistema Híbrido Room + Firebase

### Arquitectura Offline-First

```
📱 UI solicita datos
    ↓
🏠 Room devuelve datos inmediatamente (UX rápida)
    ↓
🔄 Sincronización con Firebase en background
    ↓ (si hay cambios)
📱 UI se actualiza automáticamente
```

### Ventajas
- ⚡ Carga instantánea sin spinners
- 📴 Funciona completamente offline
- 🔄 Sincronización automática cuando hay conexión
- ☁️ Backup automático en la nube
- 📱 Sincronización entre dispositivos

### Sistema de IDs Fijos (Anti-duplicación)

Cada rutina tiene un `numeroRutina` (1-20) **INMUTABLE**:
- El ID en Firebase es siempre `rutina_{numeroRutina}`
- Las actualizaciones **sobreescriben** el mismo documento
- Se usa `set()` en lugar de `add()` para evitar duplicados

---

## 📊 Estructura de Firebase Firestore

```
Firestore/
├── ejercicios/                    # Colección global
│   └── {ejercicioId}/
│       ├── nombre: String
│       ├── grupoMuscular: String
│       ├── descripcion: String
│       ├── esPredefinido: Boolean
│       └── creadoPor: String?
│
├── clientes/{userId}/
│   ├── rutinas/
│   │   └── rutina_{1-20}/        # ID fijo
│   │       ├── numeroRutina: 1-20
│   │       ├── nombre: String
│   │       ├── ejercicioIds: List<String>
│   │       ├── propietarioId: String
│   │       ├── trainerId: String?
│   │       └── fechaModificacion: Timestamp
│   │
│   ├── entrenamientos/{id}/
│   │   ├── rutinaId: String
│   │   ├── fechaEntrenamiento: Timestamp
│   │   ├── ejercicios: List<EjercicioEntrenamiento>
│   │   └── volumenTotal: Float
│   │
│   ├── estadisticas/{fecha}/
│   │   ├── fecha: String (YYYY-MM-DD)
│   │   ├── volumenTotal: Float
│   │   ├── numeroEntrenamientos: Int
│   │   ├── ejerciciosCompletados: Int
│   │   └── tiempoEntrenamientoMs: Long
│   │
│   └── perfil/info/
│       ├── trainerId: String?
│       ├── nombre: String
│       ├── email: String
│       └── telefono: String
│
├── trainers/{userId}/
│   ├── rutinas/                   # Misma estructura
│   ├── clientes/{clienteId}/      # Índice ligero
│   │   ├── nombre: String
│   │   ├── email: String
│   │   └── activo: Boolean
│   └── certificado: String?
│
├── solicitudes/{solicitudId}/     # Temporal
│   ├── clienteId: String
│   ├── trainerId: String?
│   ├── estado: "pendiente|aceptada|rechazada"
│   ├── tipo: "cliente_a_trainer|trainer_a_cliente"
│   ├── mensaje: String?
│   └── fechaSolicitud: Timestamp
│
└── mensajes/{mensajeId}/
    ├── remitenteId: String
    ├── destinatarioId: String
    ├── contenido: String (max 200 chars)
    ├── leido: Boolean
    ├── fechaEnvio: Timestamp
    └── expiracion: Timestamp (7 días)
```

---

## 🔐 Seguridad - Firestore Rules

Las reglas de seguridad implementadas en `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Ejercicios (lectura global, escritura solo del creador)
    match /ejercicios/{ejercicioId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && 
                       request.resource.data.creadoPor == request.auth.uid;
      allow update, delete: if request.auth != null && 
                               resource.data.creadoPor == request.auth.uid;
    }
    
    // Datos de clientes
    match /clientes/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId 
                         || esTrainerDe(userId);
    }
    
    // Datos de trainers
    match /trainers/{userId}/{document=**} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Solicitudes
    match /solicitudes/{solicitudId} {
      allow create: if request.auth != null;
      allow read, update: if request.auth.uid == resource.data.clienteId
                          || request.auth.uid == resource.data.trainerId;
    }
    
    // Helper: Verificar si userId tiene este trainerId
    function esTrainerDe(userId) {
      return get(/databases/$(database)/documents/clientes/$(userId)/perfil/info).data.trainerId == request.auth.uid;
    }
  }
}
```

---

## 👥 Sistema de Conexión Trainer-Cliente

### Modelo de Relación
- **1 cliente** → **1 trainer**
- **1 trainer** → **N clientes**
- Cliente solicita trainer → Trainers disponibles reciben notificación → Aceptan/rechazan
- Trainer puede invitar clientes con código/email/teléfono
- Relación indefinida hasta cancelación

### Flujo de Conexión

```
1. SOLICITUD
   Cliente busca trainer
   O
   Trainer invita cliente (email/teléfono)
   ↓
2. NOTIFICACIÓN
   Se crea documento en /solicitudes/
   Notificación push al destinatario
   ↓
3. ACEPTACIÓN
   Destinatario acepta/rechaza
   ↓
4. CONEXIÓN ESTABLECIDA (Batch write)
   - clientes/{id}/perfil/info → trainerId
   - trainers/{id}/clientes/{clienteId} → {nombre, activo}
   - solicitudes/{id} → estado: "aceptada"
   ↓
5. ACCESO DIRECTO
   Trainer accede a datos del cliente sin copiar
   Security Rules validan el acceso vía trainerId
```

### Características
- **Sin duplicación:** El trainer accede DIRECTAMENTE al espacio Firestore del cliente
- **Autorización automática:** Security Rules verifican `trainerId` en cada operación
- **Caché de permisos:** Firebase cachea las verificaciones (5-60 min)
- **Mensajería interna:** Máx 200 caracteres, persistencia semanal
- **Costo mínimo:** ~1 lectura extra por sesión (cacheada)

---

## 🔔 Sistema de Notificaciones

> 📄 **Documentación completa:** Ver [NOTIFICACIONES_LOCALES.md](./NOTIFICACIONES_LOCALES.md)

### Arquitectura de Notificaciones (Sin Cloud Functions)

Este proyecto implementa notificaciones **100% locales** que NO requieren Cloud Functions ni el plan Blaze de Firebase:

- ✅ **100% Gratuito** (Firebase Spark Plan compatible)
- ✅ **Firestore Snapshot Listeners** detectan cambios en tiempo real
- ✅ **Notificaciones locales** generadas por Android NotificationManager
- ✅ **Funciona offline** después de sincronizar
- ✅ **Badge en el ícono** usando ShortcutBadger

**Flujo:**
```
Trainer envía solicitud → Firestore guarda notificación
   ↓
MainActivity detecta cambio (Snapshot Listener)
   ↓
NotificacionLocalService genera notificación local
   ↓
Usuario ve: Notificación push + Badge en ícono
```

### Badge en el Ícono de la App (Launcher Icon)

El badge muestra un número en el ícono de la app indicando notificaciones pendientes **sin necesidad de abrir la aplicación**.

**Librería utilizada:** [ShortcutBadger](https://github.com/leolin310148/ShortcutBadger) v1.1.22

**Compatibilidad:**
- ✅ Samsung (TouchWiz/OneUI)
- ✅ Xiaomi (MIUI)
- ✅ Sony (Xperia)
- ✅ HTC
- ✅ Huawei/Honor (EMUI)
- ✅ OPPO/Realme (ColorOS)
- ✅ Vivo (FuntouchOS)
- ✅ OnePlus (OxygenOS)
- ⚠️ Google Pixel (requiere activar en Configuración → Apps → Gimnasio Pro → Notificaciones → Badge)
- ⚠️ Stock Android (depende del launcher instalado)
- ⚠️ Emuladores Android (NO soportado, solo funciona en dispositivos reales)

**API de uso:**

```kotlin
// Incrementar cuando llega notificación
NotificationBadgeManager.incrementarBadge(context)

// Decrementar al eliminar notificación no leída
NotificationBadgeManager.decrementarBadge(context)

// Limpiar al marcar todas como leídas
NotificationBadgeManager.limpiarBadge(context)

// Sincronizar con Firestore (actualiza badge visible)
NotificationBadgeManager.sincronizarConFirestore(context, count)

// Verificar compatibilidad con el launcher
NotificationBadgeManager.esCompatibleConLauncher(context)

// Diagnóstico completo
NotificationBadgeManager.diagnosticar(context)

// Limpiar todo al cerrar sesión
NotificationBadgeManager.limpiarTodo(context)
```

**Flujo automático:**

```
1. App inicia
   ↓
2. GimnasioproApplication.onCreate() restaura badge desde SharedPreferences
   ↓
3. MainActivity observa notificaciones en Firestore
   ↓
4. Cuando llega notificación → incrementarBadge()
   ↓
5. ShortcutBadger actualiza VISUALMENTE el ícono (sin abrir app)
   ↓
6. Usuario marca como leída → decrementarBadge()
   ↓
7. Badge se actualiza en tiempo real
```

**Características técnicas:**

- ✅ **Persistencia:** El badge se guarda en `SharedPreferences` con clave `badge_count`
- ✅ **Restauración automática:** Al reiniciar el dispositivo/app, el badge se restaura
- ✅ **Límite visual:** Muestra "9+" para 10 o más notificaciones
- ✅ **Funciona offline:** El badge persiste incluso sin conexión
- ✅ **Sincronización:** Se sincroniza con Firestore automáticamente
- ✅ **Sin permisos especiales:** No requiere permisos adicionales del usuario
- ✅ **Diagnóstico integrado:** Detecta automáticamente launcher y compatibilidad

### Permisos Incluidos para Badge

El `AndroidManifest.xml` incluye **30+ permisos específicos** para diferentes launchers:

```xml
<!-- Permisos generales -->
<uses-permission android:name="com.android.launcher.permission.READ_SETTINGS" />
<uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />

<!-- Samsung/TouchWiz -->
<uses-permission android:name="com.sec.android.provider.badge.permission.READ" />
<uses-permission android:name="com.sec.android.provider.badge.permission.WRITE" />

<!-- Huawei -->
<uses-permission android:name="com.huawei.android.launcher.permission.CHANGE_BADGE" />

<!-- OPPO -->
<uses-permission android:name="com.oppo.launcher.permission.READ_SETTINGS" />
<uses-permission android:name="com.oppo.launcher.permission.WRITE_SETTINGS" />

<!-- ... y más (ver AndroidManifest.xml completo) -->
```

### ❓ Solución de Problemas del Badge

#### **Problema: El badge NO aparece en el ícono de escritorio**

**Verificar logs de diagnóstico:**

```bash
# Filtrar logs del badge
adb logcat -s BadgeManager GimnasioproApp MainActivity

# Buscar estos mensajes:
# ✨ Badge VISIBLE actualizado en el ícono: X
# ⚠️ ShortcutBadger no soportado en este launcher
# 📱 Launcher detectado: com.android.launcher3
```

**Posibles causas y soluciones:**

1. **Emulador Android (no soportado)**
   - ❌ ShortcutBadger NO funciona en emuladores
   - ✅ Prueba en un **dispositivo físico** (Samsung, Xiaomi, Huawei, etc.)

2. **Launcher no compatible (Stock Android, Nova Launcher, etc.)**
   - Log: `⚠️ ShortcutBadger no soportado en este launcher`
   - ✅ **Solución:** Cambiar al launcher por defecto del fabricante
   - ✅ **Alternativa:** Activar badges en: **Configuración → Apps → Gimnasio Pro → Notificaciones → Permitir badge**

3. **Badges deshabilitados en configuración del sistema**
   - ✅ **Samsung/Xiaomi/Huawei:**
     - Ve a: `Configuración → Apps → Gimnasio Pro → Notificaciones`
     - Activa: `Permitir notificaciones` y `Badge en ícono de app`
   - ✅ **Google Pixel/Stock Android:**
     - Mantén presionado el ícono de la app
     - Ve a: `Información de la app → Notificaciones`
     - Activa: `Permitir badge en ícono`

4. **Permisos de notificación no concedidos**
   - Verifica que la app tenga permiso de mostrar notificaciones
   - Android 13+: El sistema solicita permiso explícito en el primer uso

5. **Contador en 0 (ninguna notificación pendiente)**
   - El badge solo se muestra cuando `count > 0`
   - Verifica logs: `🔔 Badge actualizado: 0 notificaciones no leídas`

**Comando de diagnóstico manual:**

```kotlin
// Ejecutar en MainActivity.onCreate() o desde Debug Console
NotificationBadgeManager.diagnosticar(this)

// Output esperado:
// === DIAGNÓSTICO DE BADGE ===
// Contador guardado: 3
// Launcher: com.sec.android.app.launcher (Samsung)
// Compatible: true
// Notificaciones activas del sistema: 3
// === FIN DIAGNÓSTICO ===
```

**Si el badge NO aparece pero los logs dicen "✨ Badge VISIBLE actualizado":**

Esto significa que ShortcutBadger funcionó correctamente, pero:
- El launcher actual **no muestra badges visualmente** (por diseño)
- Necesitas activar badges en la configuración del sistema
- Estás usando un emulador (no soportado)

**Verificación final:**

```bash
# 1. Verificar que hay notificaciones pendientes
adb shell content query --uri content://com.example.gimnasiopro/notifications

# 2. Ver SharedPreferences
adb shell run-as com.example.gimnasiopro cat /data/data/com.example.gimnasiopro/shared_prefs/notification_badge.xml

# 3. Forzar actualización del badge (desde adb shell)
adb shell am broadcast -a android.intent.action.BADGE_COUNT_UPDATE
```

### Resumen de Comportamiento Esperado

| Ubicación | Comportamiento | Estado |
|-----------|---------------|--------|
| **Campanita en la app** | Badge rojo con número | ✅ SIEMPRE funciona |
| **Ícono de escritorio** | Badge numérico (1, 2, 9+) | ⚠️ Depende del launcher |
| **SharedPreferences** | Contador persistido | ✅ SIEMPRE se guarda |
| **Firestore** | Sincronización automática | ✅ SIEMPRE se sincroniza |

### Firebase Cloud Messaging (FCM)

```
1. Firebase Cloud Messaging recibe mensaje
   ↓
2. MyFirebaseMessagingService.onMessageReceived()
   ↓
3. NotificationBadgeManager.incrementarBadge()
   ↓
4. Se muestra notificación con número
   ↓
5. Ícono de la app muestra badge (1, 2, 3... 9+)
   ↓
6. Badge visible incluso con la app cerrada
```

### Tipos de Notificaciones

1. **Solicitud de conexión Trainer → Cliente**
   - Badge incrementa automáticamente
   - Notificación push con botones Aceptar/Rechazar
   
2. **Solicitud de conexión Cliente → Trainer**
   - Badge incrementa automáticamente
   - Notificación push con botones Aceptar/Rechazar

3. **Mensaje interno (max 200 caracteres)**
   - Badge incrementa automáticamente
   - Se elimina automáticamente después de 7 días
   - Persistencia semanal para evitar sobrecarga

### Depuración del Badge

Para verificar que el badge funciona correctamente:

```bash
# Ver logs en tiempo real
adb logcat | grep BadgeManager

# Logs esperados:
# ✨ Badge VISIBLE actualizado en el ícono: 1
# 🔄 Badge sincronizado con Firestore: 2 notificaciones no leídas
# ➕ Badge incrementado: 1 → 2
# ➖ Badge decrementado: 2 → 1
# 🧹 Badge limpiado
# 🔔 Badge restaurado al iniciar: 3 notificaciones pendientes
```

---

## 📱 Funcionalidades Principales

### Para Clientes
- ✅ Crear y editar rutinas personalizadas
- ✅ Calendario de entrenamientos semanal
- ✅ Registro de entrenamientos con pesos y repeticiones
- ✅ Estadísticas de progreso (volumen total, racha, mejores marcas)
- ✅ Búsqueda de trainers
- ✅ Chat con trainer asignado
- ✅ Notificaciones de solicitudes

### Para Trainers
- ✅ Gestión de múltiples clientes
- ✅ Acceso directo a rutinas de clientes
- ✅ Creación/edición de rutinas para clientes
- ✅ Visualización de progreso de clientes
- ✅ Sistema de mensajería con clientes
- ✅ Certificado profesional visible
- ✅ Dashboard de clientes activos

### Comunes
- ✅ Biblioteca de 160+ ejercicios por grupo muscular
- ✅ Crear ejercicios personalizados
- ✅ Búsqueda de ejercicios por nombre
- ✅ Funcionamiento offline completo
- ✅ Sincronización automática en la nube

---

## 🚀 Instalación y Configuración

### Requisitos Previos
- Android Studio Arctic Fox o superior
- JDK 17+
- Cuenta de Firebase (plan gratuito)
- SDK de Android 24+ (Android 7.0 Nougat)

### Pasos de Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/GimnasioPro.git
   cd GimnasioPro
   ```

2. **Configurar Firebase**
   - Crear proyecto en [Firebase Console](https://console.firebase.google.com/)
   - Descargar `google-services.json`
   - Colocar en `app/google-services.json`

3. **Configurar Firestore**
   - Habilitar Firestore Database en Firebase Console
   - Modo de producción
   - Región: `us-central1` (o la más cercana)

4. **Subir Reglas de Seguridad**
   ```bash
   # Opción 1: Desde Firebase Console
   # Copiar contenido de firestore.rules y pegar en Console
   
   # Opción 2: Usando Firebase CLI
   firebase deploy --only firestore:rules
   ```

5. **Crear Índices**
   ```bash
   # Opción 1: Importar desde archivo
   firebase deploy --only firestore:indexes
   
   # Opción 2: Crear manualmente en Firebase Console → Índices
   # Ver detalles en firestore.indexes.json
   ```

6. **Compilar y ejecutar**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Verificación

Después de la instalación:
- ✅ La app migra ejercicios a Firestore automáticamente
- ✅ Al hacer login, migra datos del usuario
- ✅ Verifica en Firebase Console:
  - `ejercicios/` debe tener ~160 documentos
  - `clientes/{userId}/` o `trainers/{userId}/` debe existir

---

## 🧪 Testing

### Tests Unitarios
```bash
./gradlew test
```

### Tests de Integración
```bash
./gradlew connectedAndroidTest
```

### Cobertura de Tests
- ✅ Repositorios híbridos
- ✅ Use Cases
- ✅ ViewModels
- ✅ Sincronización Firebase
- ✅ Sistema de conexión trainer-cliente

---

## 📁 Estructura del Proyecto

```
app/src/main/
├── java/com/example/gimnasiopro/
│   ├── data/                       # Capa de datos
│   │   ├── Ejercicio.kt           # Entidades Room
│   │   ├── EjercicioDao.kt        # DAOs
│   │   ├── EjercicioRepository.kt # Repositorios locales
│   │   ├── GymDatabase.kt         # Configuración Room
│   │   └── firestore/             # Firebase
│   │       ├── EjercicioFirestore.kt
│   │       ├── RutinaFirestore.kt
│   │       ├── EjercicioFirestoreRepository.kt
│   │       ├── RutinaRepositoryHibrido.kt
│   │       └── ConexionRepository.kt
│   │
│   ├── domain/                     # Lógica de negocio
│   │   ├── model/
│   │   ├── repository/
│   │   └── usecase/
│   │
│   ├── presentation/               # UI
│   │   ├── cliente/
│   │   └── trainer/
│   │
│   ├── components/                 # UI reutilizable
│   │   ├── EjercicioAdapter.kt
│   │   ├── RutinaAdapter.kt
│   │   └── NotificacionAdapter.kt
│   │
│   ├── utils/                      # Utilidades
│   │   ├── NotificationBadgeManager.kt
│   │   └── SyncManager.kt
│   │
│   └── MainActivity.kt
│
└── res/
    ├── layout/                     # XML Layouts
    ├── values/
    │   ├── colors.xml             # Tema verde oscuro
    │   └── strings.xml
    └── drawable/
```

---

## 🎨 Diseño y UI

### Tema
- **Color principal:** Azul oscuro 
- **Acento:** Azul claro 
- **Background:** Gris oscuro 
- **Texto:** Blanco/Gris claro

### Componentes Reutilizables
- Botones redondeados
- Cards con elevación
- RecyclerViews optimizados
- Diálogos personalizados

---

## 📈 Roadmap

### Versión Actual (v1.0)
- ✅ Sistema de rutinas completo
- ✅ Conexión trainer-cliente
- ✅ Sincronización offline-first
- ✅ Estadísticas básicas


### Próximas Versiones

#### v1.1
- [ ] Gráficos de progreso avanzados
- [ ] Exportar/importar rutinas
- [ ] Compartir rutinas entre usuarios
- [ ] Modo oscuro/claro

#### v1.2
- [ ] Grupos de entrenamiento
- [ ] Ranking de clientes
- [ ] Desafíos semanales
- [ ] Integración con wearables

#### v2.0
- [ ] Migración a Jetpack Compose
- [ ] Videos de ejercicios
- [ ] Planes de nutrición
- [ ] IA para recomendación de rutinas

---

## 🤝 Contribución

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo `LICENSE` para más detalles.

---

## 👨‍💻 Autor

**Francisco Jose Meroño Muñoz ** 

---

## 🙏 Agradecimientos

- Comunidad de Android Developers
- Firebase por las herramientas de backend
- Todos los contribuidores y testers
- Equipo de Brais moure
- Equipo de Big school

---

## 📞 Soporte

¿Problemas o preguntas? Abre un issue en GitHub o contacta a:
- Email: mmdevmerono@gmail.com
- GitHub Issues: [github.com/tu-usuario/GimnasioPro/issues](https://github.com/tu-usuario/GimnasioPro/issues)

---

**¡Entrena inteligente con GimnasioPro! 💪**

