package com.guankui

import androidx.room.*

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): List<NotificationMessage>

    @Query("SELECT * FROM messages WHERE appName = :appName ORDER BY timestamp DESC")
    fun getMessagesByApp(appName: String): List<NotificationMessage>

    @Query("SELECT DISTINCT appName FROM messages ORDER BY appName")
    fun getAllAppNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: NotificationMessage)

    @Query("DELETE FROM messages")
    fun deleteAll()
}
