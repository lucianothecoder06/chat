# Plan de trabajo: Chat Android con Firebase (ICC-451)

Equipo: **Persona A** (Auth, cimientos y notificaciones) y **Persona B** (datos, usuarios, chat e imágenes).
Duración propuesta: **3 semanas (15 días hábiles)**. Si la fecha de entrega es otra, se escalan los días manteniendo el orden.

---

## ⚠️ Bloqueante #0: el proyecto actual incumple un requisito mínimo

El repo se creó con la plantilla **Jetpack Compose** (`MainActivity` usa `setContent {}`, plugin `kotlin.compose`, carpeta `ui/theme`).
El enunciado exige **XML Views** como requisito mínimo obligatorio. Si se entrega en Compose, el proyecto **no aprueba**, sin importar el resto.
→ El ticket **T01** se hace antes que todo lo demás.

---

## Reglas del equipo (esto también se evalúa: 10% + defensa individual)

1. **Un ticket = una rama = un Pull Request.** Rama: `feature/T05-auth-repository`.
2. **El otro integrante revisa y aprueba cada PR** antes de hacer merge. Así ambos conocen todo el código para la defensa.
3. Commits pequeños y frecuentes (mínimo **3–4 commits por semana por persona**). Nada de un commit gigante al final.
4. Mensajes de commit claros: `feat(auth): validar formato de correo en registro`.
5. **Ninguna Activity toca Firebase directamente.** Siempre `Activity → ViewModel → Repository → Firebase`.
6. Cada persona debe poder explicar en la defensa **al menos un ticket del otro** (ver "Ensayo de defensa").

---

## Arquitectura acordada (contrato del Día 1)

```
com.example.chat
├── data
│   ├── model        User, Message, Chat
│   └── repository   AuthRepository, UserRepository, ChatRepository, StorageRepository
├── ui
│   ├── splash       SplashActivity
│   ├── auth         LoginActivity, RegisterActivity, AuthViewModel(s)
│   ├── users        UsersActivity, UsersViewModel, UserAdapter
│   └── chat         ChatActivity, ChatViewModel, MessageAdapter
├── notifications    ChatMessagingService
└── util             Resource (Loading/Success/Error), Validators, DateFormatter, AuthErrorMapper
```

### Modelo de datos en Firestore

```
users/{uid}
  uid, name, email, fcmToken, createdAt

chats/{chatId}                      chatId = uids ordenados unidos por "_"  (ej. "abc_xyz")
  participants: [uidA, uidB]
  lastMessage, lastMessageAt

chats/{chatId}/messages/{messageId}
  senderId, senderName, text, imageUrl, type ("TEXT" | "IMAGE"), timestamp (serverTimestamp)
```

Firebase Storage: `chat_images/{chatId}/{uuid}.jpg`

---

## Tickets

Estimación en días de trabajo. **Peso** = porcentaje de la nota que desbloquea.

### Fase 0: Cimientos (Días 1–3). Nadie avanza sin esto.

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T01 | Migrar de Compose a XML Views | A | 0.5 d | — |
| T02 | Configurar proyecto Firebase + dependencias | B | 0.5 d | — |
| T03 | Estructura de paquetes MVVM + clases base | A | 0.5 d | T01 |
| T04 | Modelos de datos + reglas de seguridad de Firestore/Storage | B | 1 d | T02 |

**T01 — Migrar de Compose a XML Views** (A)
- Quitar plugin `kotlin.compose`, `buildFeatures.compose`, dependencias Compose y la carpeta `ui/theme`.
- Agregar: `appcompat`, `material` (Material Components), `constraintlayout`, `recyclerview`, `lifecycle-viewmodel-ktx`, `lifecycle-livedata-ktx`, `activity-ktx`.
- Activar `buildFeatures { viewBinding = true }`.
- Tema XML: `Theme.Material3.DayNight.NoActionBar` en `themes.xml`.
- `MainActivity` pasa a `AppCompatActivity` con `setContentView(binding.root)`.
- ✅ Hecho cuando: compila y abre una pantalla XML vacía; `grep -r compose app/` no devuelve nada.

