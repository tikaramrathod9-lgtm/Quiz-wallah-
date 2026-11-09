package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudyLink(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val subject: String = "Math",
    val description: String = "",
    val snapshotContent: String = "",
    val authorEmail: String = "",
    val authorId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        val VALID_SUBJECTS = listOf("Math", "Physics", "Chemistry", "Hindi", "English")
    }
}
