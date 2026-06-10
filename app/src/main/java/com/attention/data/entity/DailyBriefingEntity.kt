package com.attention.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_briefings")
data class DailyBriefingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // format: YYYY-MM-DD
    val briefingText: String,
    val significanceLevel: Int, // 1-3
    val topStoryTitles: String, // JSON list of strings
    val generatedAt: Long,
    val status: String = "FULL" // FULL, UNRANKED, FAILED
)
