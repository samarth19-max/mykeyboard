package com.wasimaster.wmkeyboard.core.voice.whisper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperDownloadManagerTest {

    @Test
    fun `recognises the published Swift Hinglish GGML file by name and size band`() {
        assertTrue(
            WhisperDownloadManager.recognises(
                "ggml-oriserve-hinglish-q8_0.bin",
                78_000_000L,
            ),
        )
        assertTrue(
            WhisperDownloadManager.recognises(
                "ggml-oriserve-hinglish-q8_0.bin",
                82_000_000L,
            ),
        )
        assertFalse(
            WhisperDownloadManager.recognises(
                "ggml-oriserve-hinglish-q8_0.bin",
                120_000_000L,
            ),
        )
    }

    @Test
    fun `recognises the published Apex Hinglish GGML file by name and size band`() {
        assertTrue(
            WhisperDownloadManager.recognises(
                "ggml-apex-hinglish-q8_0.bin",
                834_000_000L,
            ),
        )
        assertTrue(
            WhisperDownloadManager.recognises(
                "ggml-apex-hinglish-q8_0.bin",
                840_000_000L,
            ),
        )
        assertFalse(
            WhisperDownloadManager.recognises(
                "ggml-apex-hinglish-q8_0.bin",
                950_000_000L,
            ),
        )
        assertFalse(
            WhisperDownloadManager.recognises(
                "renamed-apex.bin",
                834_000_000L,
            ),
        )
    }
}
