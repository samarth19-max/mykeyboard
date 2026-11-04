package com.wasimaster.wmkeyboard.core.voice.whisper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperCppPlanTest {

    private fun model(
        size: WhisperSize,
        english: Boolean = false,
        langCodes: Set<String> = if (english) setOf("en") else emptySet(),
        fastInference: Boolean = false,
        forceAutoLanguage: Boolean = false,
    ) = WhisperModel(
        id = "test",
        displayName = "Whisper test",
        size = size,
        repo = "repo",
        modelFile = "ggml-test.bin",
        vocabFile = "",
        modelBytes = 1,
        vocabBytes = 0,
        langCodes = langCodes,
        selectableLang = !english,
        tier = WhisperTier.STANDARD,
        descriptionRes = 0,
        format = WhisperFormat.GGML,
        fastInference = fastInference,
        forceAutoLanguage = forceAutoLanguage,
    )

    @Test
    fun `beam search on the sizes where it is cheap, greedy on the big ones`() {
        assertEquals(5, WhisperCppPlan.beamSize(model(WhisperSize.TINY)))
        assertEquals(5, WhisperCppPlan.beamSize(model(WhisperSize.SMALL)))
        assertEquals(5, WhisperCppPlan.beamSize(model(WhisperSize.TURBO)))
        assertEquals(1, WhisperCppPlan.beamSize(model(WhisperSize.MEDIUM)))
        assertEquals(1, WhisperCppPlan.beamSize(model(WhisperSize.LARGE)))
    }

    @Test
    fun `Apex auto detection is not offered for unrelated languages`() {
        if (!WhisperCatalog.cppAvailable) return
        val apex = WhisperCatalog.byId("cpp-apex-hinglish")!!
        assertTrue(apex.covers("hi"))
        assertTrue(apex.covers("en"))
        assertTrue(!apex.covers("de"))
    }

    @Test
    fun `Swift forces Hindi even when the active keyboard layout is English`() {
        val swift = model(WhisperSize.BASE, langCodes = setOf("hi"))
        assertEquals(listOf("hi"), WhisperCppPlan.languages(swift, listOf("en")))
    }

    @Test
    fun `Apex uses model-side language detection for Hinglish`() {
        val apex = model(
            WhisperSize.TURBO,
            langCodes = setOf("hi", "en"),
            forceAutoLanguage = true,
        )
        assertTrue(WhisperCppPlan.languages(apex, listOf("hi", "en")).isEmpty())
    }

    @Test
    fun `Swift fast mode uses greedy decoding`() {
        assertEquals(1, WhisperCppPlan.beamSize(model(WhisperSize.BASE, fastInference = true)))
    }

    @Test
    fun `auto asks for the GPU from Base up, the explicit choices always win`() {
        assertEquals(false, WhisperCppPlan.wantsGpu(model(WhisperSize.TINY), WhisperCompute.AUTO))
        assertEquals(true, WhisperCppPlan.wantsGpu(model(WhisperSize.BASE), WhisperCompute.AUTO))
        assertEquals(true, WhisperCppPlan.wantsGpu(model(WhisperSize.TURBO), WhisperCompute.AUTO))
        assertEquals(true, WhisperCppPlan.wantsGpu(model(WhisperSize.TINY), WhisperCompute.GPU))
        assertEquals(false, WhisperCppPlan.wantsGpu(model(WhisperSize.LARGE), WhisperCompute.CPU))
        // An unknown stored value reads as Auto.
        assertEquals(true, WhisperCppPlan.wantsGpu(model(WhisperSize.SMALL), "npu"))
    }

    @Test
    fun `every dictation language is named, in order, in Whisper's spelling`() {
        assertEquals(listOf("bn", "en"), WhisperCppPlan.languages(model(WhisperSize.SMALL), listOf("bn", "en")))
        // Bokmål is Whisper's "no"; a language Whisper lacks is left out.
        assertEquals(listOf("no"), WhisperCppPlan.languages(model(WhisperSize.SMALL), listOf("nb", "zz")))
        assertEquals(listOf("de"), WhisperCppPlan.languages(model(WhisperSize.SMALL), listOf("de", "de")))
    }

    @Test
    fun `an English-only model is told no language`() {
        assertTrue(WhisperCppPlan.languages(model(WhisperSize.BASE, english = true), listOf("en")).isEmpty())
    }

    @Test
    fun `the prompt lists the user's words once each, up to the cap`() {
        assertNull(WhisperCppPlan.prompt(emptyList()))
        assertNull(WhisperCppPlan.prompt(listOf(" ", "")))
        assertEquals("Glossary: Arian, WM Keyboard.", WhisperCppPlan.prompt(listOf(" Arian ", "WM Keyboard", "Arian")))
        val many = (1..100).map { "word$it" }
        val prompt = WhisperCppPlan.prompt(many)!!
        assertTrue(prompt.endsWith("word${WhisperCppPlan.PROMPT_WORDS}."))
        assertTrue(!prompt.contains("word${WhisperCppPlan.PROMPT_WORDS + 1}"))
    }

    @Test
    fun `a word with a control character is not prompted`() {
        assertEquals("Glossary: ok.", WhisperCppPlan.prompt(listOf("bad\u0000word", "ok")))
    }
}
