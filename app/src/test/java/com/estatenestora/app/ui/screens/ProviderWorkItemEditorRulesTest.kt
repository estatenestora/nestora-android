package com.estatenestora.app.ui.screens

import com.estatenestora.app.data.model.ServiceAttributeTemplate
import com.estatenestora.app.data.model.serviceTypeDisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderWorkItemEditorRulesTest {
    private val dealTypes = ServiceAttributeTemplate(
        key = "deal_types",
        displayLabel = "Deal Types",
        inputType = "multiselect",
        options = listOf("Rent", "Sell"),
        isRequired = true
    )
    private val propertyTypes = ServiceAttributeTemplate(
        key = "property_types",
        displayLabel = "Property Types Handled",
        inputType = "multiselect",
        options = listOf("Flat", "Villa", "PG", "Plot", "Commercial", "Office", "Warehouse"),
        isRequired = false
    )
    private val parking = ServiceAttributeTemplate(
        key = "parking",
        displayLabel = "Parking Available",
        inputType = "select",
        options = listOf("Car & Bike", "Bike only", "Car only", "None")
    )
    private val securityDeposit = ServiceAttributeTemplate(
        key = "security_deposit",
        displayLabel = "Security Deposit",
        inputType = "select",
        options = listOf("1 Month", "2 Months", "3 Months", "6 Months")
    )

    @Test
    fun `sanitizeBrokerAttributeTemplates updates deal types and property types options without duplicates`() {
        val rawDealTypes = ServiceAttributeTemplate(
            key = "deal_types",
            displayLabel = "Deal types",
            inputType = "multiselect",
            options = listOf("Buy", "Sell", "Rent", "PG", "Commercial", "Plot"),
            isRequired = true
        )
        val rawProp = ServiceAttributeTemplate(
            key = "property_types",
            displayLabel = "Property types",
            inputType = "multiselect",
            options = listOf("Flat", "Villa", "Plot", "Commercial", "Office", "Warehouse"),
            isRequired = false
        )
        val sanitized = sanitizeBrokerAttributeTemplates(listOf(rawDealTypes, rawProp))

        val deal = sanitized.first { it.key == "deal_types" }
        val prop = sanitized.first { it.key == "property_types" }

        assertEquals(listOf("Rent", "Sell"), deal.options)
        assertEquals(listOf("Flat", "Villa", "Plot", "Commercial", "Office", "Warehouse", "PG"), prop.options)
        assertEquals(prop.options?.distinct(), prop.options)
        assertEquals(deal.options?.distinct(), deal.options)
    }

    @Test
    fun `sanitizeBrokerAttributeTemplates does not invent broker fields for other services`() {
        val plumber = listOf(parking)
        assertEquals(plumber, sanitizeBrokerAttributeTemplates(plumber))
    }

    @Test
    fun `broker field order is deal types then property types then security then rest`() {
        val bhk = ServiceAttributeTemplate(key = "bhk", displayLabel = "BHK Handled", inputType = "select")
        val ordered = orderedBrokerWorkItemTemplates(
            listOf(bhk, securityDeposit, propertyTypes, dealTypes),
            hideSecurityDeposit = false
        )
        assertEquals(listOf("deal_types", "property_types", "security_deposit", "bhk"), ordered.map { it.key })
        assertEquals(
            listOf("deal_types", "property_types", "bhk"),
            orderedBrokerWorkItemTemplates(listOf(bhk, securityDeposit, propertyTypes, dealTypes), hideSecurityDeposit = true).map { it.key }
        )
    }

    @Test
    fun `property owner keeps property type and preferred tenants separate from broker property types`() {
        val rawPropertyType = ServiceAttributeTemplate(
            key = "property_type",
            displayLabel = "Property Type",
            inputType = "select",
            options = listOf("Apartment Flat", "Villa")
        )
        val rawTenants = ServiceAttributeTemplate(
            key = "tenant_preference",
            displayLabel = "Preferred Tenants",
            inputType = "multiselect",
            options = listOf("Family")
        )
        val sanitized = withPropertyOwnerBrokerFields(listOf(rawPropertyType, rawTenants, parking))
        assertEquals("property_type", sanitized.first { it.key == "property_type" }.key)
        assertEquals("tenant_preference", sanitized.first { it.key == "tenant_preference" }.key)
        assertEquals(listOf("Rent", "Sell"), sanitized.first { it.key == "deal_types" }.options)
		assertTrue(sanitized.any { it.key == "property_types" })
        assertEquals("select", sanitized.first { it.key == "property_type" }.inputType)
        assertEquals("multiselect", sanitized.first { it.key == "tenant_preference" }.inputType)
        val injected = withPropertyOwnerBrokerFields(emptyList())
        assertTrue(injected.any { it.key == "deal_types" })
        assertTrue(injected.any { it.key == "property_type" })
        assertTrue(injected.any { it.key == "tenant_preference" })
        assertEquals(
            listOf("deal_types", "property_types", "security_deposit", "property_type", "tenant_preference", "bhk"),
            orderedBrokerWorkItemTemplates(
                listOf(
                    ServiceAttributeTemplate(key = "bhk", displayLabel = "BHK", inputType = "select"),
                    securityDeposit,
                    rawPropertyType,
                    rawTenants,
                    propertyTypes,
                    dealTypes
                ),
                hideSecurityDeposit = false
            ).map { it.key }
        )
        assertTrue(isPropertyOwnerWorkItemEditor("property_owner"))
        assertTrue(isPropertyOwnerWorkItemEditor("property owner"))
        assertTrue(isPropertyOwnerWorkItemEditor("Flat Owner"))
        assertTrue(
            isPropertyOwnerWorkItemEditor(
                "",
                listOf(
                    ServiceAttributeTemplate(key = "property_type", displayLabel = "Property Type", inputType = "select"),
                    ServiceAttributeTemplate(key = "tenant_preference", displayLabel = "Preferred Tenants", inputType = "multiselect")
                )
            )
        )
        assertFalse(workItemSupportsOtherOption("deal_types"))
        assertTrue(workItemSupportsOtherOption("property_type"))
        assertTrue(workItemSupportsOtherOption("tenant_preference"))
    }

    @Test
    fun `sell hides security deposit and rent uses rent price label`() {
        assertTrue(brokerHidesSecurityDeposit(listOf("Sell")))
        assertTrue(brokerHidesSecurityDeposit(listOf("Rent", "Sell")))
        assertFalse(brokerHidesSecurityDeposit(listOf("Rent")))
        assertEquals("Rent Price (₹) *", brokerPriceFieldLabel(listOf("Rent")))
        assertEquals("Selling Price (₹) *", brokerPriceFieldLabel(listOf("Sell")))
        assertEquals("Price (₹) *", brokerPriceFieldLabel(listOf("Rent", "Sell")))
        assertEquals("Price (₹) *", brokerPriceFieldLabel(emptyList()))
        assertEquals("Enter monthly rent", brokerPriceFieldPlaceholder(listOf("Rent")))
        assertEquals("Enter selling price", brokerPriceFieldPlaceholder(listOf("Sell")))
        assertEquals("Enter price", brokerPriceFieldPlaceholder(listOf("Rent", "Sell")))
        assertEquals(listOf("Sell"), selectedBrokerDealTypes("Sell"))
        assertEquals(listOf("Rent"), selectedBrokerDealTypes("Rent"))
        assertTrue(workItemUsesBrokerDealUi("", listOf(dealTypes)))
        assertTrue(workItemUsesBrokerDealUi("Property Owner", emptyList()))
        assertTrue(workItemUsesBrokerDealUi("property_owner", emptyList()))
        assertFalse(workItemUsesBrokerDealUi("plumber", listOf(parking)))
        assertTrue(isDealTypesWorkItemAttribute("dealTypes", "Deal Types", listOf("Rent", "Sell")))
        assertTrue(
            isDealTypesWorkItemAttribute(
                "txn",
                "Deal Types",
                listOf("Buy", "Rent", "Sell", "PG", "Commercial")
            )
        )
        assertTrue(isDealTypesWorkItemAttribute(dealTypes))
        assertEquals("deal_types", normalizeWorkItemAttributeKey("dealTypes"))
        assertEquals("security_deposit", normalizeWorkItemAttributeKey("securityDeposit"))
        assertTrue(workItemUsesSecurityDepositEditor("securityDeposit"))
        assertTrue(workItemChoiceIsSingleSelect("dealTypes", "multiselect"))
    }

    @Test
    fun `deal types allow only one selected choice`() {
        assertTrue(workItemChoiceIsSingleSelect("deal_types", "multiselect"))
        assertTrue(workItemChoiceIsSingleSelect("property_types", "multiselect"))
        assertTrue(workItemChoiceIsSingleSelect("furnishing", "multiselect"))
        assertFalse(workItemChoiceIsSingleSelect("amenities", "multiselect"))
        assertTrue(workItemChoiceIsSingleSelect("parking", "select"))
        assertEquals("Rent", coerceSingleChoiceCsv("Rent, Sell"))
        assertEquals("Sell", coerceSingleChoiceCsv("Sell"))
        assertEquals("Flat", coerceSingleChoiceCsv("Flat, Villa, PG"))
        assertFalse(workItemChoiceChipEnabled(singleSelect = true, thisChosen = false, exclusiveSelectionTaken = true))
        assertTrue(workItemChoiceChipEnabled(singleSelect = true, thisChosen = true, exclusiveSelectionTaken = true))
        assertTrue(workItemChoiceChipEnabled(singleSelect = true, thisChosen = false, exclusiveSelectionTaken = false))
        assertTrue(workItemChoiceChipEnabled(singleSelect = false, thisChosen = false, exclusiveSelectionTaken = true))
    }

    @Test
    fun `legacy deal type values move to property types without repeating`() {
        val relocated = relocateBrokerDealValuesToPropertyTypes(
            "Buy, Rent, PG, Plot",
            "Flat, Plot",
            listOf("Flat", "Villa", "PG", "Plot", "Commercial")
        )
        assertEquals("Rent", relocated.first)
        assertEquals("Flat, Plot, PG", relocated.second)
    }

    @Test
    fun `security deposit accepts years and months and supports zero selection`() {
        assertEquals("0 Years", securityDepositYearLabel(0))
        assertEquals("1 Year", securityDepositYearLabel(1))
        assertEquals("2 Years", securityDepositYearLabel(2))
        assertEquals("0 Months", securityDepositMonthLabel(0))
        assertEquals("1 Month", securityDepositMonthLabel(1))
        assertEquals("3 Months", securityDepositMonthLabel(3))
        assertFalse(securityDepositSelectionValid(0, 0, "", zeroSelected = false))
        assertTrue(securityDepositSelectionValid(0, 0, "", zeroSelected = true))
        assertTrue(securityDepositSelectionValid(0, 3, ""))
        assertTrue(securityDepositSelectionValid(1, 2, ""))
        assertTrue(securityDepositSelectionValid(0, 0, "50000"))
        assertTrue(securityDepositSelectionValid(0, 0, "0"))
        assertTrue(securityDepositSelectionValid(1, 2, "50000"))
        assertEquals("0 Years 3 Months", encodeSecurityDepositDuration(0, 3))
        assertEquals("1 Year 2 Months", encodeSecurityDepositDuration(1, 2))
        assertEquals("0 Years 0 Months", encodeSecurityDepositDuration(0, 0))
        assertEquals("0 Years 3 Months + ₹12000", encodeSecurityDeposit(0, 3, "12000"))
        assertEquals("₹12000", encodeSecurityDeposit(0, 0, "12000"))
        assertEquals("0 Years 0 Months", encodeSecurityDeposit(0, 0, ""))
        val parsed = parseSecurityDeposit("1 Year 2 Months + ₹25000")
        assertEquals(1, parsed.years)
        assertEquals(2, parsed.months)
        assertEquals("25000", parsed.amount)
        assertEquals(0 to 3, parseSecurityDepositDuration("3 Months"))
        assertEquals(0 to 3, parseSecurityDepositDuration("0 Years 3 Months"))
    }

    @Test
    fun `other option adds temporary chips without replacing the Other action`() {
        assertEquals("Other Property Type", workItemOtherFieldLabel("Property Types Handled"))
        assertEquals("Other Parking", workItemOtherFieldLabel("Parking Available"))
        assertEquals("Other Amenity", workItemOtherFieldLabel("Amenities Provided"))
        assertEquals("Lease", resolveTemporaryWorkItemChip(dealTypes.options.orEmpty(), emptyList(), "Lease"))
        assertEquals("Rent", resolveTemporaryWorkItemChip(dealTypes.options.orEmpty(), emptyList(), "rent"))
        assertEquals("Lease", resolveTemporaryWorkItemChip(dealTypes.options.orEmpty(), listOf("Lease"), "lease"))
        assertNull(resolveTemporaryWorkItemChip(dealTypes.options.orEmpty(), emptyList(), "  "))
        assertEquals("Rent, Other: Lease", encodeWorkItemChoiceValues(setOf("Rent"), listOf("Lease")))
        val parsed = parseWorkItemChoiceSelection("Rent, Other: Lease", dealTypes.options.orEmpty())
        assertEquals(setOf("Rent"), parsed.catalogSelected)
        assertEquals(listOf("Lease"), parsed.extraChips)
        assertEquals(setOf("Lease"), parsed.extraSelected)
    }

    @Test
    fun `invalid broker sections are flagged for highlighting`() {
        assertTrue(
            workItemAttributeIsInvalid(dealTypes, "", otherDraftOpen = true, otherDraftText = "", depositAmount = "")
        )
        assertTrue(
            workItemAttributeIsInvalid(dealTypes, "", depositAmount = "")
        )
        assertFalse(
            workItemAttributeIsInvalid(dealTypes, "Rent", depositAmount = "")
        )
        assertFalse(
            workItemAttributeIsInvalid(
                ServiceAttributeTemplate(
                    key = "tenant_preference",
                    displayLabel = "Preferred Tenants",
                    inputType = "multiselect",
                    options = listOf("Family"),
                    isRequired = true
                ),
                "",
                extraChips = listOf("Working professionals"),
                otherDraftOpen = true,
                otherDraftText = "Working professionals",
                depositAmount = ""
            )
        )
        assertTrue(
            workItemAttributeIsInvalid(securityDeposit, "", depositAmount = "")
        )
        assertFalse(
            workItemAttributeIsInvalid(securityDeposit, "", depositMonths = 3, depositAmount = "")
        )
        assertFalse(
            workItemAttributeIsInvalid(securityDeposit, "", depositAmount = "25000")
        )
        assertFalse(
            workItemAttributeIsInvalid(parking, "", depositAmount = "")
        )
        assertFalse(
            workItemAttributeIsInvalid(parking, "", otherDraftOpen = true, otherDraftText = "", depositAmount = "")
        )
    }

    @Test
    fun `formatServiceTypeDisplayName converts owner slugs to Property Owner`() {
        assertEquals("Property Owner", serviceTypeDisplayName("Flat Owner"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("property_owner"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("property owner"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("flat_owner"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("flat owner"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("flat_owners"))
        assertEquals("Property Owner", formatServiceTypeDisplayName("flat_owner"))
        assertEquals("Broker", formatServiceTypeDisplayName("Broker"))
    }

    @Test
    fun `property type and preferred tenants support other option`() {
        assertTrue(workItemSupportsOtherOption("property_type"))
        assertTrue(workItemSupportsOtherOption("property_types"))
        assertTrue(workItemSupportsOtherOption("preferred_tenants"))
        assertTrue(workItemSupportsOtherOption("tenant_preference"))
        assertEquals("Other Preferred Tenant", workItemOtherFieldLabel("Preferred Tenants"))
        assertEquals("Other Property Type", workItemOtherFieldLabel("Property Type"))
    }

    @Test
    fun `work item title and duration fallbacks remain supported`() {
        assertEquals("Work item", defaultWorkItemTitle(null, null))
        assertEquals("Broker · Rent, Sell", defaultWorkItemTitle(null, "Rent, Sell"))
        assertEquals("Existing listing", defaultWorkItemTitle("Existing listing", "Rent"))
        assertEquals(60, defaultWorkItemDurationMinutes(null))
        assertEquals(45, defaultWorkItemDurationMinutes(45))
    }
}
