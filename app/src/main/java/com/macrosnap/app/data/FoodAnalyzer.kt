package com.macrosnap.app.data

import com.macrosnap.app.model.AnalysisResult
import com.macrosnap.app.model.Outcome
import com.macrosnap.app.model.SupplementAnalysis

/** The seam for swapping recognition backends (cloud today, an on-device model later). */
interface FoodAnalyzer {
    suspend fun analyzePhoto(photoPath: String, hint: String?): Outcome<AnalysisResult>

    /** Turns free text like "50 g olive oil, 2 eggs" into items with weights and macros. */
    suspend fun analyzeText(description: String): Outcome<AnalysisResult>

    /** Turns "vitamin D3 2000 IU" or "creatine monohydrate 5 g" into one dose's nutrient content. */
    suspend fun analyzeSupplement(description: String): Outcome<SupplementAnalysis>
}
