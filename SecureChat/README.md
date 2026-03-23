# 🔐 SecureChat — Android Kotlin

> Ứng dụng nhắn tin & gọi video bảo mật  
> Stack: Kotlin · Jetpack Compose · MVVM + Clean Architecture · Firebase · WebRTC

---

## 📐 Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ LoginScreen  │  │  ChatScreen  │  │  CallScreen  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │  observe StateFlow via collectAsState          │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐  │
│  │ AuthViewModel│  │ ChatViewModel│  │ CallViewModel│  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │  calls UseCases │                 │
┌─────────▼─────────────────▼─────────────────▼───────────┐
│  Domain Layer (Pure Kotlin — no Android imports)        │
│  SignInUseCase · SendMessageUseCase · InitiateCallUseCase│
│  AuthRepository (interface) · ChatRepository (interface) │
└─────────────────────────┬───────────────────────────────┘
                          │  implements
┌─────────────────────────▼───────────────────────────────┐
│  Data Layer                                             │
│  AuthRepositoryImpl    → Firebase Auth + Firestore      │
│  ChatRepositoryImpl    → Firestore + Room DB (cache)    │
│  CallRepositoryImpl    → Firestore (WebRTC Signaling)   │
│  WebRTCManager         → stream-webrtc-android          │
│  SecureChatFCMService  → Firebase Cloud Messaging       │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 Cấu trúc thư mục

```
app/src/main/java/com/securechat/
├── MainActivity.kt              ← Entry point + Hilt
├── di/
│   └── AppModules.kt            ← Firebase, Room, Repository bindings
├── domain/
│   ├── model/Models.kt          ← User, Message, ChatRoom, CallSession
│   ├── repository/Repositories.kt ← Interfaces (abstractions)
│   └── usecase/UseCases.kt      ← Business logic thuần
├── data/
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── ChatRepositoryImpl.kt
│   │   ├── CallRepositoryImpl.kt
│   │   └── UserRepositoryImpl.kt
│   ├── local/
│   │   ├── RoomDatabase.kt      ← Room DB + DAO
│   │   └── entity/
│   ├── remote/webrtc/
│   │   └── WebRTCManager.kt     ← WebRTC P2P logic
│   └── mapper/Mappers.kt        ← Domain ↔ Entity converters
├── ui/
│   ├── navigation/NavGraph.kt   ← Compose Navigation
│   ├── theme/Theme.kt           ← Material3 + colors + typography
│   └── screens/
│       ├── auth/                ← Login, Register + ViewModel
│       ├── home/                ← Room list + ViewModel
│       ├── chat/                ← Chat screen + ViewModel
│       └── call/                ← Video call + ViewModel
└── service/
    └── SecureChatFCMService.kt  ← Push notification
```

---

## 🚀 Cài đặt & Chạy

### Bước 1 — Firebase setup
1. Tạo project tại [Firebase Console](https://console.firebase.google.com)
2. Thêm Android app với package `com.securechat`
3. Tải `google-services.json` → đặt vào `app/`
4. Bật **Authentication** (Email/Password)
5. Tạo **Firestore Database** (production mode)
6. Deploy security rules: `firebase deploy --only firestore:rules`

### Bước 2 — Chạy project
```bash
# Clone repo
git clone https://github.com/<team>/secure-chat-android.git

# Mở bằng Android Studio Ladybug trở lên
# File → Open → chọn thư mục SecureChat

# Sync Gradle, build và run trên emulator/device
```

---

## 🌿 Git Workflow (Bắt buộc theo yêu cầu đề)

```bash
# 1. Luôn pull develop trước khi tạo nhánh
git checkout develop
git pull origin develop

# 2. Tạo nhánh feature
git checkout -b feature/login-screen-nguyenvanA

# 3. Commit theo chuẩn Conventional Commits
git commit -m "feat: add LoginScreen with email/password validation"
git commit -m "fix: resolve StateFlow not updating on error"
git commit -m "refactor: extract MessageBubble to separate composable"
git commit -m "test: add unit test for SignInUseCase"

# 4. Push và tạo Pull Request → develop
git push origin feature/login-screen-nguyenvanA
# Vào GitHub → New Pull Request → yêu cầu 1 người review
```

### Quy tắc đặt tên nhánh
| Loại | Pattern | Ví dụ |
|------|---------|-------|
| Tính năng | `feature/[chức-năng]-[tên]` | `feature/chat-screen-tranthiB` |
| Bug fix | `fix/[mô-tả]-[tên]` | `fix/fcm-token-null-levanC` |
| Refactor | `refactor/[mô-tả]-[tên]` | `refactor/clean-viewmodel-phamthiD` |

---

## 🗃️ Firestore Schema

```
users/{uid}
  displayName: String
  email:       String
  photoUrl:    String?
  isOnline:    Boolean
  fcmToken:    String?
  createdAt:   Timestamp

rooms/{roomId}
  name:        String
  members:     [uid, uid, ...]
  isGroup:     Boolean
  lastMessage: { content, senderId, createdAt }
  createdAt:   Timestamp

  messages/{messageId}
    senderId:  String
    content:   String
    type:      "TEXT" | "IMAGE" | "FILE" | "CALL_LOG"
    fileUrl:   String?
    isRead:    Boolean
    createdAt: Timestamp

calls/{sessionId}
  callerId:  String
  calleeId:  String
  status:    "RINGING" | "ACCEPTED" | "DECLINED" | "ENDED"
  type:      "VIDEO" | "AUDIO"
  offer:     { sdp: String }
  answer:    { sdp: String }

  iceCandidates/{id}
    candidate: "sdpMid|sdpMLineIndex|sdp"
```

---

## 👥 Phân công nhóm (4 người)

| Thành viên | Nhánh chính | Phụ trách |
|-----------|-------------|-----------|
| Thành viên A | `feature/auth-*` | Auth flow, Firebase Auth, Login/Register UI |
| Thành viên B | `feature/chat-*` | Chat screen, Firestore realtime, Room DB |
| Thành viên C | `feature/call-*` | WebRTC, VideoCall screen, FCM |
| Thành viên D | `feature/ui-*` | Home screen, Navigation, Theme, Design |

---

## 📊 Tỷ lệ đóng góp (ví dụ — cập nhật từ GitHub Insights)

| Thành viên | Commits | Lines | % |
|-----------|---------|-------|---|
| Thành viên A | ~30 | ~800 | 25% |
| Thành viên B | ~35 | ~950 | 30% |
| Thành viên C | ~28 | ~850 | 27% |
| Thành viên D | ~22 | ~600 | 18% |

> Cập nhật bảng này từ **GitHub → Insights → Contributors** trước khi nộp báo cáo.

---

## ✅ Checklist trước demo

- [ ] Đăng ký / Đăng nhập hoạt động
- [ ] Danh sách phòng chat hiển thị realtime
- [ ] Gửi/nhận tin nhắn realtime (Firestore)
- [ ] Offline cache hoạt động (Room DB)
- [ ] Gọi video P2P kết nối được (WebRTC)
- [ ] Nhận notification khi app bị kill (FCM)
- [ ] Firestore Security Rules đã deploy
- [ ] Tất cả nhánh feature đã merge vào develop
- [ ] Ít nhất 1 Pull Request mỗi thành viên
- [ ] README đã cập nhật % đóng góp
