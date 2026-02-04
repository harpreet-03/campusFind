package com.example.finder

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

object ImageHash {
    fun generatePHash(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        resized.getPixels(pixels, 0, 8, 0, 0, 8, 8)

        val avg = pixels.map { Color.luminance(it) }.average()
        var hash = 0L
        for (pixel in pixels) {
            val lum = Color.luminance(pixel)
            if (lum > avg) hash = (hash shl 1) or 1
            else hash = hash shl 1
        }
        return hash.toString(16).padStart(16, '0')
    }

    fun hammingDistance(hash1: String, hash2: String): Int {
        val h1 = hash1.toLong(16)
        val h2 = hash2.toLong(16)
        return Integer.bitCount((h1 xor h2).toInt())
    }

    fun isSimilar(hash1: String, hash2: String): Boolean = hammingDistance(hash1, hash2) <= 8
}
