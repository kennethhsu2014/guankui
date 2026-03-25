package com.guankui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
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
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

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
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun showEnableNotificationDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要开启通知监听权限")
            .setMessage("管窥需要读取通知栏消息才能工作。请在设置中开启通知监听权限。")
            .setPositiveButton("去设置") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("稍后") { _, _ -> }
            .show()
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("清空历史记录")
            .setMessage("确定要清空所有消息记录吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                clearAllMessages()
            }
            .setNegativeButton("取消") { _, _ -> }
            .show()
    }

    private fun loadMessages() {
        // 从数据库加载消息
        val db = AppDatabase.getDatabase(this)
        messageList.clear()
        messageList.addAll(db.messageDao().getAllMessages())
        adapter.notifyDataSetChanged()
    }

    private fun clearAllMessages() {
        val db = AppDatabase.getDatabase(this)
        db.messageDao().deleteAll()
        messageList.clear()
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        if (isNotificationServiceEnabled()) {
            loadMessages()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val db = AppDatabase.getDatabase(this)
        val appNames = db.messageDao().getAllAppNames().distinct()

        AlertDialog.Builder(this)
            .setTitle("按 APP 筛选")
            .setItems(appNames.toTypedArray()) { _, which ->
                val selectedApp = appNames[which]
                filterByApp(selectedApp)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun filterByApp(appName: String) {
        val db = AppDatabase.getDatabase(this)
        messageList.clear()
        messageList.addAll(db.messageDao().getMessagesByApp(appName))
        adapter.notifyDataSetChanged()
        supportActionBar?.subtitle = "筛选：$appName"
    }
}
