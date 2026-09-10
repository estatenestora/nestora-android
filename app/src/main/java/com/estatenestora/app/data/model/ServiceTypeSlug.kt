package com.estatenestora.app.data.model

fun canonicalServiceTypeSlug(raw: String): String {
    val slug = raw.trim().lowercase()
        .replace("-", "_")
        .replace(" ", "_")
    return when (slug) {
        "flat_owner", "flat_owners", "flatowner", "property_owners",
        "owner_flats", "owner_flat", "flats_owner" -> "property_owner"
        else -> slug
    }
}

fun looksLikePropertyOwnerServiceType(raw: String): Boolean {
    if (canonicalServiceTypeSlug(raw) == "property_owner") return true
    val compact = raw.lowercase().filter { it.isLetterOrDigit() }
    return compact.contains("flatowner") || compact.contains("propertyowner")
}

fun isPropertyOwnerServiceTypeSlug(raw: String): Boolean =
    looksLikePropertyOwnerServiceType(raw)

fun serviceTypeDisplayName(raw: String): String {
    if (looksLikePropertyOwnerServiceType(raw)) {
        return "Property Owner"
    }
    if (raw.contains('_')) {
        val titled = raw.trim().split('_').filter { it.isNotBlank() }.joinToString(" ") {
            it.replaceFirstChar { ch -> ch.uppercase() }
        }
        if (titled.equals("Flat Owner", ignoreCase = true)) {
            return "Property Owner"
        }
        return titled
    }
    return raw
}
