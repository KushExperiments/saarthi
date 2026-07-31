package com.lifeos.app.core.memory

/**
 * Memory §17's "swappable Embedder" seam — a decade-long memory store must
 * not be hard-tied to one embedding model. [KeywordEmbedder] is the only
 * implementation today (a real on-device embedding model is a separate,
 * much heavier undertaking: model packaging plus CPU/memory budget on
 * minSdk-26 devices); a future `OnDeviceEmbedder`/`CloudEmbedder` slots in
 * behind this same interface with zero call-site changes.
 */
interface Embedder {
    /** Higher is more similar. Not assumed to be normalized/bounded by callers. */
    fun similarity(query: String, candidate: String): Float
}
