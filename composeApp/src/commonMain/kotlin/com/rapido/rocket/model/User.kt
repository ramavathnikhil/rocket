package com.rapido.rocket.model

import com.rapido.rocket.util.currentTimeMillis

enum class UserRole {
    ADMIN,
    USER
}

enum class UserStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
}

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.PENDING_APPROVAL,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "email" to email,
        "username" to username,
        "role" to role.name,
        "status" to status.name,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any>): User = User(
            id = map["id"] as? String ?: "",
            email = map["email"] as? String ?: "",
            username = map["username"] as? String ?: "",
            role = UserRole.valueOf(map["role"] as? String ?: UserRole.USER.name),
            status = UserStatus.valueOf(map["status"] as? String ?: UserStatus.PENDING_APPROVAL.name),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: currentTimeMillis(),
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: currentTimeMillis()
        )
    }
} 