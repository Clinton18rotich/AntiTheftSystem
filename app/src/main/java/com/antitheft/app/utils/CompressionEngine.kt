package com.antitheft.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*

class CompressionEngine {
    fun compressImage(inputFile: File, quality: Int = 70): File {
        val outputFile = File(inputFile.parent, "compressed_${inputFile.name}")
        try {
            val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
            val outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.close()
            bitmap.recycle()
            return outputFile
        } catch (e: Exception) { return inputFile }
    }

    fun compressText(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(outputStream).use { it.write(data) }
        return outputStream.toByteArray()
    }

    companion object {
        fun fastCompress(data: Any): Any = data
        fun standardCompress(data: Any): Any = data
        fun aggressiveCompress(data: Any): Any = data
        fun maximumCompression(data: Any): Any = data
        fun emergencyCompression(data: Any): ByteArray = ByteArray(0)
    }
}
