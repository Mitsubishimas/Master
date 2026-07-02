package com.mastermitsu.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mastermitsu.app.R
import com.mastermitsu.app.data.api.ApiClient
import com.mastermitsu.app.data.models.ForumTopic
import kotlinx.coroutines.*

class ForumFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private val topics = mutableListOf<ForumTopic>()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_forum, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val title: TextView = v.findViewById(R.id.topicTitle)
                val desc: TextView = v.findViewById(R.id.topicLastMessage)
                val count: TextView = v.findViewById(R.id.topicReplies)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_forum_topic, p, false))
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val vh = h as VH
                val t = topics[pos]
                vh.title.text = t.name
                vh.desc.text = t.description
                vh.count.text = "💬 ${t.msgCount}"
                vh.itemView.setOnClickListener {
                    startActivity(Intent(context, TopicMessagesActivity::class.java).apply {
                        putExtra("cat_id", t.id)
                        putExtra("cat_name", t.name)
                    })
                }
            }
            override fun getItemCount() = topics.size
        }
        
        loadTopics()
        return view
    }
    
    private fun loadTopics() {
        progressBar.visibility = ProgressBar.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = ApiClient.instance.getForumTopics()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    r.data?.let {
                        topics.clear(); topics.addAll(it)
                        recyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = ProgressBar.GONE }
            }
        }
    }
}
