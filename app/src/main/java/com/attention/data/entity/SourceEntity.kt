package com.attention.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val feedUrl: String,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val lastFetched: Long = 0,
    val faviconUrl: String? = null
)
