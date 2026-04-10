package com.securechat.data.remote.signaling

data class TurnCredentials(
    val urls: List<String>,
    val username: String,
    val credential: String,
    val ttl: Long
)

