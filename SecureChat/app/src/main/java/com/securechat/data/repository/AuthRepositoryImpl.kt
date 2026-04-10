package com.securechat.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
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

    override val currentUser: User?
        get() = firebaseAuth.currentUser?.toUser()

    override fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser() ?: return AuthResult.Error("Đăng nhập thất bại")
            AuthResult.Success(user)
        } catch (e: FirebaseNetworkException) {
            AuthResult.Error("Lỗi mạng. Hãy kiểm tra kết nối Internet và cấu hình Firebase.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Email hoặc mật khẩu không hợp lệ.")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Lỗi không xác định")
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

            try {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    this.displayName = displayName
                }
                firebaseUser.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Không chặn signup nếu chỉ lỗi cập nhật displayName
            }

            val user = User(
                uid = firebaseUser.uid,
                displayName = displayName,
                email = email
            )

            try {
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
            } catch (e: FirebaseFirestoreException) {
                return when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        AuthResult.Error("Đăng ký được nhưng không lưu được hồ sơ Firestore. Kiểm tra `firestore.rules`.")
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        AuthResult.Error("Đăng ký được nhưng Firestore chưa kết nối. Kiểm tra Internet/Firebase project.")
                    else -> AuthResult.Error(e.localizedMessage ?: "Không lưu được hồ sơ người dùng")
                }
            }

            AuthResult.Success(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            AuthResult.Error("Mật khẩu phải có ít nhất 6 ký tự.")
        } catch (e: FirebaseAuthUserCollisionException) {
            AuthResult.Error("Email này đã được đăng ký.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Email không hợp lệ hoặc mật khẩu không đúng định dạng.")
        } catch (e: FirebaseNetworkException) {
            AuthResult.Error("Không kết nối được Firebase. Kiểm tra Internet, API key hoặc dịch vụ Google trên máy/emulator.")
        } catch (e: FirebaseAuthException) {
            AuthResult.Error("FirebaseAuth(${e.errorCode}): ${e.localizedMessage ?: "Lỗi xác thực"}")
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Lỗi không xác định")
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
                .update("fcmToken", token)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Cập nhật token thất bại")
        }
    }
}
