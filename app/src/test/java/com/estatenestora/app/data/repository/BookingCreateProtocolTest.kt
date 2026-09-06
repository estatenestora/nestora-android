package com.estatenestora.app.data.repository

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingCreateProtocolTest {
    @Test
    fun `booking creation carries location atomically within telegram query limit`() {
        val listingId = "6ca809b0-e88a-4027-af3c-1735f0e81566"
        val command = NestoraRepository().buildCreateBookingCommand(
            listingId = listingId,
            home = true,
            lat = 22.566963,
            lon = 88.513828,
            address = "Customer address ".repeat(40)
        )

        assertTrue("AAPP::0000000000000::$command".length <= 256)
        val payload = command.removePrefix("CREATE_BOOKING::$listingId::")
        val json = JsonParser.parseString(payload).asJsonObject
        assertEquals(22.566963, json["lat"].asDouble, 0.0)
        assertEquals(88.513828, json["lon"].asDouble, 0.0)
        assertTrue(json["address"].asString.isNotBlank())
    }
}
