package com.staatseigentum.kollaps.core.audio

/**
 * The smallest RIFF header that is still a valid WAV file, and the samples behind it.
 *
 * Its own file because two things now need it — the short cues and the loop the music runs on —
 * and a header written twice is a header that eventually disagrees with itself about how many
 * bytes it claims to hold.
 */
internal object Wav {

    const val HEADER_BYTES = 44

    /** Wraps 16 bit mono PCM at [sampleRate] in a WAV container. */
    fun riff(samples: ShortArray, sampleRate: Int): ByteArray {
        val dataBytes = samples.size * 2
        val out = ByteArray(HEADER_BYTES + dataBytes)
        var at = 0

        fun ascii(text: String) {
            for (c in text) out[at++] = c.code.toByte()
        }

        fun int32(value: Int) {
            out[at++] = (value and 0xFF).toByte()
            out[at++] = ((value shr 8) and 0xFF).toByte()
            out[at++] = ((value shr 16) and 0xFF).toByte()
            out[at++] = ((value shr 24) and 0xFF).toByte()
        }

        fun int16(value: Int) {
            out[at++] = (value and 0xFF).toByte()
            out[at++] = ((value shr 8) and 0xFF).toByte()
        }

        ascii("RIFF")
        int32(HEADER_BYTES - 8 + dataBytes)
        ascii("WAVE")
        ascii("fmt ")
        int32(16) // size of this chunk
        int16(1) // uncompressed PCM
        int16(1) // mono
        int32(sampleRate)
        int32(sampleRate * 2) // bytes per second
        int16(2) // bytes per frame
        int16(16) // bits per sample
        ascii("data")
        int32(dataBytes)

        for (sample in samples) int16(sample.toInt())
        return out
    }

    /** Scales to [peak], so nothing is quietly louder than anything else. */
    fun normalise(samples: DoubleArray, peak: Double): ShortArray {
        val loudest = samples.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0
        val scale = if (loudest <= 0.0) 0.0 else peak / loudest
        return ShortArray(samples.size) { (samples[it] * scale).toInt().toShort() }
    }
}
