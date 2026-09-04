package com.estatenestora.app.data.model

import com.google.gson.annotations.SerializedName

// ── Core domain models ───────────────────────────────────────────────────────

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val servicesCount: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true,
    val backendId: String = ""
)

data class ServiceType(
    val slug: String,
    val name: String,
    val emoji: String,
    val description: String,
    val categorySlug: String,
    @SerializedName("is_active") val isActive: Boolean = true,
    val backendId: String = ""
)

data class ServiceListing(
    val id: String,
    val title: String,
    val description: String = "",
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
    val phone: String? = null,
    val isActive: Boolean = true,
    val totalBookingCount: Int = 0,
    val openBookingCount: Int = 0,
    val requestedBookingCount: Int = 0,
    val workItemCount: Int = 0,
    val packageCount: Int = 0,
    val serviceRadiusKm: Int = 5,
    val tagline: String = "",
    val pricingModel: String = "",
    val currencyCode: String = "INR",
    val unitLabel: String = "",
    val platformNote: String = "",
    val isNegotiable: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mediaUrls: List<String> = emptyList()
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
    @SerializedName("viewer_role") val viewerRole: String = "",
    @SerializedName("status") val status: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("stage_label") val stageLabel: String,
    @SerializedName("customer_message") val customerMessage: String = "",
    @SerializedName("provider_message") val providerMessage: String = "",
    @SerializedName("service_fee") val serviceFee: Double,
    @SerializedName("service_scope") val serviceScope: BookingServiceScope? = null,
    @SerializedName("cancellation_fee") val cancellationFee: Double = 0.0,
    @SerializedName("is_home_service") val isHomeService: Boolean = false,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("unseen_by_customer") val unseenByCustomer: Boolean = false,
    @SerializedName("unseen_by_provider") val unseenByProvider: Boolean = false,
    @SerializedName("customer_address") val customerAddress: String = "",
    @SerializedName("provider_address") val providerAddress: String = ""
)

/** Compact, server-derived scope for booking list cards. The full immutable
 * package/item snapshot is still returned only by GET_BOOKING. */
