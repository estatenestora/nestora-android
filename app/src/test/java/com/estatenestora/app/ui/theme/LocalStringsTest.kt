package com.estatenestora.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LocalStringsTest {
    @Test
    fun `language codes resolve to their shared global string sets`() {
        assertSame(EnglishStrings, stringsForLanguage(NestoraLanguage.fromCode("en")))
        assertSame(HindiStrings, stringsForLanguage(NestoraLanguage.fromCode("hi")))
        assertSame(BengaliStrings, stringsForLanguage(NestoraLanguage.fromCode("bn")))
    }

    @Test
    fun `unknown saved language code safely falls back to English`() {
        assertEquals(NestoraLanguage.English, NestoraLanguage.fromCode("unsupported"))
        assertSame(EnglishStrings, stringsForLanguage(NestoraLanguage.fromCode("unsupported")))
    }
}
