package com.guankui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val FOREGROUND_SERVICE_ID = 1
    private val CHANNEL_ID = "listener_service"

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

    /**
     * 创建前台服务通知通道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "通知监听服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "管窥通知监听服务正在运行"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("NotificationListener", "通知通道已创建")
        }
    }

    /**
     * 启动前台服务，提升服务优先级防止被系统杀死
     */
    private fun startForegroundService() {
        try {
            createNotificationChannel()
            
            // 创建点击通知打开 MainActivity 的 PendingIntent
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("管窥运行中")
                .setContentText("正在监听通知消息")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
            
            startForeground(FOREGROUND_SERVICE_ID, notification)
            Log.d("NotificationListener", "前台服务已启动")
        } catch (e: Exception) {
            Log.e("NotificationListener", "启动前台服务失败", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 通知移除时不做处理
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.w("NotificationListener", "服务已销毁")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "服务已连接")
        // 启动前台服务保活
        startForegroundService()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("NotificationListener", "服务已断开连接，请求重启")
        // 请求系统重新绑定服务（尝试重启）
        requestRebind()
    }
}
