package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.LocalStudyLink
import com.example.data.local.StudyLinkDao
import com.example.data.model.StudyLink
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyRepository(
    private val db: FirebaseFirestore,
    private val localDao: StudyLinkDao
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        AppDatabase.getDatabase(context.applicationContext).studyLinkDao()
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing study links.")
    }

    private fun currentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    fun observeStudyLinks(subjectFilter: String? = null): Flow<List<StudyLink>> = flow {
        val baseQuery: Query = db.collection("study_links")
        val filteredQuery = if (!subjectFilter.isNullOrBlank() && subjectFilter != "All") {
            baseQuery.whereEqualTo("subject", subjectFilter)
        } else {
            baseQuery
        }

        emitAll(
            filteredQuery.snapshots()
                .map { snapshot ->
                    val links = snapshot.toObjects(StudyLink::class.java)
                    // Cache to local Room DB for offline viewing
                    try {
                        val localEntities = links.map { LocalStudyLink.fromStudyLink(it) }
                        localDao.insertAll(localEntities)
                    } catch (ignored: Exception) {
                    }
                    links
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "study_links")

                    // Fallback to Room offline cache
                    val fallbackFlow = if (!subjectFilter.isNullOrBlank() && subjectFilter != "All") {
                        localDao.getLinksBySubject(subjectFilter)
                    } else {
                        localDao.getAllLinks()
                    }
                    emitAll(fallbackFlow.map { list -> list.map { it.toStudyLink() } })
                }
        )
    }

    suspend fun addStudyLink(
        title: String,
        url: String,
        subject: String,
        description: String,
        snapshotContent: String
    ): Result<String> {
        val uid = requireUserId()
        val email = currentUserEmail()
        val docRef = db.collection("study_links").document()

        val payload = mapOf(
            "title" to title.trim(),
            "url" to url.trim(),
            "subject" to subject.trim(),
            "description" to description.trim(),
            "snapshotContent" to snapshotContent.trim(),
            "authorEmail" to email,
            "authorId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val createdLink = StudyLink(
                id = docRef.id,
                title = title.trim(),
                url = url.trim(),
                subject = subject.trim(),
                description = description.trim(),
                snapshotContent = snapshotContent.trim(),
                authorEmail = email,
                authorId = uid
            )
            localDao.insert(LocalStudyLink.fromStudyLink(createdLink))
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStudyLink(link: StudyLink): Result<Unit> {
        requireUserId()
        require(link.id.isNotBlank()) { "StudyLink ID must not be blank for update" }
        val docRef = db.collection("study_links").document(link.id)

        val payload = mapOf(
            "title" to link.title.trim(),
            "url" to link.url.trim(),
            "subject" to link.subject.trim(),
            "description" to link.description.trim(),
            "snapshotContent" to link.snapshotContent.trim(),
            "authorEmail" to link.authorEmail,
            "authorId" to link.authorId,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(payload).await()
            localDao.insert(LocalStudyLink.fromStudyLink(link))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyLink(linkId: String): Result<Unit> {
        requireUserId()
        require(linkId.isNotBlank()) { "StudyLink ID cannot be blank for deletion" }
        val docRef = db.collection("study_links").document(linkId)

        return try {
            docRef.delete().await()
            localDao.deleteById(linkId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCachedSnapshot(linkId: String): String {
        return try {
            val doc = db.collection("study_links").document(linkId).get().await()
            doc.getString("snapshotContent") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
