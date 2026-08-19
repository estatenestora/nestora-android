package com.estatenestora.app.data.model

import com.google.gson.annotations.SerializedName

// ── Core domain models ───────────────────────────────────────────────────────

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val servicesCount: Int = 0
)

data class ServiceType(
    val slug: String,
    val name: String,
    val emoji: String,
    val description: String,
    val categorySlug: String
)

data class ServiceListing(
    val id: String,
    val title: String,
    val categoryName: String,
    val serviceType: String,
    val providerName: String,
    val providerAvatarUrl: String? = null,
    val photoUrl: String? = null,
    val price: Double,
    val currency: String = "INR",
    val rating: Float = 0f,
    val matchScore: Int = 0,
    val isVerified: Boolean = false,
    val location: String,
    val badge: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val phone: String? = null
)

// Booking list-row shape — matches the backend's bookingSummaryDTO
// (android_booking_bridge.go) exactly. Powers BookingsScreen's My/Sent/Received
// tabs (split client-side by comparing customerUserId/providerUserId to the
// logged-in user's id) and the cheap GET_BOOKING_UPDATES polling path.
data class BookingSummary(
    @SerializedName("id") val id: String,
    @SerializedName("reference_code") val referenceCode: String,
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("listing_title") val listingTitle: String,
    @SerializedName("customer_user_id") val customerUserId: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("provider_user_id") val providerUserId: String,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("status") val status: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("stage_label") val stageLabel: String,
    @SerializedName("service_fee") val serviceFee: Double,
    @SerializedName("cancellation_fee") val cancellationFee: Double = 0.0,
    @SerializedName("is_home_service") val isHomeService: Boolean = false,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("unseen_by_customer") val unseenByCustomer: Boolean = false,
    @SerializedName("unseen_by_provider") val unseenByProvider: Boolean = false,
    @SerializedName("customer_address") val customerAddress: String = "",
    @SerializedName("provider_address") val providerAddress: String = ""
)

// Full detail shape for a single booking's tracking screen — matches the
// backend's bookingDetailDTO exactly. otpCode is only ever non-null when the
// backend's gating rule is met (viewer is the customer, status is an
// *_ARRIVED stage) — never trust/assume its presence client-side either.
data class BookingDetail(
    @SerializedName("id") val id: String,
    @SerializedName("reference_code") val referenceCode: String,
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("listing_title") val listingTitle: String,
    @SerializedName("customer_user_id") val customerUserId: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("provider_user_id") val providerUserId: String,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("status") val status: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("stage_label") val stageLabel: String,
    @SerializedName("service_fee") val serviceFee: Double,
    @SerializedName("cancellation_fee") val cancellationFee: Double = 0.0,
    @SerializedName("is_home_service") val isHomeService: Boolean = false,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("unseen_by_customer") val unseenByCustomer: Boolean = false,
    @SerializedName("unseen_by_provider") val unseenByProvider: Boolean = false,
    @SerializedName("problem_description") val problemDescription: String = "",
    @SerializedName("customer_address") val customerAddress: String = "",
    @SerializedName("customer_latitude") val customerLatitude: Double? = null,
    @SerializedName("customer_longitude") val customerLongitude: Double? = null,
    @SerializedName("otp_code") val otpCode: String? = null,
    @SerializedName("provider_arrived_at") val providerArrivedAt: String? = null,
    @SerializedName("customer_arrived_at") val customerArrivedAt: String? = null,
    @SerializedName("otp_verified_at") val otpVerifiedAt: String? = null,
    @SerializedName("cancellation_fee_pct") val cancellationFeePct: Double? = null,
    @SerializedName("created_at") val createdAt: String = "",
    // Live tracking — traveler is whichever party is currently *_EN_ROUTE;
    // null before travel starts / after arrival. Destination is server-picked
    // (customer's address for home service, else the provider's listing
    // location) so the client never has to branch on isHomeService itself.
    @SerializedName("traveler_latitude") val travelerLatitude: Double? = null,
    @SerializedName("traveler_longitude") val travelerLongitude: Double? = null,
    @SerializedName("traveler_location_updated_at") val travelerLocationUpdatedAt: String? = null,
    @SerializedName("customer_live_latitude") val customerLiveLatitude: Double? = null,
    @SerializedName("customer_live_longitude") val customerLiveLongitude: Double? = null,
    @SerializedName("customer_live_location_updated_at") val customerLiveLocationUpdatedAt: String? = null,
    @SerializedName("destination_latitude") val destinationLatitude: Double? = null,
    @SerializedName("destination_longitude") val destinationLongitude: Double? = null,
    @SerializedName("agreed_price") val agreedPrice: Double? = null,
    @SerializedName("commuting_fee") val commutingFee: Double = 0.0,
    @SerializedName("advance_amount") val advanceAmount: Double? = null,
    @SerializedName("advance_commission_pct") val advanceCommissionPct: Double? = null,
    @SerializedName("remaining_amount") val remainingAmount: Double = 0.0,
    @SerializedName("has_reviewed") val hasReviewed: Boolean = false,
    @SerializedName("customer_phone") val customerPhone: String = "",
    @SerializedName("provider_phone") val providerPhone: String = "",
    @SerializedName("payment_rejected") val paymentRejected: Boolean = false
) {
    fun toSummary(): BookingSummary = BookingSummary(
        id = id, referenceCode = referenceCode, listingId = listingId, listingTitle = listingTitle,
        customerUserId = customerUserId, customerName = customerName,
        providerUserId = providerUserId, providerName = providerName,
        status = status, stage = stage, stageLabel = stageLabel,
        serviceFee = serviceFee, cancellationFee = cancellationFee, isHomeService = isHomeService,
        updatedAt = updatedAt, unseenByCustomer = unseenByCustomer, unseenByProvider = unseenByProvider
    )
}

