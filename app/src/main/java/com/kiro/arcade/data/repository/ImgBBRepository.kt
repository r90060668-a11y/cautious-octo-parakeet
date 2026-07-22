package com.kiro.arcade.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

object ImgBBRepository {

    private const val API_KEY = "4bc3ddd6b33913a1504007f72dbab6ff"
    private const val API_URL = "https://api.imgbb.com/1/upload"

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Read and compress image
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Не удалось открыть файл"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            // Upload to ImgBB
            val url = URL("$API_URL?key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val body = "image=${java.net.URLEncoder.encode(base64Image, "UTF-8")}"
            conn.outputStream.write(body.toByteArray())
            conn.outputStream.flush()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val imageUrl = json.getJSONObject("data").getString("url")
                Result.success(imageUrl)
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "Ошибка загрузки"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImageFromBytes(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            val url = URL("$API_URL?key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val body = "image=${java.net.URLEncoder.encode(base64Image, "UTF-8")}"
            conn.outputStream.write(body.toByteArray())
            conn.outputStream.flush()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val imageUrl = json.getJSONObject("data").getString("url")
                Result.success(imageUrl)
            } else {
                Result.failure(Exception("Ошибка загрузки: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