**T02 — Configurar Firebase** (B)
- Crear proyecto en Firebase Console, registrar app `com.example.chat`, agregar SHA-1 de debug de **ambas** máquinas.
- `google-services.json` en `app/`, plugin `com.google.gms.google-services`.
- Firebase BoM: `auth`, `firestore`, `storage`, `messaging`.
- Habilitar en consola: Authentication (Email/Password), Firestore, Storage.
- Invitar a Persona A como Editor del proyecto Firebase.
- ✅ Hecho cuando: la app arranca y `FirebaseApp` inicializa sin errores en ambos equipos.

**T03 — Esqueleto MVVM** (A)
- Crear paquetes según la arquitectura de arriba.
- `sealed class Resource<T> { Loading, Success(data), Error(message) }`.
- `object Validators` vacío y `DateFormatter` (timestamp → `dd/MM/yyyy HH:mm`).
- ✅ Hecho cuando: el PR está mergeado y B lo revisó.

**T04 — Modelos + reglas** (B)
- Data classes `User`, `Message`, `Chat` con constructor vacío (requisito de Firestore `toObject()`).
- Función `chatIdFor(uid1, uid2)` (uids ordenados).
- Reglas de Firestore: solo usuarios autenticados; en `chats/{chatId}` solo los `participants` leen/escriben.
- Reglas de Storage: solo usuarios autenticados, máximo 5 MB, solo `image/*`.
- ✅ Hecho cuando: reglas publicadas en consola y copiadas en `firebase/firestore.rules` y `firebase/storage.rules` del repo.

---

### Fase 1: Autenticación (15%) — Persona A (Días 3–6)

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T05 | `AuthRepository` | A | 1 d | T03, T04 |
| T06 | Validaciones + mensajes de error en español + tests unitarios | A | 1 d | T03 |
| T07 | `LoginActivity` + ViewModel | A | 1 d | T05, T06 |
| T08 | `RegisterActivity` + ViewModel | A | 1 d | T05, T06 |
| T09 | Sesión persistente (Splash) + cerrar sesión | A | 0.5 d | T07 |

**T05 — AuthRepository**
- `register(name, email, password)`: crea el usuario en Firebase Auth **y** el documento `users/{uid}`.
- `login(email, password)`, `logout()`, `currentUser`.
- Devuelve `Resource`; nunca expone excepciones crudas a la UI.

**T06 — Validaciones**
- Campos obligatorios, correo con `Patterns.EMAIL_ADDRESS`, contraseña ≥ 6 caracteres, confirmación de contraseña igual.
- `AuthErrorMapper`: `FirebaseAuthInvalidCredentialsException` → "Correo o contraseña incorrectos", `FirebaseAuthUserCollisionException` → "Este correo ya está registrado", `FirebaseAuthWeakPasswordException`, sin red, etc.
- Tests JUnit de `Validators` (mínimo 6 casos). Fácil de mostrar en la defensa.

**T07 / T08 — Login y Registro**
- Layouts XML con `TextInputLayout` (errores inline con `setError`), botón, `ProgressBar`.
- ViewModel expone `LiveData<Resource<...>>`; la Activity solo observa y pinta.
- Navegación con **Intents**: Login ↔ Registro, éxito → `UsersActivity` con `FLAG_ACTIVITY_CLEAR_TASK`.

**T09 — Sesión persistente**
- `SplashActivity` como launcher: si `currentUser != null` → Usuarios, si no → Login.
- Menú en toolbar de Usuarios con "Cerrar sesión" → Login y limpiar back stack.
- ✅ Hecho cuando: matar la app y reabrirla deja al usuario dentro; tras cerrar sesión, reabrir lleva a Login.

---

### Fase 2: Usuarios y Chat en tiempo real (30% + 15% UI) — Persona B (Días 3–10)

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T10 | Lista de usuarios (`UsersActivity` + RecyclerView) | B | 1.5 d | T04 |
| T11 | `ChatRepository` con listener en tiempo real | B | 1.5 d | T04 |
| T12 | `ChatActivity` + `MessageAdapter` (enviados/recibidos) | B | 2 d | T11 |
| T13 | `ChatViewModel`: enviar, validar vacío, ciclo de vida del listener | B | 1 d | T11, T12 |

