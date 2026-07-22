package com.kiro.arcade.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ZeroTierRepository {

    private const val API_URL = "https://api.zerotier.com/api/v1"
    // Токен хранится здесь — не публикуй на GitHub!
    private const val API_TOKEN = "lHssbbdAoIgi9VSn0wa3eUihxghaum7i"

    suspend fun createNetwork(name: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL/network")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "token $API_TOKEN")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("name", name)
                    put("private", true)
                    put("v4AssignMode", JSONObject().apply { put("zt", true) })
                })
            }.toString()

            conn.outputStream.write(body.toByteArray())
            conn.outputStream.flush()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response).getString("id")
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun deleteNetwork(networkId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL/network/$networkId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("Authorization", "token $API_TOKEN")
            conn.responseCode == 200
        } catch (e: Exception) { false }
    }

    suspend fun authorizeDevice(networkId: String, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL/network/$networkId/member/$deviceId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "token $API_TOKEN")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("config", JSONObject().apply { put("authorized", true) })
            }.toString()

            conn.outputStream.write(body.toByteArray())
            conn.responseCode == 200
        } catch (e: Exception) { false }
    }

    suspend fun getNetworkMembers(networkId: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL/network/$networkId/member")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "token $API_TOKEN")
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                org.json.JSONArray(response).length()
            } else 0
        } catch (e: Exception) { 0 }
    }
}
