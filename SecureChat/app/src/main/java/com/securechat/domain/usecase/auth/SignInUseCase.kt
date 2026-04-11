package com.securechat.domain.usecase.auth

import com.securechat.domain.repository.AuthRepository
import com.securechat.domain.model.AuthResult
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank())
            return AuthResult.Error("Email và mật khẩu không được để trống")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return AuthResult.Error("Email không hợp lệ")
        return authRepository.signIn(email, password)
    }
}
