package com.attention.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceId"]), Index(value = ["url"], unique = true)]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: Long,
    val title: String,
    val url: String,
    val publishedAt: Long,
    val fetchedAt: Long,
    val excerpt: String,
    val importanceScore: Int? = null,
    val topicTag: String,
    val coverageCount: Int = 1,
    val dedupeGroupId: String? = null,
    val expiresAt: Long
)
