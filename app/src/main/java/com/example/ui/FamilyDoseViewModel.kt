package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.ScheduleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyDoseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FamilyDoseRepository(
        db.medicationDao(),
        db.doseLogDao(),
        db.sharedProfileDao(),
        db.childDao()
    )

    // UI state flows. Medications are ordered chronologically by their parsed schedule time
    // (sorting by the raw string in SQL is incorrect).
    val medications: StateFlow<List<Medication>> = repository.allMedications
        .map { meds -> meds.sortedBy { ScheduleUtils.timeToMinutes(it.scheduledTime) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doseLogs: StateFlow<List<DoseLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<SharedProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val children: StateFlow<List<Child>> = repository.allChildren
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of medicationId -> timestamp of its most recent dose logged *today*. Derived from
    // the dose logs so a medication's "taken today" state is non-destructive and resets daily.
    val takenTodayMap: StateFlow<Map<Int, Long>> = repository.allLogs
        .map { logs ->
            logs.filter { ScheduleUtils.isToday(it.timestamp) }
                .groupBy { it.medicationId }
                .mapValues { (_, entries) -> entries.maxOf { it.timestamp } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Current navigation tab state (0: Home, 1: Kids, 2: Account)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Dialog state
    private val _showAddMedDialog = MutableStateFlow(false)
    val showAddMedDialog: StateFlow<Boolean> = _showAddMedDialog.asStateFlow()

    private val _showAddProfileDialog = MutableStateFlow(false)
    val showAddProfileDialog: StateFlow<Boolean> = _showAddProfileDialog.asStateFlow()

    private val _showAddChildDialog = MutableStateFlow(false)
    val showAddChildDialog: StateFlow<Boolean> = _showAddChildDialog.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setShowAddMedDialog(show: Boolean) {
        _showAddMedDialog.value = show
    }

    fun setShowAddProfileDialog(show: Boolean) {
        _showAddProfileDialog.value = show
    }

    fun setShowAddChildDialog(show: Boolean) {
        _showAddChildDialog.value = show
    }

    fun logMedication(medication: Medication) {
        viewModelScope.launch {
            val log = DoseLog(
                medicationId = medication.id,
                medicationName = medication.name,
                recipient = medication.recipient,
                dosage = medication.dosage,
                loggedBy = "Sarah" // Default current user
            )
            repository.insertLog(log)
            // The medication's "taken today" state is derived from the dose logs (see
            // takenTodayMap), so we deliberately do NOT mutate the medication row here.
            // This keeps the recurring schedule intact for future days.
        }
    }

    /** Undoes the most recent dose logged for this medication (e.g. an accidental tap). */
    fun undoLogForMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteLatestLogForMed(medication.id)
        }
    }

    fun addNewMedication(
        name: String,
        dosage: String,
        recipient: String,
        scheduledTime: String,
        isCritical: Boolean,
        category: String
    ) {
        viewModelScope.launch {
            val med = Medication(
                name = name,
                dosage = dosage,
                recipient = recipient,
                scheduledTime = scheduledTime,
                isCritical = isCritical,
                category = category
            )
            repository.insertMedication(med)
            _showAddMedDialog.value = false
        }
    }

    /** Updates an existing medication's details, preserving its id (and thus its dose history). */
    fun updateMedicationDetails(
        id: Int,
        name: String,
        dosage: String,
        recipient: String,
        scheduledTime: String,
        isCritical: Boolean,
        category: String
    ) {
        viewModelScope.launch {
            repository.updateMedication(
                Medication(
                    id = id,
                    name = name,
                    dosage = dosage,
                    recipient = recipient,
                    scheduledTime = scheduledTime,
                    isCritical = isCritical,
                    category = category
                )
            )
        }
    }

    fun addNewChild(name: String, age: String, dob: String, height: String, weight: String) {
        viewModelScope.launch {
            val kid = Child(
                name = name,
                age = age,
                avatarLetter = name.firstOrNull()?.uppercase() ?: "K",
                dob = dob,
                height = height,
                weight = weight
            )
            repository.insertChild(kid)
            _showAddChildDialog.value = false
        }
    }

    fun updateChildInfo(id: Int, name: String, age: String, dob: String, height: String, weight: String) {
        viewModelScope.launch {
            val kid = Child(
                id = id,
                name = name,
                age = age,
                avatarLetter = name.firstOrNull()?.uppercase() ?: "K",
                dob = dob,
                height = height,
                weight = weight
            )
            repository.insertChild(kid)
        }
    }

    fun addNewProfile(
        name: String,
        role: String,
        sharingType: String,
        durationHours: Int? = null,
        scheduleTimeStart: String? = null,
        scheduleTimeEnd: String? = null
    ) {
        viewModelScope.launch {
            val profile = SharedProfile(
                name = name,
                avatarLetter = name.firstOrNull()?.uppercase() ?: "C",
                role = role,
                sharingType = sharingType,
                durationHours = durationHours,
                scheduleTimeStart = scheduleTimeStart,
                scheduleTimeEnd = scheduleTimeEnd,
                startTimestamp = System.currentTimeMillis()
            )
            repository.insertProfile(profile)
            _showAddProfileDialog.value = false
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    fun deleteLog(logId: Int) {
        viewModelScope.launch {
            repository.deleteLogById(logId)
        }
    }

    fun deleteProfile(profile: SharedProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun deleteChild(child: Child) {
        viewModelScope.launch {
            repository.deleteChild(child)
        }
    }

    // Factory Class for direct instantiation using modern ViewModelProvider
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FamilyDoseViewModel::class.java)) {
                return FamilyDoseViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
