package com.securechat.domain.usecase.call

import com.securechat.domain.model.CallSession
import com.securechat.domain.repository.CallRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIncomingCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    operator fun invoke(userId: String): Flow<CallSession?> =
        callRepository.observeIncomingCall(userId)
}