data class BookingServiceScope(
    @SerializedName("kind") val kind: String,
    @SerializedName("label") val label: String,
    @SerializedName("item_count") val itemCount: Int,
    @SerializedName("provider_amount") val providerAmount: Double,
    @SerializedName("duration_minutes") val durationMinutes: Int
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
    @SerializedName("viewer_role") val viewerRole: String = "",
    @SerializedName("status") val status: String,
    @SerializedName("stage") val stage: String,
    @SerializedName("stage_label") val stageLabel: String,
    @SerializedName("customer_message") val customerMessage: String = "",
    @SerializedName("provider_message") val providerMessage: String = "",
    @SerializedName("service_fee") val serviceFee: Double,
    @SerializedName("service_scope") val serviceScope: BookingServiceScope? = null,
    @SerializedName("cancellation_fee") val cancellationFee: Double = 0.0,
    @SerializedName("is_home_service") val isHomeService: Boolean = false,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("unseen_by_customer") val unseenByCustomer: Boolean = false,
    @SerializedName("unseen_by_provider") val unseenByProvider: Boolean = false,
    @SerializedName("problem_description") val problemDescription: String = "",
    @SerializedName("service_selection") val serviceSelection: com.google.gson.JsonObject? = null,
    @SerializedName("customer_address") val customerAddress: String = "",
    @SerializedName("customer_latitude") val customerLatitude: Double? = null,
    @SerializedName("customer_longitude") val customerLongitude: Double? = null,
    @SerializedName("otp_code") val otpCode: String? = null,
    @SerializedName("provider_arrived_at") val providerArrivedAt: String? = null,
    @SerializedName("customer_arrived_at") val customerArrivedAt: String? = null,
    @SerializedName("otp_verified_at") val otpVerifiedAt: String? = null,
    @SerializedName("cancellation_fee_pct") val cancellationFeePct: Double? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("scheduled_start_at") val scheduledStartAt: String? = null,
    @SerializedName("scheduled_end_at") val scheduledEndAt: String? = null,
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
    @SerializedName("platform_fee_amount") val platformFeeAmount: Double = 0.0,
    @SerializedName("platform_gst_amount") val platformGstAmount: Double = 0.0,
    @SerializedName("platform_fee_kind") val platformFeeKind: String = "",
    @SerializedName("platform_fee_paid_at") val platformFeePaidAt: String? = null,
    @SerializedName("remaining_amount") val remainingAmount: Double = 0.0,
    @SerializedName("has_reviewed") val hasReviewed: Boolean = false,
    @SerializedName("customer_phone") val customerPhone: String = "",
    @SerializedName("provider_phone") val providerPhone: String = "",
    @SerializedName("payment_rejected") val paymentRejected: Boolean = false,
    @SerializedName("visit_kind") val visitKind: String = "SERVICE",
    @SerializedName("engagement_id") val engagementId: String = "",
    @SerializedName("requires_provider_quote") val requiresProviderQuote: Boolean = false,
    @SerializedName("request_answers") val requestAnswers: Map<String, String> = emptyMap(),
    @SerializedName("cancellation_payment_payer_user_id") val cancellationPaymentPayerUserId: String = "",
    @SerializedName("cancellation_payment_payee_user_id") val cancellationPaymentPayeeUserId: String = "",
    @SerializedName("cancellation_payment_state") val cancellationPaymentState: String = ""
) {
    fun toSummary(): BookingSummary = BookingSummary(
        id = id, referenceCode = referenceCode, listingId = listingId, listingTitle = listingTitle,
        customerUserId = customerUserId, customerName = customerName,
        providerUserId = providerUserId, providerName = providerName,
        viewerRole = viewerRole,
        status = status, stage = stage, stageLabel = stageLabel, customerMessage = customerMessage, providerMessage = providerMessage,
        serviceFee = serviceFee, serviceScope = serviceScope, cancellationFee = cancellationFee, isHomeService = isHomeService,
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
    @SerializedName("platform_fee_amount") val platformFeeAmount: Double = 0.0,
    @SerializedName("platform_gst_amount") val platformGstAmount: Double = 0.0,
    @SerializedName("platform_fee_kind") val platformFeeKind: String = "",
    @SerializedName("provider_estimate") val providerEstimate: Double = 0.0,
    @SerializedName("provider_estimate_message") val providerEstimateMessage: String = "",
    @SerializedName("cancellation_policy") val cancellationPolicy: String = "",
    // false when the viewer is the provider looking at a read-only billing
    // summary — only the customer (isPayer=true) can actually pay.
    @SerializedName("is_payer") val isPayer: Boolean = true
)

data class CancelPreview(
    @SerializedName("fee_amount") val feeAmount: Double,
    @SerializedName("fee_pct") val feePct: Double,
    @SerializedName("is_free") val isFree: Boolean,
    @SerializedName("policy_summary") val policySummary: String? = "",
    @SerializedName("direct_payment_required") val directPaymentRequired: Boolean = false,
    @SerializedName("payer_role") val payerRole: String = "",
    @SerializedName("payee_role") val payeeRole: String = "",
    @SerializedName("payee_upi_id") val payeeUpiId: String = ""
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

/** Authoritative, single-read operational summary for the provider home. */
data class ProviderDashboardSummary(
    @SerializedName("verification_status") val verificationStatus: String = "NOT_REGISTERED",
    @SerializedName("tier") val tier: String = "FREE",
    @SerializedName("is_available") val isAvailable: Boolean = false,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("review_count") val reviewCount: Int = 0,
    @SerializedName("response_rate_pct") val responseRatePct: Double = 0.0,
    @SerializedName("profile_score") val profileScore: Int = 0,
    @SerializedName("total_listings") val totalListings: Int = 0,
    @SerializedName("active_listings") val activeListings: Int = 0,
    @SerializedName("inactive_listings") val inactiveListings: Int = 0,
    @SerializedName("requested_jobs") val requestedJobs: Int = 0,
    @SerializedName("unseen_requests") val unseenRequests: Int = 0,
    @SerializedName("active_jobs") val activeJobs: Int = 0,
    @SerializedName("today_jobs") val todayJobs: Int = 0,
    @SerializedName("upcoming_jobs") val upcomingJobs: Int = 0,
    @SerializedName("completed_jobs") val completedJobs: Int = 0,
    @SerializedName("ended_jobs") val endedJobs: Int = 0,
    @SerializedName("disputed_jobs") val disputedJobs: Int = 0,
    @SerializedName("wallet_balance") val walletBalance: Double = 0.0
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
    @SerializedName("pricing_model") val pricingModel: String? = null,
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
    @SerializedName("id") val id: String = "",
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("description") val description: String = ""
) {
    fun toCategory(): Category = Category(
        id = slug,
        name = name,
        emoji = emoji,
        description = description,
        backendId = id
    )
}

data class AndroidServiceTypeItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("category_slug") val categorySlug: String = "",
    @SerializedName("is_active") val isActive: Boolean = true
) {
    fun toServiceType(): ServiceType = ServiceType(
        slug = slug,
        name = name,
        emoji = emoji,
        description = description,
        categorySlug = categorySlug,
        isActive = isActive,
        backendId = id
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
    @SerializedName("registration_token") val registrationToken: String? = null,
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
    ,@SerializedName("engagement_draft") val engagementDraft: EngagementDraft? = null
    ,@SerializedName("booking_policy") val bookingPolicy: BookingPolicy? = null
    ,@SerializedName("engagement_id") val engagementId: String? = null
    ,@SerializedName("engagement_plan") val engagementPlan: EngagementPlan? = null
    ,@SerializedName("engagement_quotes") val engagementQuotes: List<EngagementQuote>? = null
    ,@SerializedName("capacity_reservation") val capacityReservation: CapacityReservation? = null
    ,@SerializedName("listing_capacity") val listingCapacity: CapacityPoolSettings? = null
    ,@SerializedName("availability_slots") val availabilitySlots: List<AvailabilitySlot> = emptyList()
    ,@SerializedName("provider_availability") val providerAvailability: ProviderAvailabilitySettings? = null
    ,@SerializedName("attachment_upload") val attachmentUpload: EngagementAttachmentUpload? = null
    ,@SerializedName("listing_activation") val listingActivation: ListingActivation? = null,
    @SerializedName("provider_dashboard") val providerDashboard: ProviderDashboardSummary? = null,
    @SerializedName("service_catalog") val serviceCatalog: ListingServiceCatalog? = null,
    @SerializedName("media_upload") val mediaUpload: MediaUploadSession? = null,
    @SerializedName("media_assets") val mediaAssets: List<MediaAsset>? = null,
    @SerializedName("wallet_balance") val walletBalance: Double? = null
)

data class MediaVariant(
    @SerializedName("variant") val variant: String,
    @SerializedName("telegram_file_id") val telegramFileId: String,
    @SerializedName("telegram_file_unique_id") val telegramFileUniqueId: String = "",
    @SerializedName("mime_type") val mimeType: String = "image/jpeg",
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("byte_size") val byteSize: Int,
    @SerializedName("checksum") val checksum: String = ""
)

data class MediaAsset(
    @SerializedName("id") val id: String,
    @SerializedName("owner_kind") val ownerKind: String,
    @SerializedName("owner_provider_id") val ownerProviderId: String = "",
    @SerializedName("scope") val scope: String,
    @SerializedName("scope_id") val scopeId: String = "",
    @SerializedName("role") val role: String = "PRIMARY",
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("action_label") val actionLabel: String = "",
    @SerializedName("action_value") val actionValue: String = "",
    @SerializedName("status") val status: String = "ACTIVE",
    @SerializedName("display_order") val displayOrder: Int = 0,
    @SerializedName("variants") val variants: List<MediaVariant> = emptyList()
) {
    fun fileIdFor(preferred: String): String? =
        variants.firstOrNull { it.variant.equals(preferred, ignoreCase = true) }?.telegramFileId
            ?: variants.firstOrNull { it.variant.equals("CARD", ignoreCase = true) }?.telegramFileId
            ?: variants.firstOrNull()?.telegramFileId
}

data class MediaUploadSession(
    @SerializedName("token") val token: String,
    @SerializedName("scope") val scope: String,
    @SerializedName("scope_id") val scopeId: String = "",
    @SerializedName("role") val role: String = "PRIMARY",
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("owner_kind") val ownerKind: String,
    @SerializedName("provider_id") val providerId: String = ""
)

data class ListingActivation(
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("total_booking_count") val totalBookingCount: Int = 0,
    @SerializedName("open_booking_count") val openBookingCount: Int,
    @SerializedName("requested_booking_count") val requestedBookingCount: Int
)

data class AvailabilitySlot(
    @SerializedName("start_at") val startAt: String,
    @SerializedName("end_at") val endAt: String
)

data class ProviderAvailabilitySettings(
    @SerializedName("preset") val preset: String = "ASAP_ONLY",
    @SerializedName("supported") val supported: Boolean = false,
	@SerializedName("supports_now") val supportsNow: Boolean = false,
    @SerializedName("days") val days: List<Int>? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("end_time") val endTime: String? = null
)

data class EngagementDraft(
    @SerializedName("id") val id: String,
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("status") val status: String,
    @SerializedName("service_selection") val serviceSelection: com.google.gson.JsonObject? = null,
    @SerializedName("request_note") val requestNote: String = "",
    @SerializedName("is_home_service") val isHomeService: Boolean = true,
    @SerializedName("customer_address") val customerAddress: String = "",
    @SerializedName("requested_start_at") val requestedStartAt: String? = null,
    @SerializedName("requested_end_at") val requestedEndAt: String? = null,
    @SerializedName("recurrence_kind") val recurrenceKind: String = "ONE_TIME",
    @SerializedName("time_term") val timeTerm: String = "NOW",
    @SerializedName("time_preference") val timePreference: com.google.gson.JsonObject? = null,
    @SerializedName("timezone") val timezone: String = "Asia/Kolkata",
    @SerializedName("engagement_id") val engagementId: String? = null,
    @SerializedName("expires_at") val expiresAt: String
)

/** Customer-safe catalogue for one listing. The backend fixes its provider and
 * service type, so this model can never become a multi-provider cart. */
data class ListingServiceCatalog(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("provider_id") val providerId: String,
    @SerializedName("service_type_id") val serviceTypeId: String,
    @SerializedName("offerings") val offerings: List<ProviderServiceOffering> = emptyList(),
    @SerializedName("packages") val packages: List<ProviderServicePackage> = emptyList(),
    @SerializedName("listing_media") val listingMedia: MediaAsset? = null
)

data class ProviderServiceOffering(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("attribute_values") val attributeValues: com.google.gson.JsonObject? = null,
    @SerializedName("price_amount") val priceAmount: Double,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("display_order") val displayOrder: Int = 0,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("media") val media: MediaAsset? = null
)

data class ProviderServicePackage(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("included_text") val includedText: String = "",
    @SerializedName("excluded_text") val excludedText: String = "",
    @SerializedName("package_price_amount") val packagePriceAmount: Double,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("status") val status: String = "PUBLISHED",
    @SerializedName("display_order") val displayOrder: Int = 0,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("items") val items: List<ProviderServiceOffering> = emptyList(),
    @SerializedName("media") val media: MediaAsset? = null
)

data class BookingPolicy(
    @SerializedName("service_type_id") val serviceTypeId: String,
    @SerializedName("policy_version") val policyVersion: Int,
    @SerializedName("fulfillment_model") val fulfillmentModel: String,
    @SerializedName("timing_modes") val timingModes: List<String> = emptyList(),
    @SerializedName("time_terms") val timeTerms: List<String> = emptyList(),
    @SerializedName("commitment_gate") val commitmentGate: String,
    @SerializedName("price_mode") val priceMode: String,
    @SerializedName("default_duration_minutes") val defaultDurationMinutes: Int,
    @SerializedName("draft_ttl_minutes") val draftTtlMinutes: Int,
    @SerializedName("request_schema") val requestSchema: com.google.gson.JsonArray? = null,
    @SerializedName("ui_defaults") val uiDefaults: com.google.gson.JsonObject? = null,
    @SerializedName("provider_preset") val providerPreset: String = "ASAP_ONLY"
)

data class EngagementAttachmentUpload(
    @SerializedName("upload_token") val uploadToken: String,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("status") val status: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class EngagementVisit(
    @SerializedName("booking_id") val bookingId: String = "",
    @SerializedName("sequence_no") val sequenceNo: Int,
    @SerializedName("start_at") val startAt: String,
    @SerializedName("end_at") val endAt: String,
    @SerializedName("status") val status: String
)

data class EngagementPlan(
    @SerializedName("engagement_id") val engagementId: String,
    @SerializedName("status") val status: String,
    @SerializedName("is_recurring") val isRecurring: Boolean = false,
    @SerializedName("recurrence_rule") val recurrenceRule: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("paused_at") val pausedAt: String? = null,
    @SerializedName("next_visit") val nextVisit: EngagementVisit? = null,
    @SerializedName("upcoming_visits") val upcomingVisits: List<EngagementVisit> = emptyList()
)

/** App-safe status for one inventory or subscription reservation. */
data class CapacityReservation(
    @SerializedName("engagement_id") val engagementId: String,
    @SerializedName("status") val status: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("subscription_status") val subscriptionStatus: String = "",
    @SerializedName("next_billing_at") val nextBillingAt: String? = null
)

/** Provider-managed settings for a finite inventory/subscription listing. */
data class CapacityPoolSettings(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("capacity_units") val capacityUnits: Int,
    @SerializedName("hold_ttl_minutes") val holdTtlMinutes: Int,
    @SerializedName("billing_period") val billingPeriod: String,
    @SerializedName("is_active") val isActive: Boolean
)

data class EngagementQuote(
    @SerializedName("id") val id: String,
    @SerializedName("engagement_id") val engagementId: String,
    @SerializedName("revision_no") val revisionNo: Int,
    @SerializedName("scope_summary") val scopeSummary: String,
    @SerializedName("visit_fee_paise") val visitFeePaise: Long,
    @SerializedName("labour_paise") val labourPaise: Long,
    @SerializedName("materials_paise") val materialsPaise: Long,
    @SerializedName("total_paise") val totalPaise: Long,
    @SerializedName("currency") val currency: String,
    @SerializedName("status") val status: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("can_customer_act") val canCustomerAct: Boolean = false
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
    @SerializedName("description") val description: String = "",
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
    @SerializedName("phone") val phone: String = "",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("total_booking_count") val totalBookingCount: Int = 0,
    @SerializedName("open_booking_count") val openBookingCount: Int = 0,
    @SerializedName("requested_booking_count") val requestedBookingCount: Int = 0,
    @SerializedName("work_item_count") val workItemCount: Int = 0,
    @SerializedName("package_count") val packageCount: Int = 0,
    @SerializedName("service_radius_km") val serviceRadiusKm: Int = 5,
    @SerializedName("tagline") val tagline: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("unit_label") val unitLabel: String? = null,
    @SerializedName("platform_note") val platformNote: String? = null,
    @SerializedName("is_negotiable") val isNegotiable: Boolean = false,
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("attributes") val attributes: Map<String, String>? = null,
    @SerializedName("media_urls") val mediaUrls: List<String>? = null
) {
    fun toServiceListing(): ServiceListing = ServiceListing(
        id = listingId,
        title = title,
        description = description,
        categoryName = category,
        serviceType = serviceType,
        providerName = providerName,
        price = basePrice ?: 0.0,
        rating = rating.toFloat(),
        isVerified = isVerified,
        location = if (area.isNotBlank()) "$area, $city" else city,
        badge = if (isVerified) "\u2705 VERIFIED" else "",
        phone = phone,
        isActive = isActive,
        totalBookingCount = totalBookingCount,
        openBookingCount = openBookingCount,
        requestedBookingCount = requestedBookingCount,
        workItemCount = workItemCount,
        packageCount = packageCount,
        serviceRadiusKm = serviceRadiusKm,
        tagline = tagline.orEmpty(),
        pricingModel = pricingModel.orEmpty(),
        currencyCode = currency.orEmpty().ifBlank { "INR" },
        unitLabel = unitLabel.orEmpty(),
        platformNote = platformNote.orEmpty(),
        isNegotiable = isNegotiable,
        latitude = latitude,
        longitude = longitude,
        attributes = attributes.orEmpty(),
        mediaUrls = mediaUrls.orEmpty()
    )
}

/** Complete non-availability change set from My Listings' editor. */
data class ListingEditorUpdate(
    @SerializedName("listing_id") val listingId: String,
    @SerializedName("title") val title: String,
    @SerializedName("tagline") val tagline: String,
    @SerializedName("description") val description: String,
    @SerializedName("base_price") val basePrice: Double,
    @SerializedName("pricing_model") val pricingModel: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("unit_label") val unitLabel: String,
    @SerializedName("platform_note") val platformNote: String,
    @SerializedName("is_negotiable") val isNegotiable: Boolean,
    @SerializedName("location") val location: String,
    @SerializedName("city") val city: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("service_radius_km") val serviceRadiusKm: Int,
    @SerializedName("attributes") val attributes: Map<String, String>,
    @SerializedName("media_urls") val mediaUrls: List<String>
)
