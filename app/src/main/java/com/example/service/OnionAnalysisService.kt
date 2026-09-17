package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.OnionResult

sealed class OnionAnalysisResponse {
    data class Success(val result: OnionResult) : OnionAnalysisResponse()
    data class NoOnionsDetected(val message: String) : OnionAnalysisResponse()
}

interface OnionAnalysisService {
    suspend fun detectAndAnalyze(
        context: Context,
        imageUri: Uri?,
        presetType: String? = null,
        onProgress: ((Int, String) -> Unit)? = null
    ): OnionAnalysisResponse
    
    fun close()
}
