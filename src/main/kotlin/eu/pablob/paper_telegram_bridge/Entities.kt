package eu.pablob.paper_telegram_bridge

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
    val caption: String? = null,
    @Name("reply_markup") val replyMarkup: InlineKeyboardMarkup? = null, // Inline keyboard attached to the message
    @Name("is_topic_message") val isTopicMessage: Boolean? = null,
    @Name("message_thread_id") val messageThreadId: Long? = null,
    val photo: List<Any>? = null,
    val video: Any? = null,
    val audio: Any? = null,
    val voice: Any? = null,
    @Name("video_note") val videoNote: Any? = null,
)

data class Update(
    @Name("update_id") val updateId: Long,
    val message: Message? = null,
    @Name("callback_query") val callbackQuery: CallbackQuery? = null
)

data class CallbackQuery(
    val id: String, // Unique identifier for the callback query
    val from: User, // User who clicked the button
    val message: Message?, // Message that contained the inline keyboard
    @Name("chat_instance") val chatInstance: String?, // Unique identifier for the chat instance
    val data: String? // Data associated with the button
)

data class InlineKeyboardMarkup(
    @Name("inline_keyboard") val inlineKeyboard: List<List<InlineKeyboardButton>>
)

data class InlineKeyboardButton(
    val text: String, // Label text on the button
    @Name("callback_data") val callbackData: String? = null, // Data to be sent in a callback query
    val url: String? = null // Optional URL for URL buttons
)

data class BotCommand(val command: String, val description: String)

data class SetMyCommands(val commands: List<BotCommand>)
