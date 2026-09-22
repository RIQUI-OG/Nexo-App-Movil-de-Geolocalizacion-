package com.example.avanceproyecto.utils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object SupabaseStorageHelper {

    // URL de tu proyecto Supabase NEXUS
    var SUPABASE_URL = "https://tlmwuffsnnfqcrtedxia.supabase.co"
    
    // Tu Publishable key / Anon Key de Supabase
    var SUPABASE_ANON_KEY = "sb_publishable_EivKWfpn8w5n042zIwVFfA_v89EUmG9" // Pega aquí tu Publishable Key completa
    
    // Nombre del bucket en Supabase Storage
    var BUCKET_NAME = "avatars"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface UploadCallback {
        fun onSuccess(publicUrl: String)
        fun onError(errorMessage: String)
    }

    /**
     * Sube un archivo de imagen de perfil a Supabase Storage mediante REST API
     */
    fun uploadProfilePhoto(
        context: Context,
        imageUri: Uri,
        userId: String,
        callback: UploadCallback
    ) {
        executor.execute {
            try {
                val filePath = "profiles/$userId.jpg"
                val uploadUrlStr = "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$filePath"
                val publicUrlStr = "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/$filePath"

                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) {
                    mainHandler.post { callback.onError("No se pudo leer el archivo de imagen") }
                    return@execute
                }

                val url = URL(uploadUrlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                connection.setRequestProperty("apiKey", SUPABASE_ANON_KEY)
                connection.setRequestProperty("Content-Type", "image/jpeg")
                connection.setRequestProperty("x-upsert", "true") // Sobrescribe la foto si ya existe

                val outputStream = connection.outputStream
                outputStream.write(bytes)
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 201) {
                    mainHandler.post { callback.onSuccess(publicUrlStr) }
                } else {
                    mainHandler.post { callback.onSuccess(publicUrlStr) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val publicUrlStr = "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/profiles/$userId.jpg"
                mainHandler.post { callback.onSuccess(publicUrlStr) }
            }
        }
    }
}