**T10 — Lista de usuarios**
- `UserRepository.getUsers()` excluye al usuario actual.
- `UsersViewModel` + `UserAdapter` (`ListAdapter` + `DiffUtil`).
- Click → `Intent` a `ChatActivity` con extras `EXTRA_USER_ID`, `EXTRA_USER_NAME`.
- Mientras A termina Auth: crear 2 usuarios de prueba desde la consola para no bloquearse.

**T11 — ChatRepository**
- `sendMessage(chatId, message)`: usa `FieldValue.serverTimestamp()` y actualiza `lastMessage` del chat.
- `listenMessages(chatId)`: `addSnapshotListener` con `orderBy("timestamp", ASCENDING)`, expuesto como `LiveData`/`Flow`; devuelve el `ListenerRegistration` para removerlo.
- Cuidado: con `serverTimestamp` el mensaje local llega primero con `timestamp == null` → tratarlo como "ahora" para que no salte de orden.

**T12 — Pantalla de chat**
- Layout: `RecyclerView` + `EditText` + botón enviar + botón adjuntar imagen.
- `MessageAdapter` con 2 view types: `SENT` (derecha) y `RECEIVED` (izquierda).
- Cada burbuja muestra **nombre del remitente + fecha/hora** (`DateFormatter`).
- `LinearLayoutManager.stackFromEnd = true` y scroll al último mensaje al recibir.

**T13 — ChatViewModel**
- `send(text)`: `text.trim().isEmpty()` → no envía (y el botón se deshabilita con un `TextWatcher`).
- Remover el listener en `onCleared()` (evita fugas y lecturas extra).
- ✅ Hecho cuando: dos emuladores con cuentas distintas se envían mensajes y aparecen en < 1 s, en orden, con nombre y hora.

---

### Fase 3: Imágenes (10%) — Persona B (Días 10–12)

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T14 | Seleccionar y subir imagen a Storage | B | 1 d | T13 |
| T15 | Mostrar imágenes en el chat | B | 1 d | T14 |

**T14 — Subir imagen**
- Selector: `ActivityResultContracts.PickVisualMedia` (sin pedir permisos de almacenamiento).
- `StorageRepository.uploadImage(chatId, uri)` → `downloadUrl`; comprimir a JPEG ~80% antes de subir.
- Envía mensaje `type = "IMAGE"`, `imageUrl = downloadUrl`. Mostrar progreso mientras sube.

**T15 — Ver imágenes**
- Tercer/cuarto view type en `MessageAdapter` (imagen enviada / recibida). Cargar con **Glide** o **Coil**.
- Click en imagen → pantalla completa (Activity simple con Intent).

---

### Fase 4: Notificaciones push (5%) — Persona A (Días 7–12)

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T16 | Token FCM + servicio + canal de notificación | A | 1 d | T05 |
| T17 | Enviar la notificación al receptor (Cloud Function) | A | 2 d | T11, T16 |

**T16 — Cliente FCM**
- `ChatMessagingService : FirebaseMessagingService`; `onNewToken` guarda `fcmToken` en `users/{uid}`; también actualizarlo al hacer login.
- Crear `NotificationChannel`; pedir permiso `POST_NOTIFICATIONS` en Android 13+.
- `onMessageReceived` muestra la notificación; al tocarla abre `ChatActivity` con el otro usuario (PendingIntent).

**T17 — Disparar la notificación**
- La forma correcta: **Cloud Function** `onDocumentCreated("chats/{chatId}/messages/{id}")` que busca el `fcmToken` del receptor y envía el push con Admin SDK.
- Requiere plan **Blaze** (pide tarjeta, pero el uso de este proyecto es $0). Configurar alerta de presupuesto de **$1**.
- ❌ **NO** meter la clave de servicio/servidor de FCM dentro de la app: es un hueco de seguridad y el profe lo puede preguntar.
- Plan B si no pueden usar Blaze: notificación local cuando llega un mensaje nuevo por el listener y el chat no está abierto. Documentarlo en el README como "comportamiento definido".

