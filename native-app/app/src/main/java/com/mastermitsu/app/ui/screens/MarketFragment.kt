package com.mastermitsu.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mastermitsu.app.R
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class MarketFragment : Fragment() {
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_market, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.recyclerView)
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        
        val items = mutableListOf<Map<String, Any?>>()
        rv.layoutManager = GridLayoutManager(context, 2)
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val title: TextView = v.findViewById(R.id.itemTitle)
                val price: TextView = v.findViewById(R.id.itemPrice)
                val seller: TextView = v.findViewById(R.id.itemSeller)
            }
            override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_market, p, false))
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val vh = h as VH
                val m = items[pos]
                vh.title.text = m["title"]?.toString() ?: ""
                vh.price.text = "${(m["price"] as? Double)?.toInt() ?: 0} ₽"
                vh.seller.text = m["city"]?.toString() ?: ""
            }
            override fun getItemCount() = items.size
        }
        rv.adapter = adapter
        
        fun load() {
            swipe.isRefreshing = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val text = java.net.URL("https://mastermitsu.ru/api/market").readText()
                    val map = GsonBuilder().setLenient().create()
                        .fromJson<Map<String, Any?>>(text, object : TypeToken<Map<String, Any?>>() {}.type)
                    val data = map["data"] as? List<Map<String, Any?>> ?: emptyList()
                    withContext(Dispatchers.Main) {
                        items.clear(); items.addAll(data); adapter.notifyDataSetChanged()
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
