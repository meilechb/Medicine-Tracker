package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    // NOTE: ordering is handled in the ViewModel by parsed clock time. Sorting by the
    // scheduledTime string in SQL is incorrect ("11:30 AM" would sort before "8:00 AM").
    @Query("SELECT * FROM medications")
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

    // Removes the most recent log for a medication. Used to "undo" a dose that was
    // just logged (e.g. via the Undo snackbar action or tapping a Taken card).
    @Query("DELETE FROM dose_logs WHERE id = (SELECT id FROM dose_logs WHERE medicationId = :medId ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatestLogForMed(medId: Int)
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
