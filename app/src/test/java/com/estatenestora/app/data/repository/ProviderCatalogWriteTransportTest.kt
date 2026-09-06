package com.estatenestora.app.data.repository

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogWriteTransportTest {
    @Test
    fun `provider catalog payload is split below inline query limit and round trips`() {
        val payload = """{"title":"Tap replacement","description":"${"work details ".repeat(45)}"}""".toByteArray()
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)

        val chunks = providerCatalogWriteChunks(encoded, payload.size)!!
        val decoded = Base64.getUrlDecoder().decode(chunks.joinToString(""))

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 100 })
        chunks.forEachIndexed { index, chunk ->
            val fullInlineQuery =
                "AAPP::${"1".repeat(13)}::PROVIDER_CATALOG_WRITE_CHUNK::${"t".repeat(36)}::$index::${chunks.size}::$chunk"
            assertTrue(fullInlineQuery.length <= 256)
            val draftSelectionQuery =
                "AAPP::${"1".repeat(13)}::DRAFT_SERVICE_SELECTION_WRITE_CHUNK::${"t".repeat(36)}::$index::${chunks.size}::$chunk"
            assertTrue(draftSelectionQuery.length <= 256)
        }
        assertArrayEquals(payload, decoded)
    }

    @Test
    fun `empty and oversized provider catalog payloads are rejected`() {
        assertNull(providerCatalogWriteChunks("", 0))
        assertNull(providerCatalogWriteChunks("a", 10_001))
    }
}
