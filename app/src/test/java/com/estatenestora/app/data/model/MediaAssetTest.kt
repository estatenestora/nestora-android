package com.estatenestora.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaAssetTest {
    private fun variant(name: String, id: String) = MediaVariant(name, id, width = 1, height = 1, byteSize = 1)

    @Test
    fun preferredVariantIsSelected() {
        val asset = MediaAsset("a", "PLATFORM", scope = "APP_CAROUSEL", variants = listOf(variant("THUMBNAIL", "t"), variant("HERO", "h")))
        assertEquals("h", asset.fileIdFor("HERO"))
    }

    @Test
    fun cardThenFirstVariantAreSafeFallbacks() {
        val card = MediaAsset("a", "PROVIDER", scope = "PACKAGE", variants = listOf(variant("THUMBNAIL", "t"), variant("CARD", "c")))
        assertEquals("c", card.fileIdFor("HERO"))
        val first = MediaAsset("b", "PROVIDER", scope = "PACKAGE", variants = listOf(variant("THUMBNAIL", "t")))
        assertEquals("t", first.fileIdFor("HERO"))
        assertNull(MediaAsset("empty", "PLATFORM", scope = "CATEGORY").fileIdFor("CARD"))
    }
}
