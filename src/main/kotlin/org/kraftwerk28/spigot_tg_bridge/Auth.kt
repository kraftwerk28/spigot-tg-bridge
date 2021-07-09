package org.kraftwerk28.spigot_tg_bridge

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Auth() {
    var conn: Connection? = null

    suspend fun connect() {
        try {
            val connString = "jdbc:sqlite:spigot-tg-bridge.sqlite"
            val initDbQuery = """
                create table if not exists ign_links (
                    telegram_id bigint,
                    telegram_username varchar,
                    minecraft_ign varchar,
                    linked_timestamp int
                )
            """
            conn = DriverManager.getConnection(connString).apply {
                createStatement().execute(initDbQuery)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}
