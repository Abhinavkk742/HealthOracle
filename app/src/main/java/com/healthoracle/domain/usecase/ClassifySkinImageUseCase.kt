package com.healthoracle.domain.usecase

import android.graphics.Bitmap
import com.healthoracle.core.util.Resource
import com.healthoracle.data.model.PredictionResult
import com.healthoracle.data.repository.SkinDiseaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ClassifySkinImageUseCase @Inject constructor(
    private val repository: SkinDiseaseRepository
) {
    operator fun invoke(bitmap: Bitmap): Flow<Resource<PredictionResult>> = flow {
        emit(Resource.Loading)
        val result = repository.classifyImage(bitmap)
        emit(result)
    }
}
