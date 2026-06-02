package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FamilyDoseRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val sharedProfileDao: SharedProfileDao,
    private val childDao: ChildDao
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()
    val allProfiles: Flow<List<SharedProfile>> = sharedProfileDao.getAllProfiles()
    val allChildren: Flow<List<Child>> = childDao.getAllChildren()

    init {
        // Safe asynchronous seeding on dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        val meds = medicationDao.getAllMedications().firstOrNull() ?: emptyList()
        if (meds.isEmpty()) {
            medicationDao.insertMedication(
                Medication(
                    name = "Amoxicillin",
                    dosage = "3.5ml",
                    recipient = "Leo",
                    scheduledTime = "8:00 AM",
                    isCritical = true,
                    category = "Antibiotic"
                )
            )
            medicationDao.insertMedication(
                Medication(
                    name = "Vitamin D Drop",
                    dosage = "1 drop",
                    recipient = "Maya",
                    scheduledTime = "11:30 AM",
                    isCritical = false,
                    category = "Vitamin"
                )
            )
            medicationDao.insertMedication(
                Medication(
                    name = "Claritin Kids",
                    dosage = "5ml",
                    recipient = "Leo",
                    scheduledTime = "8:00 PM",
                    isCritical = false,
                    category = "Allergy"
                )
            )
        }

        val kids = childDao.getAllChildren().firstOrNull() ?: emptyList()
        if (kids.isEmpty()) {
            childDao.insertChild(
                Child(
                    name = "Leo",
                    age = "4 years old",
                    avatarLetter = "L"
                )
            )
            childDao.insertChild(
                Child(
                    name = "Maya",
                    age = "2 years old",
                    avatarLetter = "M"
                )
            )
        }

        val profiles = sharedProfileDao.getAllProfiles().firstOrNull() ?: emptyList()
        if (profiles.isEmpty()) {
            sharedProfileDao.insertProfile(
                SharedProfile(
                    name = "Sarah",
                    avatarLetter = "S",
                    role = "Mother / Admin",
                    sharingType = "FULL"
                )
            )
            sharedProfileDao.insertProfile(
                SharedProfile(
                    name = "David",
                    avatarLetter = "D",
                    role = "Father / Co-Parent",
                    sharingType = "FULL"
                )
            )
            sharedProfileDao.insertProfile(
                SharedProfile(
                    name = "Chloe",
                    avatarLetter = "C",
                    role = "Babysitter",
                    sharingType = "DURATION",
                    durationHours = 4,
                    startTimestamp = System.currentTimeMillis()
                )
            )
            sharedProfileDao.insertProfile(
                SharedProfile(
                    name = "Grandma Ellen",
                    avatarLetter = "G",
                    role = "Caretaker",
                    sharingType = "SCHEDULED",
                    scheduleTimeStart = "08:00 AM",
                    scheduleTimeEnd = "02:00 PM"
                )
            )
        }
    }

    suspend fun insertMedication(medication: Medication) = medicationDao.insertMedication(medication)
    suspend fun updateMedication(medication: Medication) = medicationDao.updateMedication(medication)
    suspend fun deleteMedication(medication: Medication) = medicationDao.deleteMedication(medication)
    suspend fun deleteMedicationById(id: Int) = medicationDao.deleteMedicationById(id)

    suspend fun insertLog(log: DoseLog) = doseLogDao.insertLog(log)
    suspend fun deleteLogById(id: Int) = doseLogDao.deleteLogById(id)
    suspend fun deleteLatestLogForMed(medId: Int) = doseLogDao.deleteLatestLogForMed(medId)

    suspend fun insertProfile(profile: SharedProfile) = sharedProfileDao.insertProfile(profile)
    suspend fun deleteProfile(profile: SharedProfile) = sharedProfileDao.deleteProfile(profile)

    suspend fun insertChild(child: Child) = childDao.insertChild(child)
    suspend fun deleteChild(child: Child) = childDao.deleteChild(child)
}
