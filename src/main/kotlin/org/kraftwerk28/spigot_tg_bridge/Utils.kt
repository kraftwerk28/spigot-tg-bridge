package org.kraftwerk28.spigot_tg_bridge

import com.vdurmont.emoji.EmojiParser
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

fun String.escapeHtml() = this
    .replace("&", "&amp;")
    .replace(">", "&gt;")
    .replace("<", "&lt;")

fun String.escapeHTML() = this
    .replace("&", "&amp;")
    .replace(">", "&gt;")
    .replace("<", "&lt;")

fun String.escapeColorCodes() = replace("\u00A7.".toRegex(), "")

fun String.fullEscape() = escapeHTML().escapeColorCodes()

fun String.escapeEmoji() = EmojiParser.parseToAliases(this)

fun User.rawUserMention(): String =
    (if (firstName.length < 2) null else firstName)
        ?: username
        ?: lastName!!

fun DbLinkedUser.fullName() = tgFirstName + (tgLastName?.let { " $it" } ?: "")

fun Connection.stmt(query: String, vararg args: Any?): PreparedStatement =
    prepareStatement(query).apply {
        args.zip(1..args.size).forEach { (arg, i) ->
            when (arg) {
                is String -> setString(i, arg)
                is Long -> setLong(i, arg)
                is Int -> setInt(i, arg)
            }
        }
    }

fun checkMinecraftLicense(playerUuid: String): Boolean = try {
    val urlString = "https://api.mojang.com/user/profiles/$playerUuid/names"
    val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
    }
    conn.responseCode == 200
} catch (e: Exception) {
    e.printStackTrace()
    false
}

fun getMinecraftUuidByUsername(username: String): String? = try {
    val urlString = "https://api.mojang.com/users/profiles/minecraft/$username"
    val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
    }
    val regex = "\"name\"\\s*:\\s*\"(\\w+)\"".toRegex()
    val body = BufferedReader(conn.inputStream.reader()).readText()
    regex.matchEntire(body)?.groupValues?.get(1)
} catch (e: Exception) {
    e.printStackTrace()
    null
}

fun ResultSet.toLinkedUser() = DbLinkedUser(
    tgId = getLong("tg_id"),
    tgUsername = getString("tg_username"),
    tgFirstName = getString("tg_first_name"),
    tgLastName = getString("tg_last_name"),
    createdTimestamp = getDate("linked_timestamp"),
    minecraftUsername = getString("mc_username"),
    minecraftUuid = getString("mc_uuid"),
)

fun <T> PreparedStatement.first(convertFn: ResultSet.() -> T): T? {
    val result = executeQuery()
    return if (result.next()) {
        result.convertFn()
    } else {
        null
    }
}

fun <T> PreparedStatement.map(convertFn: ResultSet.() -> T): List<T> {
    val resultSet = executeQuery()
    val result = mutableListOf<T>()
    while (resultSet.next()) {
        result.add(resultSet.convertFn())
    }
    return result
}
