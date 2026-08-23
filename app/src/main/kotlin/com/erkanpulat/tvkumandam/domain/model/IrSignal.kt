package com.erkanpulat.tvkumandam.domain.model

/**
 * A ConsumerIr-compatible carrier frequency and alternating mark/space pattern.
 * The timing array is copied on input and output so profiles cannot mutate a
 * signal while it is being transmitted.
 */
class IrSignal(
    val carrierFrequencyHz: Int,
    patternMicros: IntArray,
) {
    private val pattern = patternMicros.copyOf()

    init {
        require(carrierFrequencyHz in MIN_CARRIER_HZ..MAX_CARRIER_HZ) {
            "Carrier frequency must be between $MIN_CARRIER_HZ and $MAX_CARRIER_HZ Hz."
        }
        require(pattern.size >= MIN_PATTERN_TIMINGS) {
            "IR pattern must contain at least a mark and a space."
        }
        require(pattern.all { it > 0 }) { "IR timings must be positive." }
        require(pattern.sumOf(Int::toLong) < MAX_PATTERN_DURATION_MICROS) {
            "IR pattern must be shorter than $MAX_PATTERN_DURATION_MICROS microseconds."
        }
    }

    fun patternCopy(): IntArray = pattern.copyOf()

    override fun equals(other: Any?): Boolean =
        other is IrSignal &&
            carrierFrequencyHz == other.carrierFrequencyHz &&
            pattern.contentEquals(other.pattern)

    override fun hashCode(): Int = 31 * carrierFrequencyHz + pattern.contentHashCode()

    companion object {
        private const val MIN_CARRIER_HZ = 20_000
        private const val MAX_CARRIER_HZ = 60_000
        private const val MIN_PATTERN_TIMINGS = 2
        private const val MAX_PATTERN_DURATION_MICROS = 2_000_000L
    }
}
