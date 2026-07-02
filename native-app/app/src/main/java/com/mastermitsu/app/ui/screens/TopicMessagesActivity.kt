package com.mastermitsu.app.ui.screens

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mastermitsu.app.R
import com.mastermitsu.app.data.api.ApiClient
import com.mastermitsu.app.data.models.ForumMessage
import kotlinx.coroutines.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class TopicMessagesActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private var catId = 0
    private val messages = mutableListOf<ForumMessage>()
    private lateinit var adapter: MessageAdapter
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_messages)
        
        catId = intent.getIntExtra("cat_id", 0)
        title = intent.getStringExtra("cat_name") ?: "Тема"
        
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        prefs = getSharedPreferences("master_prefs", MODE_PRIVATE)
        
        adapter = MessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        loadMessages()
        btnSend.setOnClickListener { sendMessage() }
    }
    
    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getForumMessages(catId)
                withContext(Dispatchers.Main) {
                    response.data?.let {
                        messages.clear()
                        messages.addAll(it)
                        adapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("Topic", "Ошибка загрузки", e)
            }
        }
    }
    
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        
        val token = prefs.getString("user_token", "") ?: ""
        Log.d("Topic", "Отправка с токеном: $token")
        
        btnSend.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf("cat_id" to catId, "message" to text)
                ApiClient.instance.sendForumMessage(token, body)
                withContext(Dispatchers.Main) {
                    btnSend.isEnabled = true
                    etMessage.text.clear()
                    loadMessages()
                }
            } catch (e: Exception) {
                Log.e("Topic", "Ошибка отправки", e)
                withContext(Dispatchers.Main) {
                    btnSend.isEnabled = true
                    Toast.makeText(this@TopicMessagesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    inner class MessageAdapter(private val items: List<ForumMessage>) : RecyclerView.Adapter<MessageAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val author: TextView = view.findViewById(R.id.msgAuthor)
            val text: TextView = view.findViewById(R.id.msgText)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
        override fun onBindViewHolder(holder: VH, pos: Int) {
            val m = items[pos]
            holder.author.text = m.author
            holder.text.text = m.message
        }
        override fun getItemCount() = items.size
    }
}
