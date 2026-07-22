package com.kiro.arcade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kiro.arcade.data.model.Chat
import com.kiro.arcade.data.model.Message
import com.kiro.arcade.data.model.Post
import com.kiro.arcade.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUid get() = auth.currentUser?.uid

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user!!.uid
            val user = hashMapOf(
                "uid" to uid,
                "username" to username,
                "bio" to "",
                "avatarUrl" to "",
                "followersCount" to 0,
                "followingCount" to 0,
                "fcmToken" to ""
            )
            db.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUser(uid: String): User? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            User(
                uid = doc.getString("uid") ?: uid,
                username = doc.getString("username") ?: "",
                bio = doc.getString("bio") ?: "",
                avatarUrl = doc.getString("avatarUrl") ?: "",
                followersCount = (doc.getLong("followersCount") ?: 0L).toInt(),
                followingCount = (doc.getLong("followingCount") ?: 0L).toInt(),
                fcmToken = doc.getString("fcmToken") ?: ""
            )
        } catch (e: Exception) { null }
    }

    suspend fun getCurrentUser(): User? = currentUid?.let { getUser(it) }

    suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        return try {
            db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get().await()
                .documents.mapNotNull { doc ->
                    User(
                        uid = doc.getString("uid") ?: "",
                        username = doc.getString("username") ?: "",
                        bio = doc.getString("bio") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        followersCount = (doc.getLong("followersCount") ?: 0L).toInt(),
                        followingCount = (doc.getLong("followingCount") ?: 0L).toInt()
                    )
                }
        } catch (e: Exception) { emptyList() }
    }

    // ── Follow ────────────────────────────────────────────────────────────────

    suspend fun isFollowing(targetUid: String): Boolean {
        val uid = currentUid ?: return false
        return try {
            db.collection("users").document(uid)
                .collection("following").document(targetUid)
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    suspend fun followUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            // Добавить в following текущего пользователя
            db.collection("users").document(uid)
                .collection("following").document(targetUid)
                .set(mapOf("uid" to targetUid)).await()

            // Добавить в followers целевого пользователя
            db.collection("users").document(targetUid)
                .collection("followers").document(uid)
                .set(mapOf("uid" to uid)).await()

            // Обновить счётчики через merge чтобы не упасть если поля нет
            db.collection("users").document(uid)
                .set(mapOf("followingCount" to FieldValue.increment(1)), SetOptions.merge()).await()

            db.collection("users").document(targetUid)
                .set(mapOf("followersCount" to FieldValue.increment(1)), SetOptions.merge()).await()

            // Проверить взаимную подписку → создать чат
            val theyFollowMe = db.collection("users").document(targetUid)
                .collection("following").document(uid).get().await().exists()
            if (theyFollowMe) {
                createChatIfNeeded(uid, targetUid)
            }

            // Уведомление
            try { sendFollowNotification(targetUid) } catch (e: Exception) { /* не критично */ }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            db.collection("users").document(uid)
                .collection("following").document(targetUid).delete().await()

            db.collection("users").document(targetUid)
                .collection("followers").document(uid).delete().await()

            db.collection("users").document(uid)
                .set(mapOf("followingCount" to FieldValue.increment(-1)), SetOptions.merge()).await()

            db.collection("users").document(targetUid)
                .set(mapOf("followersCount" to FieldValue.increment(-1)), SetOptions.merge()).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createChatIfNeeded(uid1: String, uid2: String) {
        val chatId = if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        val chatRef = db.collection("chats").document(chatId)
        val exists = try { chatRef.get().await().exists() } catch (e: Exception) { false }
        if (!exists) {
            chatRef.set(mapOf(
                "participants" to listOf(uid1, uid2),
                "lastMessage" to "",
                "lastMessageTime" to System.currentTimeMillis()
            )).await()
        }
    }

    private suspend fun sendFollowNotification(targetUid: String) {
        val me = getCurrentUser() ?: return
        db.collection("notifications").add(mapOf(
            "toUid" to targetUid,
            "fromUid" to (currentUid ?: ""),
            "fromUsername" to me.username,
            "type" to "follow",
            "timestamp" to System.currentTimeMillis()
        )).await()
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    fun getPostsFeed(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val posts = snap?.documents?.mapNotNull { doc ->
                    try {
                        Post(
                            id = doc.id,
                            authorUid = doc.getString("authorUid") ?: "",
                            authorUsername = doc.getString("authorUsername") ?: "",
                            authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            caption = doc.getString("caption") ?: "",
                            likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(caption: String, imageUrl: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            val user = getUser(uid)
            val postRef = db.collection("posts").document()
            val post = hashMapOf(
                "id" to postRef.id,
                "authorUid" to uid,
                "authorUsername" to (user?.username ?: ""),
                "authorAvatarUrl" to (user?.avatarUrl ?: ""),
                "imageUrl" to imageUrl,
                "caption" to caption,
                "likesCount" to 0,
                "timestamp" to System.currentTimeMillis()
            )
            postRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPosts(query: String): List<Post> {
        if (query.isBlank()) return emptyList()
        return try {
            db.collection("posts")
                .whereGreaterThanOrEqualTo("caption", query)
                .whereLessThanOrEqualTo("caption", query + "\uf8ff")
                .limit(20)
                .get().await()
                .documents.mapNotNull { doc ->
                    try {
                        Post(
                            id = doc.id,
                            authorUid = doc.getString("authorUid") ?: "",
                            authorUsername = doc.getString("authorUsername") ?: "",
                            authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            caption = doc.getString("caption") ?: "",
                            likesCount = (doc.getLong("likesCount") ?: 0L).toInt(),
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
        } catch (e: Exception) { emptyList() }
    }

    // ── Chats ─────────────────────────────────────────────────────────────────

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val uid = currentUid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val chats = snap?.documents?.mapNotNull { doc ->
                    try {
                        val participants = (doc.get("participants") as? List<*>)
                            ?.map { it.toString() } ?: return@mapNotNull null
                        val otherUid = participants.firstOrNull { it != uid } ?: return@mapNotNull null
                        Chat(
                            id = doc.id,
                            participants = participants,
                            lastMessage = doc.getString("lastMessage") ?: "",
                            lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                            otherUserUid = otherUid
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val messages = snap?.documents?.mapNotNull { doc ->
                    try {
                        Message(
                            id = doc.id,
                            senderUid = doc.getString("senderUid") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, text: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            val msgRef = db.collection("chats").document(chatId).collection("messages").document()
            val msg = hashMapOf(
                "id" to msgRef.id,
                "senderUid" to uid,
                "text" to text,
                "timestamp" to System.currentTimeMillis()
            )
            msgRef.set(msg).await()
            db.collection("chats").document(chatId).set(
                mapOf("lastMessage" to text, "lastMessageTime" to System.currentTimeMillis()),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

    // ── Hosts ─────────────────────────────────────────────────────────────────

    suspend fun createHost(name: String, version: String, networkId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            val user = getUser(uid)
            val ref = db.collection("hosts").document()
            val host = hashMapOf(
                "id" to ref.id,
                "name" to name,
                "version" to version,
                "networkId" to networkId,
                "ownerUid" to uid,
                "ownerUsername" to (user?.username ?: ""),
                "playerCount" to 0,
                "maxPlayers" to 25,
                "online" to true,
                "timestamp" to System.currentTimeMillis()
            )
            ref.set(host).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHosts(): List<com.kiro.arcade.ui.screens.HostInfo> {
        return try {
            db.collection("hosts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
                .documents.mapNotNull { doc ->
                    try {
                        com.kiro.arcade.ui.screens.HostInfo(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            version = doc.getString("version") ?: "",
                            networkId = doc.getString("networkId") ?: "",
                            ownerUid = doc.getString("ownerUid") ?: "",
                            ownerUsername = doc.getString("ownerUsername") ?: "",
                            playerCount = (doc.getLong("playerCount") ?: 0L).toInt(),
                            maxPlayers = (doc.getLong("maxPlayers") ?: 25L).toInt(),
                            online = doc.getBoolean("online") ?: true,
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteHost(hostId: String): Result<Unit> {
        return try {
            db.collection("hosts").document(hostId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Profile update ────────────────────────────────────────────────────────

    suspend fun updateProfile(username: String, bio: String, avatarUrl: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            db.collection("users").document(uid).set(
                mapOf("username" to username, "bio" to bio, "avatarUrl" to avatarUrl),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Likes ─────────────────────────────────────────────────────────────────

    suspend fun likePost(postId: String, like: Boolean): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Не авторизован"))
        return try {
            if (like) {
                db.collection("posts").document(postId).set(
                    mapOf("likesCount" to com.google.firebase.firestore.FieldValue.increment(1)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                db.collection("posts").document(postId)
                    .collection("likes").document(uid).set(mapOf("uid" to uid)).await()
            } else {
                db.collection("posts").document(postId).set(
                    mapOf("likesCount" to com.google.firebase.firestore.FieldValue.increment(-1)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                db.collection("posts").document(postId)
                    .collection("likes").document(uid).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
