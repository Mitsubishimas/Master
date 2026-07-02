package com.mastermitsu.app.data.models

import com.google.gson.annotations.SerializedName

// ============ АВТОРИЗАЦИЯ ============
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    val success: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val avatar: String? = null,
    val token: String? = null
)

// ============ НОВОСТИ ============
data class NewsItem(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    val author: String? = null,
    @SerializedName("views_count") val viewsCount: Int = 0,
    @SerializedName("image_url") val imageUrl: String? = null
)

// ============ ФОРУМ ============
data class ForumTopic(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    @SerializedName("msg_count") val msgCount: Int = 0,
    @SerializedName("last_msg_time") val lastMsgTime: Long = 0
)

data class ForumMessage(
    val id: Int = 0,
    @SerializedName("cat_id") val catId: Int = 0,
    val author: String = "",
    val message: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

// ============ ЛИЧНЫЕ СООБЩЕНИЯ ============
data class ChatDialog(
    val id: Int = 0,
    @SerializedName("user_name") val userName: String = "",
    @SerializedName("last_message") val lastMessage: String = "",
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("is_online") val isOnline: Boolean = false
)

data class ChatMessage(
    val id: Int = 0,
    @SerializedName("sender_id") val senderId: Int = 0,
    val message: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("is_read") val isRead: Boolean = false
)

// ============ БАРАХОЛКА ============
data class MarketItem(
    val id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val photos: String? = null,
    @SerializedName("user_id") val userId: Int = 0,
    val city: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

// ============ ОБЩЕЕ ============
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val total: Int? = null,
    val error: String? = null
)
