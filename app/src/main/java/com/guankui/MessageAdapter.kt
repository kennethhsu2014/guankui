package com.guankui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messages: List<NotificationMessage>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appName: TextView = view.findViewById(R.id.appName)
        val title: TextView = view.findViewById(R.id.title)
        val content: TextView = view.findViewById(R.id.content)
        val time: TextView = view.findViewById(R.id.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.appName.text = message.appName
        holder.title.text = message.title
        holder.content.text = message.content
        holder.time.text = dateFormat.format(Date(message.timestamp))
    }

    override fun getItemCount() = messages.size
}
