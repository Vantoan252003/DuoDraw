package com.toan.codraw.domain.usecase

import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.DrawingRepository
import javax.inject.Inject

class SendDrawEventUseCase @Inject constructor(
    private val repository: DrawingRepository
) {
    operator fun invoke(stroke: Stroke) {
        repository.sendStroke(stroke)
    }
}

