package org.kraftwerk28.spigot_tg_bridge

import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import com.google.gson.annotations.SerializedName as Name
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.time.Duration
import retrofit2.Retrofit

interface TgApiService {
    data class TgResponse<T>(val ok: Boolean, val result: T?, val description: String?)
    data class WebhookOptions(val drop_pending_updates: Boolean)

    data class User(
        @Name("id") val id: Long,
        @Name("is_bot") val isBot: Boolean,
        @Name("first_name") val firstName: String,
        @Name("last_name") val lastName: String? = null,
        @Name("username") val username: String? = null,
        @Name("language_code") val languageCode: String? = null,
    )

    data class Chat(
        val id: Long,
        val type: String,
        val title: String? = null,
        val username: String? = null,
        @Name("first_name") val firstName: String? = null,
        @Name("last_name") val lastName: String? = null,
    )

    data class Message(
        @Name("message_id") val messageId: Long,
        val from: User? = null,
        @Name("sender_chat") val senderChat: Chat? = null,
        val date: Long,
        val chat: Chat,
        @Name("reply_to_message") val replyToMessage: Message? = null,
        val text: String? = null,
    )

    data class Update(
        @Name("update_id") val updateId: Long,
        val message: Message? = null,
    )

    data class BotCommand(val command: String, val description: String)
    data class SetMyCommands(val commands: List<BotCommand>)

    @GET("deleteWebhook")
    suspend fun deleteWebhook(
        @Query("drop_pending_updates") dropPendingUpdates: Boolean
    ): TgResponse<Boolean>

    @GET("sendMessage?parse_mode=HTML")
    suspend fun sendMessage(
        @Query("chat_id") chatId: Long,
        @Query("text") text: String,
        @Query("reply_to_message_id") replyToMessageId: Long? = null,
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

    companion object {
        fun create(token: String): TgApiService {
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE;
            }
            val client = OkHttpClient
                .Builder()
                .addInterceptor(interceptor)
                .readTimeout(Duration.ZERO)
                .build();
            val r = Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot$token/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return r.create(TgApiService::class.java)
        }
    }
}
