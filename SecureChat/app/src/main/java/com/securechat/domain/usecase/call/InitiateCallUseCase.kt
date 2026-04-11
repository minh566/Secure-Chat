package com.securechat.domain.usecase.call

import com.securechat.domain.model.*
import com.securechat.domain.repository.CallRepository
import java.util.*
import javax.inject.Inject

class InitiateCallUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(
        callerId: String,
        calleeId: String,
        roomId: String,
        type: CallType
    ): Resource<Unit> {
        val session = CallSession(
            id        = UUID.randomUUID().toString(),
            callerId  = callerId,
            calleeId  = calleeId,
            roomId    = roomId,
            type      = type,
            status    = CallStatus.RINGING,
            startedAt = Date()
        )
        return callRepository.initiateCall(session)
    }
}
