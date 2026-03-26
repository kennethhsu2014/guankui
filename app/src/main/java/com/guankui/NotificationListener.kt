package com.guankui

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*

class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedKeys = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        serviceScope.launch {
            try {
                val packageName = sbn.packageName
                val appName = getAppName(packageName)

                Log.d("NotificationListener", "收到通知：package=$packageName, app=$appName")

                // 过滤掉自己的通知
                if (packageName == applicationContext.packageName) {
                    Log.d("NotificationListener", "过滤自己的通知")
                    return@launch
                }

                // 获取该应用的所有活跃通知（处理通知组）
                val activeNotifications = getActiveNotifications(arrayOf(packageName))
                Log.d("NotificationListener", "活跃通知数量：${activeNotifications.size}")

                for (activeSbn in activeNotifications) {
                    try {
                        val notificationKey = activeSbn.key
                        val notification = activeSbn.notification
                        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                        val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                        val time = activeSbn.postTime

                        // 使用通知 key 去重
                        if (processedKeys.contains(notificationKey)) {
                            Log.d("NotificationListener", "跳过已处理的通知：$notificationKey")
                            continue
                        }
                        processedKeys.add(notificationKey)

                        // 优先使用 BIG_TEXT（长文本），其次使用 TEXT
                        val content = if (bigText.isNotEmpty()) bigText else text

                        Log.d("NotificationListener", "通知 key=$notificationKey, title=$title, content=$content")

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
                        Log.e("NotificationListener", "处理单条通知失败", e)
                    }
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
}
