package com.luminor.actionbox.domain

class DetectActionUseCase {
    operator fun invoke(text: String): DetectedAction = ActionDetector.detect(text)

    fun forceType(text: String, type: ActionType): DetectedAction =
        ActionDetector.forceType(text, type)
}
