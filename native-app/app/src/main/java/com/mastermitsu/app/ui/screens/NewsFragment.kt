package com.mastermitsu.app.ui.screens

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mastermitsu.app.R
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class NewsFragment : Fragment() {
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerView)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        
        val news = mutableListOf<Map<String, Any?>>()
        rv.layoutManager = LinearLayoutManager(context)
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val title: TextView = v.findViewById(R.id.newsTitle)
                val content: TextView = v.findViewById(R.id.newsContent)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_news, p, false))
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val vh = h as VH
                val n = news[pos]
                vh.title.text = n["title"]?.toString() ?: ""
                vh.content.text = Html.fromHtml(n["content"]?.toString()?.take(300) ?: "", Html.FROM_HTML_MODE_COMPACT)
            }
            override fun getItemCount() = news.size
        }
        rv.adapter = adapter
        
        fun load() {
            swipe.isRefreshing = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val text = java.net.URL("https://mastermitsu.ru/api/news").readText()
                    val map = GsonBuilder().setLenient().create()
                        .fromJson<Map<String, Any?>>(text, object : TypeToken<Map<String, Any?>>() {}.type)
                    val data = map["data"] as? List<Map<String, Any?>> ?: emptyList()
                    withContext(Dispatchers.Main) {
                        news.clear(); news.addAll(data); adapter.notifyDataSetChanged()
                        swipe.isRefreshing = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { swipe.isRefreshing = false }
                }
            }
        }
        
        swipe.setOnRefreshListener { load() }
        load()
        return view
    }
}
