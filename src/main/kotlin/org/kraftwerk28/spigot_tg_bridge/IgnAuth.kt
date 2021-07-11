package org.kraftwerk28.spigot_tg_bridge

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

const val INIT_DB_QUERY = """
create table if not exists user (
    tg_id bigint not null primary key,
    tg_username varchar,
    tg_first_name varchar not null,
    tg_last_name varchar,
    mc_username varchar,
    mc_uuid varchar,
    linked_timestamp datetime default current_timestamp
);
"""

class IgnAuth(
    fileName: String,
    private val plugin: Plugin,
    private var conn: Connection? = null,
) {
    init {
        plugin.launch {
            initializeConnection(fileName)
        }
    }

    private fun initializeConnection(fileName: String) = try {
        DriverManager.getConnection("jdbc:sqlite:$fileName").apply {
            createStatement().execute(INIT_DB_QUERY)
        }.also {
            conn = it
        }
    } catch (e: SQLException) {
        plugin.logger.info(e.message)
    }

    fun close() = conn?.run {
        close()
    }

    fun linkUser(
        tgId: Long,
        tgUsername: String? = null,
        tgFirstName: String,
        tgLastName: String? = null,
        minecraftUsername: String?,
        minecraftUuid: String,
    ): Boolean = conn?.stmt(
        """
        insert into user (
            tg_id,
            tg_username,
            tg_first_name,
            tg_last_name,
            mc_uuid,
            mc_username,
        )
        values (?, ?, ?, ?, ?, ?)
        """,
        tgId,
        tgUsername,
        tgFirstName,
        tgLastName,
        minecraftUuid,
        minecraftUsername,
    )?.run {
        execute()
    } ?: false

    fun getLinkedUserByIgn(ign: String) =
        conn?.stmt("select * from user where mc_uuid = ?", ign)?.first {
            toLinkedUser()
        }

    fun getLinkedUserByTgId(id: Long) =
        conn?.stmt("select * from user where tg_id = ?", id)?.first {
            toLinkedUser()
        }

    fun unlinkUserByTgId(id: Long) =
        conn?.stmt("delete from user where tg_id = ?", id)?.run {
            executeUpdate() > 0
        }

    fun getAllLinkedUsers() =
        conn?.stmt("select * from user")?.map { toLinkedUser() }
}
