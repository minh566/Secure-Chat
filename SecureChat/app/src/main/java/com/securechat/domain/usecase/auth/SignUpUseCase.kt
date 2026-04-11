package com.securechat.domain.usecase.auth

import com.securechat.domain.model.AuthResult
import com.securechat.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        if (displayName.isBlank()) return AuthResult.Error("Tên không được để trống")
        if (password.length < 6)   return AuthResult.Error("Mật khẩu phải ít nhất 6 ký tự")
        return authRepository.signUp(email, password, displayName)
    }
}
