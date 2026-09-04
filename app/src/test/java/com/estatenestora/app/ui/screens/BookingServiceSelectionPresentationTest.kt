package com.estatenestora.app.ui.screens

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingServiceSelectionPresentationTest {
    @Test
    fun `package snapshot keeps booked inclusions prices and quantities`() {
        val snapshot = JsonParser.parseString(
            """{
              "kind":"PACKAGE",
              "package":{"name":"Kitchen care","description":"Two common repairs","included_text":"Labour","excluded_text":"New tap"},
              "items":[{"title":"Replace washer","description":"Standard washer","quantity":2,"price_amount":150,"duration_minutes":20}],
              "total_amount":250,
              "duration_minutes":40
            }"""
        ).asJsonObject

        val result = bookingServiceSelectionPresentation(snapshot)!!

        assertEquals("Kitchen care", result.title)
        assertEquals("Labour", result.includedText)
        assertEquals("New tap", result.excludedText)
        assertEquals(250.0, result.providerAmount!!, 0.001)
        assertEquals(1, result.items.size)
        assertEquals(2, result.items.first().quantity)
        assertEquals(300.0, result.items.first().lineAmount, 0.001)
        assertEquals(40, result.items.first().totalDurationMinutes)
    }

    @Test
    fun `individual snapshot receives a customer readable title`() {
        val snapshot = JsonParser.parseString(
            """{"kind":"ITEMS","items":[{"title":"Inspection","quantity":1,"price_amount":99,"duration_minutes":15}],"total_amount":99,"duration_minutes":15}"""
        ).asJsonObject

        val result = bookingServiceSelectionPresentation(snapshot)!!

        assertEquals("Inspection", result.title)
        assertEquals("ITEMS", result.kind)
    }

    @Test
    fun `unknown or malformed selection is ignored safely`() {
        assertNull(bookingServiceSelectionPresentation(JsonParser.parseString("{\"kind\":\"LISTING\"}").asJsonObject))
        assertNull(bookingServiceSelectionPresentation(JsonParser.parseString("{\"kind\":[]}").asJsonObject))
    }
}
