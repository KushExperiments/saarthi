package com.lifeos.app.core.memory

import javax.inject.Inject

/** v1 [Embedder]: Jaccard term-overlap, no ML model, fully offline. */
class KeywordEmbedder @Inject constructor() : Embedder {
    override fun similarity(query: String, candidate: String): Float {
        val queryTerms = tokenize(query)
        val candidateTerms = tokenize(candidate)
        if (queryTerms.isEmpty() || candidateTerms.isEmpty()) return 0f

        val intersection = queryTerms.intersect(candidateTerms).size
        val union = queryTerms.union(candidateTerms).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }.toSet()
}
