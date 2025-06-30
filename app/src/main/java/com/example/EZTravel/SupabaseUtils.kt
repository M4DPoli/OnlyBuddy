package com.example.EZTravel

import android.content.Context
import android.net.Uri
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

//Funzione per ottenere un ByteArray data un stringa che contiene un uri ottenuto da ImagePicker o
// CameraX
 fun uriStringToByteArray(context: Context, uriString: String): ByteArray? {
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

//Funzione per caricare immagine su Supabase a cui passare un filename (generato casualmente tramite
// UUID.random().toString e il ByteArray ottenuto con la funzione precedente
suspend fun uploadImageToSupabase(
    fileName: String,
    fileBytes: ByteArray,
    projectUrl: String = "https://jruryuhuktltrghqveno.supabase.co",
    anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpydXJ5dWh1a3RsdHJnaHF2ZW5vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg5Mzk1NDAsImV4cCI6MjA2NDUxNTU0MH0.Ga1kxHQJyztFPzJW5ULIFVMy7dNFbYrUUI8dQc_5Gjo",
    bucketName: String = "photos"
): Boolean {
    val client = HttpClient(CIO)

    val url = "$projectUrl/storage/v1/object/$bucketName/$fileName?upload=1"

    return try {
        val response = client.post(url) {
            headers {
                append("Authorization", "Bearer $anonKey")
                append("Content-Type", "image/jpeg")
            }
            setBody(fileBytes)
        }
        response.status.value in 200..299
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        client.close()
    }
}

//Funzione per eliminare una funzione da Supabase e serve solo il filename che si ottiene tramtite
// l'url pubblico che deve essere sul db e si fa con url.subStringAfterLast("/")
suspend fun deleteImageFromSupabase(
    fileName: String,
    projectUrl: String = "https://jruryuhuktltrghqveno.supabase.co",
    anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpydXJ5dWh1a3RsdHJnaHF2ZW5vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg5Mzk1NDAsImV4cCI6MjA2NDUxNTU0MH0.Ga1kxHQJyztFPzJW5ULIFVMy7dNFbYrUUI8dQc_5Gjo",
    bucketName: String = "photos"
): Boolean {
    val client = HttpClient(CIO)
    val url = "$projectUrl/storage/v1/object/$bucketName/$fileName"
    return try {
        val response = client.delete(url) {
            headers {
                append("Authorization", "Bearer $anonKey")
            }
        }
        response.status.value in 200..299
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        client.close()
    }
}

//Funzione per ottenere il ByteArray da una foto già caricata su Supabase
suspend fun downloadBytesFromUrl(urlString: String): ByteArray? {
    return try {
        withContext(Dispatchers.IO) {
            URL(urlString).openStream().use { it.readBytes() }
        }
    } catch (e: Exception) {
        Log.e("ImageDownload", "Failed to download image from $urlString", e)
        null
    }
}

//Funzione per ottenere url pubblico di una fotot e bisogan usare il filename generato per caricare la foto
 fun getPublicUrl(
    projectUrl: String = "https://jruryuhuktltrghqveno.supabase.co",
    bucket: String = "photos",
    fileName: String
): String {
    return "$projectUrl/storage/v1/object/public/$bucket/$fileName"
}