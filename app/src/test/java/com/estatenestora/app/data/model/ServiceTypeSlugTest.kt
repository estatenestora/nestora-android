package com.estatenestora.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceTypeSlugTest {
    @Test
    fun `canonicalServiceTypeSlug maps owner aliases to property_owner`() {
        assertEquals("property_owner", canonicalServiceTypeSlug("property_owner"))
        assertEquals("property_owner", canonicalServiceTypeSlug("property owner"))
        assertEquals("property_owner", canonicalServiceTypeSlug("flat_owner"))
        assertEquals("property_owner", canonicalServiceTypeSlug("flat owner"))
        assertEquals("property_owner", canonicalServiceTypeSlug("flat owners"))
        assertEquals("property_owner", canonicalServiceTypeSlug("owner flats"))
        assertEquals("Property Owner", serviceTypeDisplayName("flat_owner"))
        assertEquals("Property Owner", serviceTypeDisplayName("Flat Owner"))
        assertTrue(isPropertyOwnerServiceTypeSlug("Flat Owner"))
        assertTrue(isPropertyOwnerServiceTypeSlug("flat owners"))
        assertTrue(isPropertyOwnerServiceTypeSlug("FLAT_OWNER"))
    }
}
