package com.mastermitsu.app.ui.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mastermitsu.app.R
import com.mastermitsu.app.data.api.ApiClient
import com.mastermitsu.app.data.models.ChatMessage
import kotlinx.coroutines.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class ChatMessagesActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private var userId = 0
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_messages)
        
        userId = intent.getIntExtra("user_id", 0)
        title = intent.getStringExtra("user_name") ?: "Чат"
        
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        prefs = getSharedPreferences("master_prefs", MODE_PRIVATE)
        
        adapter = ChatMessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        loadMessages()
        btnSend.setOnClickListener { sendMessage() }
    }
    
    private fun loadMessages() {
        val token = prefs.getString("user_token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = ApiClient.instance.getChatMessages(token, userId)
                withContext(Dispatchers.Main) {
                    r.data?.let {
                        messages.clear()
                        messages.addAll(it)
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {}
        }
    }
    
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        val token = prefs.getString("user_token", "") ?: ""
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.instance.sendChatMessage(token, mapOf("user_id" to userId, "message" to text))
                withContext(Dispatchers.Main) {
                    etMessage.text.clear()
                    loadMessages()
                }
            } catch (e: Exception) {}
        }
    }
    
    inner class ChatMessageAdapter(private val items: List<ChatMessage>) : RecyclerView.Adapter<ChatMessageAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val author: TextView = view.findViewById(R.id.msgAuthor)
            val text: TextView = view.findViewById(R.id.msgText)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
        override fun onBindViewHolder(holder: VH, pos: Int) {
            val m = items[pos]
            holder.author.text = if (m.senderId == prefs.getInt("user_id", 0)) "Вы" else "Собеседник"
            holder.text.text = m.message
        }
        override fun getItemCount() = items.size
    }
}
