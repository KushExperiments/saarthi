package com.lifeos.app.core.memory

/** Memory §3's nine clusters, each with a coherent sensitivity tier. */
enum class MemoryCategory(val sensitivityTier: SensitivityTier) {
    IDENTITY(SensitivityTier.MEDIUM),
    RELATIONSHIPS(SensitivityTier.HIGH),
    HEALTH(SensitivityTier.CRITICAL),
    DAILY_ROUTINE(SensitivityTier.LOW),
    INTERESTS(SensitivityTier.LOW),
    OCCASIONS(SensitivityTier.LOW),
    BELONGINGS(SensitivityTier.MEDIUM),
    LIFE_STORY(SensitivityTier.HIGH),
    PRACTICAL_SAFETY(SensitivityTier.HIGH),
}
