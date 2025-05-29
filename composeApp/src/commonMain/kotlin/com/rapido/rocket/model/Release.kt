package com.rapido.rocket.model

enum class ReleaseStatus {
    DRAFT,
    IN_PROGRESS,
    STAGING,
    PRODUCTION_PENDING,
    PRODUCTION,
    COMPLETED,
    CANCELLED
}

data class Release(
    val id: String = "",
    val projectId: String = "",
    val version: String = "",
    val title: String = "",
    val description: String = "",
    val status: ReleaseStatus = ReleaseStatus.DRAFT,
    val createdBy: String = "",
    val assignedTo: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val targetReleaseDate: Long? = null,
    val actualReleaseDate: Long? = null,
    val stagingBuildUrl: String = "",
    val productionBuildUrl: String = "",
    val githubReleaseUrl: String = "",
    val playStoreUrl: String = "",
    val notes: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "projectId" to projectId,
            "version" to version,
            "title" to title,
            "description" to description,
            "status" to status.name,
            "createdBy" to createdBy,
            "assignedTo" to assignedTo,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "targetReleaseDate" to (targetReleaseDate ?: ""),
            "actualReleaseDate" to (actualReleaseDate ?: ""),
            "stagingBuildUrl" to stagingBuildUrl,
            "productionBuildUrl" to productionBuildUrl,
            "githubReleaseUrl" to githubReleaseUrl,
            "playStoreUrl" to playStoreUrl,
            "notes" to notes
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Release {
            return Release(
                id = map["id"] as? String ?: "",
                projectId = map["projectId"] as? String ?: "",
                version = map["version"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                status = try {
                    ReleaseStatus.valueOf(map["status"] as? String ?: "DRAFT")
                } catch (e: Exception) {
                    ReleaseStatus.DRAFT
                },
                createdBy = map["createdBy"] as? String ?: "",
                assignedTo = map["assignedTo"] as? String ?: "",
                createdAt = map["createdAt"] as? Long ?: 0L,
                updatedAt = map["updatedAt"] as? Long ?: 0L,
                targetReleaseDate = map["targetReleaseDate"].let { 
                    if (it is Long) it else null 
                },
                actualReleaseDate = map["actualReleaseDate"].let { 
                    if (it is Long) it else null 
                },
                stagingBuildUrl = map["stagingBuildUrl"] as? String ?: "",
                productionBuildUrl = map["productionBuildUrl"] as? String ?: "",
                githubReleaseUrl = map["githubReleaseUrl"] as? String ?: "",
                playStoreUrl = map["playStoreUrl"] as? String ?: "",
                notes = map["notes"] as? String ?: ""
            )
        }
    }
} 