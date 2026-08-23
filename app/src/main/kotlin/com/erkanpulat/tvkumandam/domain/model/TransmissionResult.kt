package com.erkanpulat.tvkumandam.domain.model

/** Typed outcomes keep platform failures out of the UI and avoid crashes. */
sealed interface TransmissionResult {
    data object Success : TransmissionResult
    data object UnsupportedDevice : TransmissionResult
    data class UnsupportedCarrier(val carrierFrequencyHz: Int) : TransmissionResult
    data object CommandUnavailable : TransmissionResult
    data class EncodingFailure(val message: String) : TransmissionResult
    data class PlatformFailure(val message: String) : TransmissionResult
}
