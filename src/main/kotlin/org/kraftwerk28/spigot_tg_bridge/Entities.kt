package org.kraftwerk28.spigot_tg_bridge

import java.sql.Date
import com.google.gson.annotations.SerializedName as Name

data class TgResponse<T>(
    val ok: Boolean,
    val result: T?,
    val description: String?,
)

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

data class DbLinkedUser(
    val tgId: Long,
    val tgFirstName: String,
    val tgLastName: String?,
    val tgUsername: String?,
    val minecraftUuid: String,
    val minecraftUsername: String,
    val createdTimestamp: Date,
)
