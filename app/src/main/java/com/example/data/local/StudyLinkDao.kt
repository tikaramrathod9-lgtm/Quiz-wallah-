package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLinkDao {
    @Query("SELECT * FROM cached_study_links ORDER BY createdAtMillis DESC")
    fun getAllLinks(): Flow<List<LocalStudyLink>>

    @Query("SELECT * FROM cached_study_links WHERE subject = :subject ORDER BY createdAtMillis DESC")
    fun getLinksBySubject(subject: String): Flow<List<LocalStudyLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<LocalStudyLink>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: LocalStudyLink)

    @Query("DELETE FROM cached_study_links WHERE id = :linkId")
    suspend fun deleteById(linkId: String)

    @Query("DELETE FROM cached_study_links")
    suspend fun clearAll()
}
