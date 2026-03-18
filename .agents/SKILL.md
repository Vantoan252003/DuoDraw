---
name: CoDraw Project Architecture
description: Full-stack architecture overview of the CoDraw collaborative drawing app (Android frontend + Spring Boot backend)
---

# CoDraw — Project Architecture

CoDraw is a **real-time collaborative drawing app** where two players share a canvas via WebSocket. One player creates or joins a room, and both draw simultaneously.

---

## Repository Layout

```
CoDraw/
├── app/                          ← Android frontend (Kotlin / Jetpack Compose)
│   └── src/main/java/com/toan/codraw/
└── CoDrawJavaBackend/            ← Java backend (Spring Boot 4.0.3)
    └── src/main/java/com/codraw/CoDraw/
```

---

## Backend — Spring Boot (`CoDrawJavaBackend/`)

**Tech stack:** Spring Boot 4.0.3, JPA/Hibernate, MySQL, Redis, WebSocket, JWT (jjwt 0.12.6), Cloudinary, Lombok, Java 21, Maven.

### Package Structure

| Package | Purpose |
|---------|---------|
| `controller/` | REST endpoints |
| `service/` | Business logic |
| `entity/` | JPA entities (MySQL tables) |
| `dto/` | Request/response DTOs |
| `handler/` | WebSocket handler |
| `model/` | Non-entity models (StrokeMessage) |
| `config/` | Redis, Cloudinary, Security, WebSocket config |
| `security/` | JWT filter + utils |
| `repository/` | Spring Data JPA repos |

### REST API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login, returns JWT |
| GET | `/api/profile/me` | JWT | Get current user profile |
| PUT | `/api/profile` | JWT | Update display name |
| POST | `/api/profile/avatar` | JWT | Upload avatar (Cloudinary) |
| POST | `/api/rooms/create` | JWT | Create room (PUBLIC/PRIVATE) |
| POST | `/api/rooms/join?code=X` | JWT | Join room by code |
| GET | `/api/rooms/public` | No | List public WAITING rooms |
| GET | `/api/rooms/{code}` | JWT | Get room info |
| GET | `/api/drawings/mine` | JWT | Get user's completed drawings |
| POST | `/api/drawings/complete` | JWT | Save completed drawing |
| GET | `/api/drawings/{roomCode}` | JWT | Get specific completed drawing |

### WebSocket

- **Endpoint:** `ws://host:8080/ws/draw?roomCode=ABC123&token=<jwt>`
- **Handler:** `DrawingWebSocketHandler` — manages rooms in-memory via `ConcurrentHashMap`
- **Message types:**
  - `STROKE` → saved to Redis + broadcast to others
  - `STROKE_PREVIEW` → relay only (not persisted)
  - `CLEAR` → clears Redis canvas + broadcast to all
  - `UNDO` → removes last stroke of player from Redis + broadcast
  - `COMPLETE_REQUEST/RESPONSE/FINALIZED/CANCELLED` → relay only (completion flow)
  - `JOIN` → server sends back stroke history from Redis

### Data Model

- **User** (`users`) — id, username, email, password (BCrypt), displayName, avatarUrl
- **Room** (`rooms`) — id, roomCode (6 chars), hostUsername, guestUsername, status (WAITING→PLAYING→FINISHED), roomType (PUBLIC/PRIVATE), createdAt
- **CompletedDrawing** (`completed_drawings`) — id, roomCode, hostUsername, guestUsername, roomType, savedByUsername, strokeCount, strokesJson (LONGTEXT), completedAt

### Canvas State (Redis)

- **Key:** `room:{roomCode}:strokes` → List of StrokeMessage JSON
- **TTL:** 2 hours
- **Service:** `CanvasStateService` — addStroke, getStrokes, clearStrokes, removeLastStrokeForPlayer

### Environment (.env)

```
CLOUDINARY_CLOUD_NAME=xxx
CLOUDINARY_API_KEY=xxx
CLOUDINARY_API_SECRET=xxx
```

JWT config in `application.properties`: `app.jwt.secret`, `app.jwt.expiration-ms`

---

## Frontend — Android (`app/`)

**Tech stack:** Kotlin, Jetpack Compose (Material 3), Hilt (DI), Retrofit + OkHttp, Coil (images), WebSocket (OkHttp), Gradle (KSP).

### Package Structure

| Package | Purpose |
|---------|---------|
| `data/local/` | `SessionManager` (SharedPreferences for JWT, user info, language, active room) |
| `data/remote/` | `ApiService` (Retrofit), `WebSocketManager`, DTOs |
| `data/repository/` | Repository implementations |
| `di/` | Hilt `AppModule` |
| `domain/model/` | Domain models |
| `domain/repository/` | Repository interfaces |
| `domain/usecase/` | Drawing event use cases |
| `navigation/` | `NavGraph` (Compose Navigation) |
| `presentation/ui/` | Screens (Home, Login, Register, Settings, Room, Drawing, SavedDrawing) |
| `presentation/viewmodel/` | ViewModels (Home, Auth, Room, Drawing, Settings, SavedDrawing) |
| `presentation/components/` | Reusable composables (DrawingCanvas, DrawingTool, ColorWheelPicker) |
| `presentation/model/` | UI model (DrawingToolMode) |
| `ui/theme/` | Material 3 theme + gradient colors |

### Navigation Routes

| Route | Screen |
|-------|--------|
| `login` | LoginScreen |
| `register` | RegisterScreen |
| `home` | HomeScreen |
| `room?code={code}` | RoomScreen |
| `drawing/{roomCode}/{playerId}/{playerCount}` | DrawingScreen |
| `savedDrawing/{roomCode}` | SavedDrawingScreen |
| `settings` | SettingsScreen |

### Key Features

- **Realtime public rooms:** 5-second client-side polling of `GET /api/rooms/public`
- **Active room tracking:** Persisted in SharedPreferences; shows resume card on HomeScreen
- **Pinch-to-zoom/pan:** Two-finger gestures on DrawingCanvas (scale 0.5x–5x)
- **Color wheel picker:** HSV color wheel with brightness slider + apply/cancel buttons
- **Collapsible drawing tools:** Vertical side panel with animated expand/collapse
- **Bilingual:** All strings in `values/strings.xml` (EN) and `values-vi/strings.xml` (VI)
- **Language restart:** Changing language in Settings kills process and relaunches app

### Build Commands

```bash
# Android (from project root)
./gradlew assembleDebug

# Backend (from CoDrawJavaBackend/)
./mvnw spring-boot:run
```
