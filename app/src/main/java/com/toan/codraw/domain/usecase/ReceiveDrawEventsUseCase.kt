package com.toan.codraw.domain.usecase

import com.toan.codraw.domain.model.Stroke
import com.toan.codraw.domain.repository.DrawingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReceiveDrawEventsUseCase @Inject constructor(
    private val repository: DrawingRepository
) {
    operator fun invoke(): Flow<Stroke> = repository.receiveStrokes()
}

