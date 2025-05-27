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

    // Utility functions for role and status checking
    fun isAdmin(): Boolean = role == UserRole.ADMIN
    fun isUser(): Boolean = role == UserRole.USER
    fun isApproved(): Boolean = status == UserStatus.APPROVED
    fun isPending(): Boolean = status == UserStatus.PENDING_APPROVAL
    fun isRejected(): Boolean = status == UserStatus.REJECTED
    
    // Check if user can perform admin actions
    fun canManageUsers(): Boolean = isAdmin() && isApproved()
    
    // Check if user can access the app
    fun canAccessApp(): Boolean = isApproved()
    
    // Get display name
    fun getDisplayName(): String = username.ifEmpty { email.substringBefore("@") }

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