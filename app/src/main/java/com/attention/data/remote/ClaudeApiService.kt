package com.attention.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("v1/messages")
    suspend fun getMessage(@Body request: ClaudeRequest): ClaudeResponse
}
