package com.mastermitsu.app.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mastermitsu.app.R

class ChatFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_placeholder, container, false)
        view.findViewById<TextView>(R.id.textTitle).text = "Личные сообщения"
        view.findViewById<TextView>(android.R.id.text1)?.text = "Войдите в аккаунт\nдля просмотра сообщений"
        return view
    }
}