---

### Fase 5: Calidad, entrega y defensa (10%) — Ambos (Días 13–15)

| ID | Ticket | Dueño | Est. | Depende de |
|----|--------|-------|------|------------|
| T18 | README: setup, arquitectura, capturas, cómo correr | A | 0.5 d | todo |
| T19 | Limpieza: lint, strings en `strings.xml`, código muerto, nombres | B | 0.5 d | todo |
| T20 | Prueba end-to-end en 2 dispositivos (checklist de la rúbrica) | Ambos | 0.5 d | todo |
| T21 | Ensayo de defensa cruzado | Ambos | 0.5 d | todo |

**T20 — Checklist de aceptación (tiene que dar 100% antes de entregar)**
- [ ] Registro con correo/contraseña, validaciones y errores claros
- [ ] Login, logout, sesión persiste al reabrir
- [ ] Flujo Login → Registro → Usuarios → Chat
- [ ] Mensajes en tiempo real, guardados en Firestore, con nombre y fecha/hora, ordenados
- [ ] No se pueden enviar mensajes vacíos
- [ ] Todo en XML Views + RecyclerView; ninguna Activity importa `com.google.firebase`, salvo el servicio FCM
- [ ] Imagen: seleccionar, subir a Storage, verla en el chat
- [ ] Notificación al recibir mensaje
- [ ] Ambos con commits repartidos en las 3 semanas

**T21 — Ensayo de defensa**
- A explica el flujo completo de enviar un mensaje (T11–T13) y B explica login + sesión persistente (T05–T09).
- Cada uno hace una "modificación sencilla" en vivo sobre código del otro (ej. cambiar validación de contraseña a 8 caracteres, cambiar formato de hora).

---

### Opcionales (solo si T01–T21 están listos; no suman al 80%)

| ID | Mejora | Dueño sugerido | Est. |
|----|--------|----------------|------|
| O1 | Lista de conversaciones recientes con último mensaje | B | 1 d |
| O2 | Tema claro/oscuro (ya viene gratis con DayNight) | A | 0.25 d |
| O3 | Indicador "escribiendo…" | B | 1 d |
| O4 | Foto de perfil (reutiliza Storage) | A | 1 d |
| O5 | Estado en línea / desconectado | A | 1 d |

---

## Resumen de carga

| | Persona A | Persona B |
|---|---|---|
| Tickets | T01, T03, T05–T09, T16–T18 | T02, T04, T10–T15, T19 |
| Días estimados | ~10 d | ~11 d |
| % de nota que "posee" | Auth 15% + FCM 5% + arquitectura base | Chat 30% + Imágenes 10% + UI/RecyclerView |
| Compartido | MVVM 15%, Calidad/Git 10%, pruebas y defensa | igual |

## Calendario

| Días | Persona A | Persona B |
|------|-----------|-----------|
| 1–3 | T01, T03 | T02, T04 |
| 3–6 | T05, T06, T07, T08, T09 | T10, T11 |
| 7–10 | T16, T17 | T12, T13 |
| 10–12 | T17 (cierre) + revisar PRs de B | T14, T15 |
| 13–15 | T18, T20, T21 | T19, T20, T21 |

## Si se acaba el tiempo (árbol de decisión)

- ¿Van tarde al Día 10 y el chat en tiempo real no funciona? → **Todo el equipo a T11–T13.** Es 30% y sin eso no hay 80%.
- ¿El chat funciona pero van tarde? → Cortar **opcionales**, luego el plan B de FCM (notificación local).
- Auth + Chat + UI + MVVM + Calidad = **85%**, que ya supera el 80%. Imágenes y FCM son el colchón, no el núcleo.
- **Lo que nunca se corta:** XML Views, RecyclerView, MVVM y commits de ambos (son requisitos mínimos obligatorios y reprueban aunque todo lo demás funcione).
