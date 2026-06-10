package com.attention.data.remote

import com.google.gson.annotations.SerializedName

data class ClaudeRequest(
    @SerializedName("model") val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    @SerializedName("messages") val messages: List<ClaudeMessage>,
    @SerializedName("system") val system: String? = null
)

data class ClaudeMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ClaudeResponse(
    @SerializedName("id") val id: String,
    @SerializedName("content") val content: List<ClaudeContentBlock>,
    @SerializedName("model") val model: String,
    @SerializedName("stop_reason") val stopReason: String?
)

data class ClaudeContentBlock(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
)
