package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.StudyLink

@Entity(tableName = "cached_study_links")
data class LocalStudyLink(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val subject: String,
    val description: String,
    val snapshotContent: String,
    val authorEmail: String,
    val authorId: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toStudyLink(): StudyLink {
        return StudyLink(
            id = id,
            title = title,
            url = url,
            subject = subject,
            description = description,
            snapshotContent = snapshotContent,
            authorEmail = authorEmail,
            authorId = authorId
        )
    }

    companion object {
        fun fromStudyLink(link: StudyLink): LocalStudyLink {
            return LocalStudyLink(
                id = link.id,
                title = link.title,
                url = link.url,
                subject = link.subject,
                description = link.description,
                snapshotContent = link.snapshotContent,
                authorEmail = link.authorEmail,
                authorId = link.authorId,
                createdAtMillis = link.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAtMillis = link.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
    }
}
