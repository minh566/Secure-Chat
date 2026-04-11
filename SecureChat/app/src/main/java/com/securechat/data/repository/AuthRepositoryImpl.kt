package com.securechat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.securechat.data.mapper.toUser
import com.securechat.domain.model.AuthResult
import com.securechat.domain.model.Resource
import com.securechat.domain.model.User
import com.securechat.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private fun mapAuthError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "CONFIGURATION_NOT_FOUND" -> {
                        "Firebase Auth chưa được cấu hình đúng (CONFIGURATION_NOT_FOUND). " +
                            "Vào Firebase Console -> Authentication -> Sign-in method và bật Email/Password, " +
                            "đồng thời kiểm tra đúng file google-services.json cho package com.securechat."
                    }
                    else -> "Firebase Auth lỗi ${exception.errorCode}: ${exception.localizedMessage ?: "Không có mô tả"}"
                }
            }
            else -> exception.localizedMessage ?: "Lỗi không xác định"
        }
    }

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.toUser()

    override fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser() ?: return AuthResult.Error("Đăng nhập thất bại")
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return AuthResult.Error("Tạo tài khoản thất bại")

            // Cập nhật displayName
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = displayName
            }
            firebaseUser.updateProfile(profileUpdates).await()

            // Lưu user vào Firestore collection "users"
            val user = User(
                uid = firebaseUser.uid,
                displayName = displayName,
                email = email
            )
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapAuthError(e))
        }
    }

    override suspend fun signOut() {
        // Cập nhật trạng thái offline trước khi đăng xuất
        firebaseAuth.currentUser?.uid?.let { uid ->
            firestore.collection("users")
                .document(uid)
                .update("isOnline", false)
                .await()
        }
        firebaseAuth.signOut()
    }

    override suspend fun updateProfile(displayName: String, photoUrl: String?): Resource<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Resource.Error("Chưa đăng nhập")
            val updates = mutableMapOf<String, Any>("displayName" to displayName)
            photoUrl?.let { updates["photoUrl"] = it }
            firestore.collection("users").document(uid).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cập nhật thất bại")
        }
    }

    override suspend fun updateFcmToken(token: String): Resource<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Resource.Error("Chưa đăng nhập")
            firestore.collection("users")
                .document(uid)
                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cập nhật token thất bại")
        }
    }
}
