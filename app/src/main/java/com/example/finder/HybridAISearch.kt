package com.example.finder

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class HybridAISearch(private val context: Context) {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun analyzeImage(uri: Uri): AISearchResult {
        return withContext(Dispatchers.IO) {
            try {
                // 1. ML Kit Labels
                val image = InputImage.fromFilePath(context, uri)
                val labels = labeler.process(image).await()
                    .map { it.text.lowercase(Locale.ROOT) }
                    .filter { it.length > 2 }
                    .take(8)  // Top 8 labels

                // 2. Perceptual Hash
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val hash = ImageHash.generatePHash(bitmap!!)

                AISearchResult(labels, hash)
            } catch (e: Exception) {
                // Fallback
                AISearchResult(emptyList(), "0000000000000000")
            }
        }
    }

    fun rankItems(queryResult: AISearchResult, items: List<Item>): List<Item> {
        return items
            .mapNotNull { item ->
                val score = calculateScore(queryResult, item)
                if (score > 0.3f) RankedItem(item, score) else null
            }
            .sortedByDescending { it.score }
            .take(50)  // Limit results
            .map { it.item }
    }

    private fun calculateScore(query: AISearchResult, item: Item): Float {
        var score = 0f

        // 1. Label match (50% weight)
        val labelMatches = query.labels.intersect(item.aiLabels.toSet()).size.toFloat()
        val labelScore = if (query.labels.isNotEmpty()) {
            (labelMatches / query.labels.size)
        } else 0f
        score += labelScore * 0.5f

        // 2. Hash match (30% weight)
        if (item.imageHash.isNotEmpty() && query.hash.isNotEmpty()) {
            score += if (ImageHash.isSimilar(query.hash, item.imageHash)) 0.3f else 0f
        }

        // 3. Text match (20% weight)
        val textScore = textSimilarity(query.labels.joinToString(" "), item.title + " " + item.description)
        score += textScore * 0.2f

        return score
    }

    private fun textSimilarity(a: String, b: String): Float {
        val setA = a.lowercase().split(" ").filter { it.length > 2 }.toSet()
        val setB = b.lowercase().split(" ").filter { it.length > 2 }.toSet()

        if (setA.isEmpty() || setB.isEmpty()) return 0f

        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return intersection.toFloat() / union
    }
}

// Data classes
data class AISearchResult(
    val labels: List<String>,
    val hash: String
)

data class RankedItem(
    val item: Item,
    val score: Float
)
