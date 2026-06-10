package com.attention.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.attention.data.entity.ArticleEntity
import com.attention.data.entity.SourceEntity

data class ArticleWithSource(
    @Embedded val article: ArticleEntity,
    @Relation(
        parentColumn = "sourceId",
        entityColumn = "id"
    )
    val source: SourceEntity
)
