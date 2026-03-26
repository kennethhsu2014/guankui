package com.guankui

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*

class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val packageName = sbn.packageName

                // 过滤掉自己的通知
                if (packageName == applicationContext.packageName) {
                    Log.d("NotificationListener", "过滤自己的通知")
                    return@launch
                }

                val appName = getAppName(packageName)
                val notification = sbn.notification
                val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                val time = sbn.postTime

                Log.d("NotificationListener", "收到通知：package=$packageName, app=$appName, title=$title")

                // 优先使用 BIG_TEXT（长文本），其次使用 TEXT
                val content = if (bigText.isNotEmpty()) bigText else text

                // 保存消息到数据库
                if (content.isNotEmpty()) {
                    val message = NotificationMessage(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        content = content,
                        timestamp = time
                    )
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.messageDao().insert(message)
                    Log.d("NotificationListener", "已保存到数据库：$appName - $title")
                } else {
                    Log.d("NotificationListener", "通知内容为空，跳过")
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "处理通知失败", e)
                e.printStackTrace()
            }
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "服务已连接")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("NotificationListener", "服务已断开连接")
    }
}
