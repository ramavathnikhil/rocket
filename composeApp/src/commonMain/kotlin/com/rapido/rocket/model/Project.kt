package com.rapido.rocket.model

data class Project(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val repositoryUrl: String = "",
    val playStoreUrl: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isActive: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "repositoryUrl" to repositoryUrl,
            "playStoreUrl" to playStoreUrl,
            "createdBy" to createdBy,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "isActive" to isActive
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Project {
            return Project(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                repositoryUrl = map["repositoryUrl"] as? String ?: "",
                playStoreUrl = map["playStoreUrl"] as? String ?: "",
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = map["createdAt"] as? Long ?: 0L,
                updatedAt = map["updatedAt"] as? Long ?: 0L,
                isActive = map["isActive"] as? Boolean ?: true
            )
        }
    }
} 