package com.wasimaster.wmkeyboard.app

import com.wasimaster.wmkeyboard.config.BuildConfig
import com.wasimaster.wmkeyboard.core.providers.ProviderTool
import com.wasimaster.wmkeyboard.core.providers.ProviderTypes
import com.wasimaster.wmkeyboard.core.voice.VoiceChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalOnlyProviderTest {

    @Test
    fun localOnlyCatalogContainsOnlyOnDeviceProviders() {
        if (!BuildConfig.LOCAL_ONLY) return

        assertTrue(ProviderTypes.all.isNotEmpty())
        assertTrue(ProviderTypes.all.all { it.onDevice })
        assertTrue("Whisper must be offered as an on-device provider", ProviderTypes.of("whisper")?.onDevice == true)
    }

    @Test
    fun localOnlyCatalogDoesNotResolveRemoteOrSelfHostedProviders() {
        if (!BuildConfig.LOCAL_ONLY) return

        listOf(
            "openai",
            "anthropic",
            "gemini",
            "deepseek",
            "ollama",
            "lm_studio",
            "openai_compatible",
            "translate_server",
            "transcription_server",
            "system_recognizer",
            "searxng",
            "libretranslate",
        ).forEach { assertNull("Unexpected provider: $it", ProviderTypes.of(it)) }

        assertEquals(
            listOf("on_device_ai"),
            ProviderTypes.serving(ProviderTool.AI).map { it.id },
        )
        assertFalse(ProviderTypes.serving(ProviderTool.VOICE).any { !it.onDevice })
        val choices = VoiceChoices.forLanguage(
            languageId = "hi",
            whistleReady = false,
            whisperModels = emptyList(),
            etModels = emptyList(),
            serverConfigured = true,
        )
        assertTrue("System recognition must not be offered", choices.none { it.engine == VoiceChoices.SYSTEM })
        assertTrue("Remote transcription must not be offered", choices.none { it.engine == VoiceChoices.SERVER })
    }
}
