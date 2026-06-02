package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY isCritical DESC, scheduledTime ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Int)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}

@Dao
interface SharedProfileDao {
    @Query("SELECT * FROM shared_profiles")
    fun getAllProfiles(): Flow<List<SharedProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SharedProfile)

    @Delete
    suspend fun deleteProfile(profile: SharedProfile)
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children ORDER BY name ASC")
    fun getAllChildren(): Flow<List<Child>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: Child)

    @Delete
    suspend fun deleteChild(child: Child)

    @Query("DELETE FROM children WHERE id = :id")
    suspend fun deleteChildById(id: Int)
}
