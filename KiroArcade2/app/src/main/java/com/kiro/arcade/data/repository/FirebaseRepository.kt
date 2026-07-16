package com.kiro.arcade.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            val user = User(uid = uid, username = username)
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
            db.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun getCurrentUser(): User? = currentUid?.let { getUser(it) }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(User::class.java)
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

    suspend fun followUser(targetUid: String) {
        val uid = currentUid ?: return
        val batch = db.batch()

        // Add to following
        batch.set(
            db.collection("users").document(uid).collection("following").document(targetUid),
            mapOf("uid" to targetUid)
        )
        // Add to followers
        batch.set(
            db.collection("users").document(targetUid).collection("followers").document(uid),
            mapOf("uid" to uid)
        )
        // Update counts
        batch.update(db.collection("users").document(uid), "followingCount",
            com.google.firebase.firestore.FieldValue.increment(1))
        batch.update(db.collection("users").document(targetUid), "followersCount",
            com.google.firebase.firestore.FieldValue.increment(1))
        batch.commit().await()

        // Check if mutual follow → create chat
        val theyFollowMe = db.collection("users").document(targetUid)
            .collection("following").document(uid).get().await().exists()
        if (theyFollowMe) {
            createChatIfNeeded(uid, targetUid)
        }

        // Send notification
        sendFollowNotification(targetUid)
    }

    suspend fun unfollowUser(targetUid: String) {
        val uid = currentUid ?: return
        val batch = db.batch()
        batch.delete(db.collection("users").document(uid).collection("following").document(targetUid))
        batch.delete(db.collection("users").document(targetUid).collection("followers").document(uid))
        batch.update(db.collection("users").document(uid), "followingCount",
            com.google.firebase.firestore.FieldValue.increment(-1))
        batch.update(db.collection("users").document(targetUid), "followersCount",
            com.google.firebase.firestore.FieldValue.increment(-1))
        batch.commit().await()
    }

    private suspend fun createChatIfNeeded(uid1: String, uid2: String) {
        val chatId = if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        val chatRef = db.collection("chats").document(chatId)
        if (!chatRef.get().await().exists()) {
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
            .addSnapshotListener { snap, _ ->
                val posts = snap?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(caption: String, imageUrl: String) {
        val uid = currentUid ?: return
        val user = getUser(uid) ?: return
        val post = Post(
            id = db.collection("posts").document().id,
            authorUid = uid,
            authorUsername = user.username,
            authorAvatarUrl = user.avatarUrl,
            imageUrl = imageUrl,
            caption = caption,
            timestamp = System.currentTimeMillis()
        )
        db.collection("posts").document(post.id).set(post).await()
    }

    suspend fun searchPosts(query: String): List<Post> {
        return try {
            db.collection("posts")
                .whereGreaterThanOrEqualTo("caption", query)
                .whereLessThanOrEqualTo("caption", query + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(Post::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Chats ─────────────────────────────────────────────────────────────────

    fun getChats(): Flow<List<Chat>> = callbackFlow {
        val uid = currentUid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val chats = snap?.documents?.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                    val otherUid = participants.firstOrNull { it != uid }?.toString() ?: return@mapNotNull null
                    Chat(
                        id = doc.id,
                        participants = participants.map { it.toString() },
                        lastMessage = doc.getString("lastMessage") ?: "",
                        lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                        otherUserUid = otherUid
                    )
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val messages = snap?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, text: String) {
        val uid = currentUid ?: return
        val msg = Message(
            id = db.collection("chats").document(chatId).collection("messages").document().id,
            senderUid = uid,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        db.collection("chats").document(chatId).collection("messages").document(msg.id).set(msg).await()
        db.collection("chats").document(chatId).update(
            "lastMessage", text,
            "lastMessageTime", msg.timestamp
        ).await()
    }
}
