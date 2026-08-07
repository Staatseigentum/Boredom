package com.staatseigentum.kollaps.core

/**
 * A short record of how production has moved, kept in the save.
 *
 * The statistics tab could always say what the player produces *now*; it could never say whether
 * that number was climbing or had been flat for twenty minutes. Forty samples of play time is
 * enough to show the shape of a session without turning the save into a log file.
 */
object History {

    /** Seconds of play between two samples. */
    const val SAMPLE_SECONDS = 45.0

    /** How many samples are kept. Forty at forty-five seconds is half an hour. */
    const val MAX_SAMPLES = 40

    /** The span the full record covers, in seconds. */
    const val SPAN_SECONDS = MAX_SAMPLES * SAMPLE_SECONDS

    /** Appends [value], dropping the oldest sample once the record is full. */
    fun append(samples: List<Double>, value: Double): List<Double> {
        val grown = samples + value
        return if (grown.size <= MAX_SAMPLES) grown else grown.takeLast(MAX_SAMPLES)
    }

    /** True once there is enough of a record for a line to mean anything. */
    fun isWorthShowing(samples: List<Double>): Boolean = samples.size >= 3
}
