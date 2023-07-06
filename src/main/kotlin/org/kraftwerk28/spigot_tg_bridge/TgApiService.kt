package org.kraftwerk28.spigot_tg_bridge

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TgApiService {
    @GET("deleteWebhook")
    suspend fun deleteWebhook(
        @Query("drop_pending_updates") dropPendingUpdates: Boolean
    ): TgResponse<Boolean>

    @GET("sendMessage?parse_mode=HTML")
    suspend fun sendMessage(
        @Query("chat_id") chatId: Long,
        @Query("text") text: String,
        @Query("reply_to_message_id") replyToMessageId: Long? = null,
        @Query("disable_notification") disableNotification: Boolean? = null,
//    ): Response<TgResponse<Message>>
    ): TgResponse<Message>

    @GET("getUpdates")
    suspend fun getUpdates(
        @Query("offset") offset: Long,
        @Query("limit") limit: Int = 100,
        @Query("timeout") timeout: Int = 0,
    ): TgResponse<List<Update>>

    @GET("getMe")
    suspend fun getMe(): TgResponse<User>

    @POST("setMyCommands")
    suspend fun setMyCommands(
        @Body commands: SetMyCommands,
    ): TgResponse<Boolean>

    @POST("setWebhook")
    suspend fun setWebhook(
        @Query("url") url: String,
        @Query("allowed_updates") allowedUpdates: List<String> = listOf(),
        @Query("drop_pending_updates") dropPendingUpdates: Boolean = false,
    ): TgResponse<Boolean>
}