// Price/policy preview shown in BookingCreateSheet before the customer
// commits — matches the backend's bookingQuoteDTO (GET_BOOKING_QUOTE).
data class BookingQuote(
    @SerializedName("service_fee") val serviceFee: Double,
    @SerializedName("cancellation_policy") val cancellationPolicy: String,
    @SerializedName("provider_name") val providerName: String
)

data class PaymentInfo(
    @SerializedName("upi_id") val upiId: String,
    @SerializedName("payee_name") val payeeName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("txn_ref") val txnRef: String,
    @SerializedName("note") val note: String,
    // false when the viewer is the provider looking at a read-only billing
    // summary — only the customer (isPayer=true) can actually pay.
    @SerializedName("is_payer") val isPayer: Boolean = true
)

data class CancelPreview(
    @SerializedName("fee_amount") val feeAmount: Double,
    @SerializedName("fee_pct") val feePct: Double,
    @SerializedName("is_free") val isFree: Boolean,
    @SerializedName("policy_summary") val policySummary: String? = ""
)

data class UserProfile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("trust_badge") val trustBadge: String? = "PLATINUM",
    @SerializedName("active_bookings_count") val activeBookingsCount: Int = 0,
    @SerializedName("address") val address: String? = "",
    @SerializedName("profile_pic_url") val profilePicUrl: String? = "",
    @SerializedName("upi_id") val upiId: String? = "",
    // Whether this user owns any service_listings — there's no distinct
    // "PROVIDER" role in the backend, so this is how the Received-bookings
    // tab decides between an empty-state CTA and rendering real requests.
    @SerializedName("has_provider_listings") val hasProviderListings: Boolean = false
    ,@SerializedName("role") val role: String = "USER"
)

data class AdminPaymentReview(
    @SerializedName("booking_id") val bookingId: String,
    @SerializedName("reference_code") val referenceCode: String,
    @SerializedName("listing_title") val listingTitle: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("advance_amount") val advanceAmount: Double,
    @SerializedName("payment_screenshot") val paymentScreenshot: String = "",
    @SerializedName("submitted_at") val submittedAt: String = ""
)

// ── REST API Request & Response Models ───────────────────────────────────────

data class AndroidChatRequest(
    @SerializedName("query") val query: String,
    @SerializedName("user_id") val userId: String = ""
)

data class AndroidListingCard(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("title") val title: String,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("base_price") val basePrice: Double? = null,
    @SerializedName("pricing_model") val pricingModel: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("area") val area: String = "",
    @SerializedName("service_type") val serviceType: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("phone") val phone: String = ""
) {
    fun toServiceListing(): ServiceListing = ServiceListing(
        id = listingId,
        title = title,
        categoryName = category,
        serviceType = serviceType,
        providerName = providerName,
        price = basePrice ?: 0.0,
        rating = rating.toFloat(),
        isVerified = isVerified,
        location = if (area.isNotBlank()) "$area, $city" else city,
        badge = if (isVerified) "\u2705 VERIFIED" else "",
        phone = phone
    )
}

data class AndroidChatResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("reply") val reply: String,
    @SerializedName("intent") val intent: String,
    @SerializedName("listings") val listings: List<AndroidListingCard> = emptyList(),
    @SerializedName("total_results") val totalResults: Int = 0
)

data class AndroidCategoryItem(
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("description") val description: String = ""
) {
    fun toCategory(): Category = Category(
        id = slug,
        name = name,
        emoji = emoji,
        description = description
    )
}

data class AndroidServiceTypeItem(
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("category_slug") val categorySlug: String = ""
) {
    fun toServiceType(): ServiceType = ServiceType(
        slug = slug,
        name = name,
        emoji = emoji,
        description = description,
        categorySlug = categorySlug
    )
}

// ── Chat UI model ─────────────────────────────────────────────────────────────

data class TelegramChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String,
    val isUser: Boolean,
    val status: String = "SENT",
    val attachedListings: List<ServiceListing> = emptyList(),
    val aisoGap: AisoGapField? = null
)

data class QuickPromptChip(
    val label: String,
    val icon: String,
    val promptText: String
)

// ── Telegram Bot API request/response models ─────────────────────────────────

data class TelegramSendRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("text") val text: String
)

