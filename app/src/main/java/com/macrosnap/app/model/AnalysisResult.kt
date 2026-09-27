package com.macrosnap.app.model

data class AnalysisResult(
    val items: List<DetectedItem>,
    val notes: String?,
)

data class DetectedItem(
    val item: FoodItem,
    val confidence: Confidence,
)

enum class Confidence { LOW, MEDIUM, HIGH }

data class SupplementAnalysis(
    val name: String,
    val doseLabel: String,
    val micros: Micros,
    val notes: String?,
)
