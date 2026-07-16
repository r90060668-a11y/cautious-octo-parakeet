package com.kiro.arcade.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val fcmToken: String = ""
)

data class Post(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val likesCount: Int = 0,
    val timestamp: Long = 0L
)

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val otherUserUid: String = "",
    val otherUsername: String = "",
    val otherAvatarUrl: String = ""
)
