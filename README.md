# 💬 SecureChat — Ứng dụng Gọi Video & Nhắn Tin Nhóm

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Video-WebRTC-333333?style=for-the-badge&logo=webrtc&logoColor=white"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
</p>

<p align="center">
  Ứng dụng nhắn tin nhóm và gọi video P2P thời gian thực trên Android, xây dựng với Kotlin + Firebase + WebRTC.
  <br/>
  <strong>Đồ án môn học — Khoa Toán - Tin, Trường Đại học Sư phạm Đà Nẵng</strong>
</p>

---

## 📋 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt & Chạy dự án](#-cài-đặt--chạy-dự-án)
- [Cấu hình Firebase](#-cấu-hình-firebase)
- [Giao diện ứng dụng](#-giao-diện-ứng-dụng)
- [Thành viên nhóm](#-thành-viên-nhóm)

---

## 🚀 Giới thiệu

**SecureChat** là ứng dụng giao tiếp thời gian thực được phát triển trên nền tảng Android, hướng đến phục vụ nhu cầu học tập và làm việc nhóm của sinh viên. Ứng dụng tích hợp nhắn tin 1-1 và nhóm, gửi file, gọi video nhóm peer-to-peer và thông báo đẩy thông minh.

Điểm khác biệt cốt lõi của SecureChat so với các ứng dụng thương mại là **kiến trúc hoàn toàn kiểm soát được** — từ signaling server WebRTC đến luồng media P2P — phù hợp cho mục tiêu nghiên cứu và học thuật.

---

## ✨ Tính năng

| Tính năng | Mô tả | Trạng thái |
|-----------|-------|-----------|
| 🔐 Đăng ký / Đăng nhập | Email/Password và Google Sign-In | ✅ Hoàn thành |
| 💬 Nhắn tin thời gian thực | Chat 1-1 và nhóm, độ trễ < 500ms | ✅ Hoàn thành |
| ✅ Trạng thái đã đọc | Seen receipt cho từng tin nhắn | ✅ Hoàn thành |
| 🎥 Gọi video 1-1 | Kết nối WebRTC P2P | ✅ Hoàn thành |
| 👥 Nhắn tin nhóm | Tối đa 4 người, kiến trúc Mesh | ✅ Hoàn thành |
| 🔔 Thông báo cuộc gọi | ✅ Hoàn thành |
| 😊 Thả cảm xúc | React với emoji vào tin nhắn | ✅ Hoàn thành |
| 🌙 Dark Mode | Hỗ trợ chế độ tối | ✅ Hoàn thành |
| 👤 Quản lý hồ sơ | Chỉnh sửa tên, ảnh đại diện | ✅ Hoàn thành |

---

## 🛠 Công nghệ sử dụng

### Frontend (Android)
- **Kotlin 1.9.x** — Ngôn ngữ lập trình chính
- **Jetpack Compose** — UI khai báo (declarative UI), Material Design 3
- **Kotlin Coroutines & Flow** — Xử lý bất đồng bộ
- **ViewModel + StateFlow** — Quản lý trạng thái UI

### Backend (Firebase)
- **Firebase Authentication** — Xác thực Email/Password & Google Sign-In
- **Firebase Realtime Database** — Lưu trữ và đồng bộ tin nhắn thời gian thực + Signaling WebRTC
- **Firebase Storage** — Lưu trữ file và hình ảnh
- **Firebase Cloud Messaging (FCM)** — Thông báo đẩy đa trạng thái

### Gọi Video
- **WebRTC** (io.getstream:stream-webrtc-android) — Kết nối P2P
- **STUN Server** — `stun.l.google.com:19302` (khám phá địa chỉ IP công cộng)
- **TURN Server** — Relay fallback khi P2P trực tiếp không khả thi

### Công cụ
- Android Studio Hedgehog | 2023.1.1+
- Gradle 8.x với Kotlin DSL
- Git / GitHub

---

## 🏗 Kiến trúc hệ thống

SecureChat áp dụng **MVVM + Clean Architecture** với 3 tầng độc lập:

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│   Composable UI  ←→  ViewModel          │
│         (StateFlow / UiState)           │
├─────────────────────────────────────────┤
│             Domain Layer                │
│   SendMessageUseCase, InitiateCall...   │
│        (Độc lập với framework)          │
├─────────────────────────────────────────┤
│              Data Layer                 │
│  FirebaseMessageRepo, FirebaseAuthRepo  │
│        (Firebase SDK, WebRTC SDK)       │
└─────────────────────────────────────────┘
```

### Luồng dữ liệu một chiều (UDF)
```
User Action → UI Event → ViewModel → UseCase → Repository → Firebase
                                                                ↓
                  Composable re-render ← UiState update ← Flow emission
```

### Kiến trúc WebRTC Signaling

Firebase Realtime Database được dùng làm **Signaling Server** thay vì xây dựng WebSocket server riêng:

```
Client A ──── SDP Offer ────► Firebase ──── SDP Answer ───► Client B
         ◄─── SDP Answer ──── Database ◄─── SDP Offer ────
              ICE Candidates ↔↔↔↔↔↔↔↔↔↔↔↔↔ ICE Candidates
                          ↓ P2P Connected ↓
                    ◄──── Media Stream ────►
```

### Gọi nhóm — Mesh P2P

Với N người, mỗi participant quản lý **N-1 PeerConnection** đồng thời:

```
    A ──── B
    │ ╲  ╱ │
    │  ╲╱  │
    │  ╱╲  │
    │ ╱  ╲ │
    C ──── D
    (4 người = 6 kết nối P2P)
```

---

## 📁 Cấu trúc dự án

```
Secure-Chat/
├── SecureChat/                  # Module Android chính
│   └── app/src/main/
│       ├── java/.../
│       │   ├── auth/            # Authentication module
│       │   ├── chat/            # Chat & messaging module
│       │   ├── call/            # WebRTC video call module
│       │   ├── notification/    # FCM notification module
│       │   ├── data/            # Repositories & data sources
│       │   ├── domain/          # Use cases
│       │   └── ui/              # Composable screens
│       └── res/
├── signaling-server/            # (Tham khảo) Signaling server
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 💻 Yêu cầu hệ thống

| Thành phần | Yêu cầu |
|-----------|---------|
| Android SDK | minSdk **26** (Android 8.0+), targetSdk 34 |
| IDE | Android Studio Hedgehog 2023.1.1 trở lên |
| JDK | Java 17+ |
| Kotlin | 1.9.x |
| Thiết bị test | Android 8.0+ (API 26+) |

---

## ⚙️ Cài đặt & Chạy dự án

### 1. Clone repository

```bash
git clone https://github.com/minh566/Secure-Chat.git
cd Secure-Chat
```

### 2. Cấu hình Firebase (xem phần bên dưới)

### 3. Mở dự án

Mở Android Studio → **Open** → chọn thư mục `Secure-Chat/`

### 4. Build & Run

```bash
# Qua terminal
./gradlew :SecureChat:app:installDebug

# Hoặc nhấn Run ▶ trong Android Studio
```

---

## 🔥 Cấu hình Firebase

> ⚠️ File `google-services.json` **không được commit** vào repository vì lý do bảo mật. Bạn cần tạo dự án Firebase riêng.

**Bước 1:** Tạo dự án mới tại [Firebase Console](https://console.firebase.google.com/)

**Bước 2:** Thêm ứng dụng Android với package name phù hợp, tải về `google-services.json` và đặt vào `SecureChat/app/`

**Bước 3:** Kích hoạt các dịch vụ sau trong Firebase Console:

```
Authentication    → Email/Password + Google Sign-In
Realtime Database → Tạo database (region: us-central1)
Storage           → Kích hoạt Firebase Storage
Cloud Messaging   → Mặc định đã bật
```

**Bước 4:** Cấu hình Firebase Security Rules cho Realtime Database:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": {
      "$uid": {
        ".write": "$uid === auth.uid"
      }
    },
    "messages": {
      "$convId": {
        ".read": "root.child('conversations').child($convId).child('members').child(auth.uid).exists()",
        ".write": "root.child('conversations').child($convId).child('members').child(auth.uid).exists()"
      }
    }
  }
}
```

**Bước 5:** Cấu hình STUN/TURN server trong `WebRTCConfig.kt` (nếu cần TURN riêng):

```kotlin
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    // Thêm TURN server của bạn nếu có:
    PeerConnection.IceServer.builder("turn:your-turn-server.com")
        .setUsername("username")
        .setPassword("password")
        .createIceServer()
)
```

---

## 📱 Giao diện ứng dụng

| Đăng nhập | Đăng ký | Chat | Gọi video |
|:---------:|:-------:|:----:|:---------:|
| Màn hình đăng nhập với Email/Password | Form tạo tài khoản mới | Bubble chat phân biệt gửi/nhận, react emoji | Gọi video P2P với điều khiển camera/micro |

**Các màn hình chính:**
- 🏠 **Home** — Danh sách cuộc trò chuyện, badge tin chưa đọc
- 💬 **Chat** — Lịch sử tin nhắn, gửi file/ảnh, seen receipt, emoji reaction
- 📹 **Video Call** — Grid layout đa người, điều khiển camera/micro/loa
- 👥 **Tạo nhóm** — Chọn thành viên, đặt tên và ảnh nhóm
- ⚙️ **Cài đặt** — Dark mode, thông báo, chỉnh sửa hồ sơ

---

## 👨‍💻 Thành viên nhóm

| Thành viên | MSSV | Vai trò | Đóng góp |
|-----------|------|---------|---------|
| **Tôn Thất Nhật Minh** | 3120223117 | Kiến trúc tổng thể (MVVM, Clean Architecture), quản lý tiến độ | 20% |
| **Võ Xuân Tấn** | 3120223178 | Logic nền tảng, tích hợp FCM, xử lý cuộc gọi đến | 20% |
| **Dương Phú Nhật** | 3120223133 | Thiết kế & lập trình toàn bộ UI/UX (Jetpack Compose) | 20% |
| **Nguyễn Thượng Lợi** | 3120223107 | WebRTC P2P, luồng kết nối mạng, cấu hình STUN/TURN | 20% |
| **Phạm Xuân Nguyên** | 3120223133 | Firebase Realtime Database, cấu trúc dữ liệu, Storage | 20% |

**Giảng viên hướng dẫn:** TS. Nguyễn Hoàng Hải  
**Trường:** Đại học Sư phạm Đà Nẵng — Khoa Toán - Tin  
**Năm học:** 2025–2026


- [Stream WebRTC Android SDK](https://getstream.io/video/docs/android/)

---

<p align="center">
  Made with ❤️ by <strong>Nhóm N4</strong> — Khoa Toán-Tin, ĐH Sư phạm Đà Nẵng
</p>
