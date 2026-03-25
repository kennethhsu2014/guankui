package com.guankui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messageList = mutableListOf<NotificationMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting content view", e)
            Toast.makeText(this, "布局加载失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            setSupportActionBar(findViewById(R.id.toolbar))
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting toolbar", e)
        }

        try {
            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = MessageAdapter(messageList)
            recyclerView.adapter = adapter

            val fab: FloatingActionButton = findViewById(R.id.fab)
            fab.setOnClickListener {
                showClearConfirmDialog()
            }

            // 检查通知监听权限
            if (!isNotificationServiceEnabled()) {
                showEnableNotificationDialog()
            }

            loadMessages()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        return try {
            val packageName = packageName
            val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(packageName)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking notification service", e)
            false
        }
    }

    private fun showEnableNotificationDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("需要开启通知监听权限")
                .setMessage("管窥需要读取通知栏消息才能工作。请在设置中开启通知监听权限。")
                .setPositiveButton("去设置") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("稍后") { _, _ -> }
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing dialog", e)
        }
    }

    private fun showClearConfirmDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("确定要清空所有消息记录吗？此操作不可恢复。")
                .setPositiveButton("清空") { _, _ ->
                    clearAllMessages()
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing dialog", e)
        }
    }

    private fun loadMessages() {
        try {
            val db = AppDatabase.getDatabase(this)
            messageList.clear()
            messageList.addAll(db.messageDao().getAllMessages())
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading messages", e)
            Toast.makeText(this, "加载消息失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearAllMessages() {
        try {
            val db = AppDatabase.getDatabase(this)
            db.messageDao().deleteAll()
            messageList.clear()
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error clearing messages", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (isNotificationServiceEnabled()) {
                loadMessages()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_filter -> {
                    showFilterDialog()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onOptionsItemSelected", e)
            false
        }
    }

    private fun showFilterDialog() {
        try {
            val db = AppDatabase.getDatabase(this)
            val appNames = db.messageDao().getAllAppNames().distinct()

            if (appNames.isEmpty()) {
                Toast.makeText(this, "暂无消息记录", Toast.LENGTH_SHORT).show()
                return
            }

            AlertDialog.Builder(this)
                .setTitle("按 APP 筛选")
                .setItems(appNames.toTypedArray()) { _, which ->
                    val selectedApp = appNames[which]
                    filterByApp(selectedApp)
                }
                .setNegativeButton("取消", null)
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing filter dialog", e)
            Toast.makeText(this, "加载筛选列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterByApp(appName: String) {
        try {
            val db = AppDatabase.getDatabase(this)
            messageList.clear()
            messageList.addAll(db.messageDao().getMessagesByApp(appName))
            adapter.notifyDataSetChanged()
            supportActionBar?.subtitle = "筛选：$appName"
        } catch (e: Exception) {
            Log.e("MainActivity", "Error filtering by app", e)
        }
    }
}
