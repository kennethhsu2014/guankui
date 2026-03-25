package com.guankui

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Date

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName
            val appName = getAppName(packageName)
            val notification = sbn.notification
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val time = sbn.postTime

            // 过滤掉自己的通知
            if (packageName == applicationContext.packageName) {
                return
            }

            // 保存消息到数据库
            if (text.isNotEmpty()) {
                val message = NotificationMessage(
                    appName = appName,
                    packageName = packageName,
                    title = title,
                    content = text,
                    timestamp = time
                )
                val db = AppDatabase.getDatabase(applicationContext)
                db.messageDao().insert(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 通知移除时不做处理
    }
}