data class TelegramSendResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: TelegramMessageResult? = null
)

data class TelegramMessageResult(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("text") val text: String? = null,
    @SerializedName("date") val date: Long = 0L
)

data class TelegramGetUpdatesResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: List<TelegramUpdate> = emptyList()
)

data class TelegramUpdate(
    @SerializedName("update_id") val updateId: Long,
    @SerializedName("message") val message: TelegramUpdateMessage? = null
)

data class TelegramUpdateMessage(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("text") val text: String? = null,
    @SerializedName("date") val date: Long = 0L,
    @SerializedName("from") val from: TelegramUpdateUser? = null
)

data class TelegramUpdateUser(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean = false,
    @SerializedName("first_name") val firstName: String = ""
)

// ── Android bridge response models ───────────────────────────────────────────

data class AisoGapField(
    @SerializedName("field_type") val fieldType: String,
    @SerializedName("key") val key: String,
    @SerializedName("display_label") val displayLabel: String,
    @SerializedName("input_type") val inputType: String,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("is_required") val isRequired: Boolean = false,
    @SerializedName("hint_text") val hintText: String? = null
)

/**
 * Dynamic attribute template for a service type — returned by GET_SERVICE_ATTRS.
 * Drives the dynamic attribute section of the manual (Fill) registration form:
 * - "text" / "url" / "email" / "phone" → OutlinedTextField with appropriate keyboard
 * - "number"       → numeric OutlinedTextField
 * - "boolean"      → Yes / No toggle chip pair
 * - "select"       → single-choice dropdown
 * - "multiselect"  → multi-choice FilterChip row
 */
data class ServiceAttributeTemplate(
    @SerializedName("key") val key: String,
    @SerializedName("display_label") val displayLabel: String,
    @SerializedName("input_type") val inputType: String,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("is_required") val isRequired: Boolean = false,
    @SerializedName("hint_text") val hintText: String? = null
)

data class AndroidBridgeResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("intent") val intent: String,
    @SerializedName("reply") val reply: String,
    @SerializedName("total_results") val totalResults: Int = 0,
    @SerializedName("listings") val listings: List<AndroidBridgeListing>? = null,
    @SerializedName("profile") val profile: UserProfile? = null,
    @SerializedName("categories") val categories: List<AndroidCategoryItem>? = null,
    @SerializedName("service_types") val serviceTypes: List<AndroidServiceTypeItem>? = null,
    @SerializedName("service_attributes") val serviceAttributes: List<ServiceAttributeTemplate>? = null,
    @SerializedName("listing_id") val listingId: String? = null,
    // Ad-hoc snapshot of collected fields once an Auto Register conversation
    // (AISO_PARSE) is ready to confirm — deliberately untyped (JsonObject, not
    // UserProfile) since its shape (category_slug/base_price/attributes/...)
    // doesn't match any fixed model.
    @SerializedName("aiso_summary") val aisoSummary: com.google.gson.JsonObject? = null,
    @SerializedName("geocode_result") val geocodeResult: GeocodePlace? = null,
    @SerializedName("places") val places: List<GeocodePlace>? = null,
    @SerializedName("aiso_gap") val aisoGap: AisoGapField? = null,
    // Base64-encoded raw image bytes for GET_PHOTO — see NestoraRepository.getLocalPhotoPath.
    @SerializedName("photo_b64") val photoB64: String? = null,
    // Booking bridge fields — see android_booking_bridge.go.
    @SerializedName("booking") val booking: BookingDetail? = null,
    @SerializedName("bookings") val bookings: List<BookingSummary>? = null,
    @SerializedName("quote") val quote: BookingQuote? = null,
    @SerializedName("booking_id") val bookingId: String? = null,
    @SerializedName("payment_info") val paymentInfo: PaymentInfo? = null,
    @SerializedName("cancel_preview") val cancelPreview: CancelPreview? = null
    ,@SerializedName("admin_payments") val adminPayments: List<AdminPaymentReview>? = null
)

// A single resolved location — either the one result of a reverse-geocode
// lookup (GEOCODE_REVERSE) or one candidate row of an address search
// (GEOCODE_SEARCH). Same shape either way, so one model covers both.
data class GeocodePlace(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class AndroidBridgeListing(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("title") val title: String,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("base_price") val basePrice: Double? = null,
    @SerializedName("pricing_model") val pricingModel: String = "",
    @SerializedName("city") val city: String = "",
    @SerializedName("area") val area: String = "",
    @SerializedName("service_type") val serviceType: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("phone") val phone: String = ""
) {
    fun toServiceListing(): ServiceListing = ServiceListing(
        id = listingId,
        title = title,
        categoryName = category,
        serviceType = serviceType,
        providerName = providerName,
        price = basePrice ?: 0.0,
        rating = rating.toFloat(),
        isVerified = isVerified,
        location = if (area.isNotBlank()) "$area, $city" else city,
        badge = if (isVerified) "\u2705 VERIFIED" else "",
        phone = phone
    )
}
