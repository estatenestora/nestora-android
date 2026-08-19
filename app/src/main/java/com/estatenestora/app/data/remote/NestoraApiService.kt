package com.estatenestora.app.data.remote

import com.estatenestora.app.data.model.TelegramGetUpdatesResponse
import com.estatenestora.app.data.model.TelegramSendRequest
import com.estatenestora.app.data.model.TelegramSendResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

private const val DEV1_BOT_TOKEN = "8939541613:AAFusIkBL173Lw_nHoNlHqAi2RzDWOtuYO4"

interface TelegramServerBotService {

    @POST("bot$DEV1_BOT_TOKEN/sendMessage")
    suspend fun sendMessage(@Body request: TelegramSendRequest): Response<TelegramSendResponse>

    @GET("bot$DEV1_BOT_TOKEN/getUpdates")
    suspend fun getUpdates(
        @Query("offset") offset: Long? = null,
        @Query("timeout") timeout: Int = 0,
        @Query("limit") limit: Int = 10
    ): Response<TelegramGetUpdatesResponse>
}
