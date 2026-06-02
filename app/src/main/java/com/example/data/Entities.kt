package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val recipient: String,
    val scheduledTime: String,
    val isCritical: Boolean = false,
    val category: String = "General"
)

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val recipient: String,
    val dosage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val loggedBy: String = "Sarah"
)

@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: String,
    val avatarLetter: String,
    val dob: String = "",
    val height: String = "",
    val weight: String = ""
)

@Entity(tableName = "shared_profiles")
data class SharedProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarLetter: String,
    val role: String,
    val sharingType: String = "FULL", // "FULL" (unrestricted/parent), "DURATION" (for X hours), "SCHEDULED" (specific hours daily)
    val durationHours: Int? = null,
    val scheduleTimeStart: String? = null, // e.g. "09:00 AM"
    val scheduleTimeEnd: String? = null,   // e.g. "05:00 PM"
    val startTimestamp: Long = System.currentTimeMillis()
)
