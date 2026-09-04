package com.estatenestora.app.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Nestora In-App Localisation System
 *
 * Architecture:
 * - Pure Compose CompositionLocal approach — no Activity.recreate() needed.
 * - Language switching is instant, pure UI, and does NOT touch any API call,
 *   database query, or Telegram bridge payload.
 * - Language preference is persisted in SharedPreferences ("nestora_prefs", key "app_language").
 * - Screens access strings via: val str = LocalNestoraStrings.current
 *
 * Supported languages: English (en), Hindi (hi), Bengali (bn)
 */

// ─────────────────────────────────────────────────────────────────────────────
// String Contract
// ─────────────────────────────────────────────────────────────────────────────

data class NestoraStrings(
    // ── Navigation bar
    val navExplore: String,
    val navFinder: String,
    val navAccount: String,
    val navDashboard: String,
    val navRegister: String,
    val navListings: String,
    val navBookings: String,

    // ── Home Screen
    val homeGreetingMorning: String,
    val homeGreetingAfternoon: String,
    val homeGreetingEvening: String,
    val homeSubtitle: String,
    val homePopularServices: String,
    val homeTrendingNearYou: String,
    val homeTopRated: String,
    val homeSearchPlaceholder: String,
    val homeSearchHint1: String,
    val homeSearchHint2: String,

    // ── Bookings Screen
    val bookingsTitle: String,
    val bookingsActiveTab: String,
    val bookingsHistoryTab: String,
    val bookingsEmpty: String,
    val bookingsEmptySub: String,
    val bookingsExploreNow: String,
    val bookingsActiveRequests: String,
    val bookingsActiveBookings: String,

    // ── Profile Screen
    val profileTitle: String,
    val profileEditProfile: String,
    val profileSaveChanges: String,
    val profileCancel: String,
    val profileName: String,
    val profilePhone: String,
    val profileAddress: String,
    val profileLanguage: String,
    val profileChooseLanguage: String,
    val profileLogout: String,
    val profileLogoutConfirm: String,
    val profileMyBookings: String,
    val profilePaymentModes: String,
    val profileReferEarn: String,
    val profileHelpSupport: String,
    val profileNestoraMoney: String,
    val profileAdminPayments: String,
    val profileAdminMedia: String,
    val profileSwitchToProvider: String,
    val profileSwitchToCustomer: String,
    val profileProviderMode: String,
    val profileCustomerMode: String,

    // ── Nestora Money Screen
    val moneyTitle: String,
    val moneyAvailableBalance: String,
    val moneyAddBalance: String,
    val moneyTransactionHistory: String,
    val moneyHowItWorks: String,
    val moneySecurePayments: String,
    val moneyInstantCredit: String,
    val moneyUsedAt: String,
    val moneyProceedToAdd: String,
    val moneyAmount: String,
    val moneyEnterAmount: String,
    val moneySelectAmount: String,
    val moneyNoteTitle: String,
    val moneyNote1: String,
    val moneyNote2: String,
    val moneyNote3: String,
    val moneyNote4: String,
    val moneyLoading: String,

    // ── Booking Flow
    val bookingSchedule: String,
    val bookingFlexible: String,
    val bookingSelectDate: String,
    val bookingSelectTime: String,
    val bookingServiceLocation: String,
    val bookingChangeLocation: String,
    val bookingSendRequest: String,
    val bookingServiceQuestions: String,
    val bookingSafetyInfo: String,
    val bookingStatus: String,
    val bookingStatusPending: String,
    val bookingStatusConfirmed: String,
    val bookingStatusEnRoute: String,
    val bookingStatusCompleted: String,
    val bookingStatusCancelled: String,

    // ── Finder / Search
    val finderTitle: String,
    val finderSearchPlaceholder: String,
    val finderNearby: String,
    val finderNoResults: String,
    val finderTryPlumber: String,
    val finderTryMaid: String,

    // ── Location & Permissions
    val locationTitle: String,
    val locationSubtitle: String,
    val locationAllow: String,
    val locationSkip: String,
    val notificationTitle: String,
    val notificationSubtitle: String,
    val notificationAllow: String,
    val notificationSkip: String,

    // ── General / Common
    val commonLoading: String,
    val commonError: String,
    val commonRetry: String,
    val commonDone: String,
    val commonNext: String,
    val commonBack: String,
    val commonClose: String,
    val commonSearch: String,
    val commonSave: String,
    val commonEdit: String,
    val commonDelete: String,
    val commonConfirm: String,
    val commonBookNow: String,
    val commonViewDetails: String,
    val commonViewAll: String,
    val commonRating: String,
    val commonReviews: String,
    val commonMinutes: String,
    val commonHours: String,
    val commonFrom: String,
    val commonPerHour: String,
    val commonOff: String,

    // ── Language names (for the picker UI itself — always shown in their own script)
    val langEnglish: String,
    val langHindi: String,
    val langBengali: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// English Strings
// ─────────────────────────────────────────────────────────────────────────────

val EnglishStrings = NestoraStrings(
    navExplore = "Explore",
    navFinder = "Finder",
    navAccount = "Account",
    navDashboard = "Dashboard",
    navRegister = "Register",
    navListings = "Listings",
    navBookings = "Bookings",

    homeGreetingMorning = "Good morning",
    homeGreetingAfternoon = "Good afternoon",
    homeGreetingEvening = "Good evening",
    homeSubtitle = "What service do you need today?",
    homePopularServices = "Popular Services",
    homeTrendingNearYou = "Trending Near You",
    homeTopRated = "Top Rated Providers",
    homeSearchPlaceholder = "Search for a service...",
    homeSearchHint1 = "Try 'Plumber'",
    homeSearchHint2 = "Try 'Maid'",

    bookingsTitle = "My Bookings",
    bookingsActiveTab = "Active bookings",
    bookingsHistoryTab = "History",
    bookingsEmpty = "No bookings yet",
    bookingsEmptySub = "Find and book a service to get started",
    bookingsExploreNow = "Explore Now",
    bookingsActiveRequests = "Active requests",
    bookingsActiveBookings = "Active bookings",

    profileTitle = "Account",
    profileEditProfile = "Edit Profile",
    profileSaveChanges = "Save Changes",
    profileCancel = "Cancel",
    profileName = "Full Name",
    profilePhone = "Phone Number",
    profileAddress = "Address",
    profileLanguage = "Language",
    profileChooseLanguage = "Choose Language",
    profileLogout = "Log Out",
    profileLogoutConfirm = "Are you sure you want to log out?",
    profileMyBookings = "My Bookings",
    profilePaymentModes = "Payment Modes",
    profileReferEarn = "Refer & Earn",
    profileHelpSupport = "Help & Support",
    profileNestoraMoney = "Nestora Money",
    profileAdminPayments = "Admin Payments",
    profileAdminMedia = "Admin Media",
    profileSwitchToProvider = "Switch to Provider Mode",
    profileSwitchToCustomer = "Switch to Customer Mode",
    profileProviderMode = "Provider Mode",
    profileCustomerMode = "Customer Mode",

    moneyTitle = "Nestora Money",
    moneyAvailableBalance = "Available Balance",
    moneyAddBalance = "Add Balance",
    moneyTransactionHistory = "Transaction History",
    moneyHowItWorks = "How it works",
    moneySecurePayments = "Secure Payments",
    moneyInstantCredit = "Instant Credit",
    moneyUsedAt = "Used at checkout for any Nestora service",
    moneyProceedToAdd = "Proceed to Add Balance",
    moneyAmount = "Amount",
    moneyEnterAmount = "Enter amount",
    moneySelectAmount = "Select an amount",
    moneyNoteTitle = "Please note",
    moneyNote1 = "This amount will be added to your Nestora wallet",
    moneyNote2 = "Wallet balance can be used to pay for any service on Nestora",
    moneyNote3 = "Refunds, if applicable, will be credited back to this wallet",
    moneyNote4 = "Wallet balance is non-transferable and non-refundable to bank account",
    moneyLoading = "Loading your balance...",

    bookingSchedule = "Schedule",
    bookingFlexible = "Flexible",
    bookingSelectDate = "Select Date",
    bookingSelectTime = "Select Time",
    bookingServiceLocation = "Service Location",
    bookingChangeLocation = "Change Location",
    bookingSendRequest = "Send Request",
    bookingServiceQuestions = "Service Questions",
    bookingSafetyInfo = "Safety Information",
    bookingStatus = "Booking Status",
    bookingStatusPending = "Pending",
    bookingStatusConfirmed = "Confirmed",
    bookingStatusEnRoute = "Provider En Route",
    bookingStatusCompleted = "Completed",
    bookingStatusCancelled = "Cancelled",

    finderTitle = "Find Services",
    finderSearchPlaceholder = "Search plumbers, maids, electricians...",
    finderNearby = "Nearby",
    finderNoResults = "No results found",
    finderTryPlumber = "Try 'Plumber'",
    finderTryMaid = "Try 'Maid'",

    locationTitle = "Allow Location Access",
    locationSubtitle = "We need your location to show services near you",
    locationAllow = "Allow Location",
    locationSkip = "Skip for now",
    notificationTitle = "Stay Updated",
    notificationSubtitle = "Allow notifications to receive booking updates",
    notificationAllow = "Allow Notifications",
    notificationSkip = "Skip for now",

    commonLoading = "Loading...",
    commonError = "Something went wrong",
    commonRetry = "Retry",
    commonDone = "Done",
    commonNext = "Next",
    commonBack = "Back",
    commonClose = "Close",
    commonSearch = "Search",
    commonSave = "Save",
    commonEdit = "Edit",
    commonDelete = "Delete",
    commonConfirm = "Confirm",
    commonBookNow = "Book Now",
    commonViewDetails = "View Details",
    commonViewAll = "View All",
    commonRating = "Rating",
    commonReviews = "reviews",
    commonMinutes = "mins",
    commonHours = "hrs",
    commonFrom = "from",
    commonPerHour = "per hr",
    commonOff = "OFF",

    langEnglish = "English",
    langHindi = "हिन्दी",
    langBengali = "বাংলা",
)

// ─────────────────────────────────────────────────────────────────────────────
// Hindi Strings (हिन्दी)
// ─────────────────────────────────────────────────────────────────────────────

val HindiStrings = NestoraStrings(
    navExplore = "एक्सप्लोर",
    navFinder = "खोजें",
    navAccount = "खाता",
    navDashboard = "डैशबोर्ड",
    navRegister = "पंजीकरण",
    navListings = "लिस्टिंग",
    navBookings = "बुकिंग",

    homeGreetingMorning = "सुप्रभात",
    homeGreetingAfternoon = "नमस्ते",
    homeGreetingEvening = "शुभ संध्या",
    homeSubtitle = "आज आपको कौन सी सेवा चाहिए?",
    homePopularServices = "लोकप्रिय सेवाएं",
    homeTrendingNearYou = "आपके नज़दीक ट्रेंडिंग",
    homeTopRated = "शीर्ष रेटेड प्रदाता",
    homeSearchPlaceholder = "सेवा खोजें...",
    homeSearchHint1 = "'प्लंबर' खोजें",
    homeSearchHint2 = "'मेड' खोजें",

    bookingsTitle = "मेरी बुकिंग",
    bookingsActiveTab = "सक्रिय बुकिंग",
    bookingsHistoryTab = "इतिहास",
    bookingsEmpty = "अभी तक कोई बुकिंग नहीं",
    bookingsEmptySub = "शुरू करने के लिए कोई सेवा खोजें और बुक करें",
    bookingsExploreNow = "अभी एक्सप्लोर करें",
    bookingsActiveRequests = "सक्रिय अनुरोध",
    bookingsActiveBookings = "सक्रिय बुकिंग",

    profileTitle = "खाता",
    profileEditProfile = "प्रोफ़ाइल संपादित करें",
    profileSaveChanges = "बदलाव सहेजें",
    profileCancel = "रद्द करें",
    profileName = "पूरा नाम",
    profilePhone = "फ़ोन नंबर",
    profileAddress = "पता",
    profileLanguage = "भाषा",
    profileChooseLanguage = "भाषा चुनें",
    profileLogout = "लॉग आउट",
    profileLogoutConfirm = "क्या आप वाकई लॉग आउट करना चाहते हैं?",
    profileMyBookings = "मेरी बुकिंग",
    profilePaymentModes = "भुगतान विधियां",
    profileReferEarn = "रेफर करें और कमाएं",
    profileHelpSupport = "सहायता",
    profileNestoraMoney = "नेस्टोरा मनी",
    profileAdminPayments = "एडमिन भुगतान",
    profileAdminMedia = "एडमिन मीडिया",
    profileSwitchToProvider = "प्रदाता मोड में बदलें",
    profileSwitchToCustomer = "ग्राहक मोड में बदलें",
    profileProviderMode = "प्रदाता मोड",
    profileCustomerMode = "ग्राहक मोड",

    moneyTitle = "नेस्टोरा मनी",
    moneyAvailableBalance = "उपलब्ध बैलेंस",
    moneyAddBalance = "बैलेंस जोड़ें",
    moneyTransactionHistory = "लेनदेन इतिहास",
    moneyHowItWorks = "यह कैसे काम करता है",
    moneySecurePayments = "सुरक्षित भुगतान",
    moneyInstantCredit = "तुरंत क्रेडिट",
    moneyUsedAt = "किसी भी नेस्टोरा सेवा के भुगतान में उपयोग करें",
    moneyProceedToAdd = "बैलेंस जोड़ने के लिए आगे बढ़ें",
    moneyAmount = "राशि",
    moneyEnterAmount = "राशि दर्ज करें",
    moneySelectAmount = "राशि चुनें",
    moneyNoteTitle = "कृपया ध्यान दें",
    moneyNote1 = "यह राशि आपके नेस्टोरा वॉलेट में जोड़ी जाएगी",
    moneyNote2 = "वॉलेट बैलेंस नेस्टोरा पर किसी भी सेवा के भुगतान के लिए उपयोग किया जा सकता है",
    moneyNote3 = "रिफंड, यदि लागू हो, इस वॉलेट में वापस जमा किया जाएगा",
    moneyNote4 = "वॉलेट बैलेंस हस्तांतरणीय नहीं है और बैंक खाते में वापस नहीं किया जा सकता",
    moneyLoading = "बैलेंस लोड हो रहा है...",

    bookingSchedule = "शेड्यूल",
    bookingFlexible = "लचीला",
    bookingSelectDate = "तारीख चुनें",
    bookingSelectTime = "समय चुनें",
    bookingServiceLocation = "सेवा का स्थान",
    bookingChangeLocation = "स्थान बदलें",
    bookingSendRequest = "अनुरोध भेजें",
    bookingServiceQuestions = "सेवा संबंधी प्रश्न",
    bookingSafetyInfo = "सुरक्षा जानकारी",
    bookingStatus = "बुकिंग स्थिति",
    bookingStatusPending = "लंबित",
    bookingStatusConfirmed = "पुष्टि की गई",
    bookingStatusEnRoute = "प्रदाता रास्ते में है",
    bookingStatusCompleted = "पूर्ण",
    bookingStatusCancelled = "रद्द",

    finderTitle = "सेवाएं खोजें",
    finderSearchPlaceholder = "प्लंबर, मेड, इलेक्ट्रीशियन खोजें...",
    finderNearby = "नज़दीक",
    finderNoResults = "कोई परिणाम नहीं मिला",
    finderTryPlumber = "'प्लंबर' खोजें",
    finderTryMaid = "'मेड' खोजें",

    locationTitle = "स्थान अनुमति दें",
    locationSubtitle = "आपके नज़दीक सेवाएं दिखाने के लिए हमें आपके स्थान की आवश्यकता है",
    locationAllow = "स्थान अनुमति दें",
    locationSkip = "अभी छोड़ें",
    notificationTitle = "अपडेट रहें",
    notificationSubtitle = "बुकिंग अपडेट प्राप्त करने के लिए सूचनाएं अनुमति दें",
    notificationAllow = "सूचनाएं अनुमति दें",
    notificationSkip = "अभी छोड़ें",

    commonLoading = "लोड हो रहा है...",
    commonError = "कुछ गलत हो गया",
    commonRetry = "पुनः प्रयास करें",
    commonDone = "हो गया",
    commonNext = "अगला",
    commonBack = "वापस",
    commonClose = "बंद करें",
    commonSearch = "खोजें",
    commonSave = "सहेजें",
    commonEdit = "संपादित करें",
    commonDelete = "हटाएं",
    commonConfirm = "पुष्टि करें",
    commonBookNow = "अभी बुक करें",
    commonViewDetails = "विवरण देखें",
    commonViewAll = "सभी देखें",
    commonRating = "रेटिंग",
    commonReviews = "समीक्षाएं",
    commonMinutes = "मिनट",
    commonHours = "घंटे",
    commonFrom = "से",
    commonPerHour = "प्रति घंटा",
    commonOff = "छूट",

    langEnglish = "English",
    langHindi = "हिन्दी",
    langBengali = "বাংলা",
)

// ─────────────────────────────────────────────────────────────────────────────
// Bengali Strings (বাংলা)
// ─────────────────────────────────────────────────────────────────────────────

val BengaliStrings = NestoraStrings(
    navExplore = "অন্বেষণ",
    navFinder = "খুঁজুন",
    navAccount = "অ্যাকাউন্ট",
    navDashboard = "ড্যাশবোর্ড",
    navRegister = "নিবন্ধন",
    navListings = "তালিকা",
    navBookings = "বুকিং",

    homeGreetingMorning = "শুভ সকাল",
    homeGreetingAfternoon = "শুভ অপরাহ্ন",
    homeGreetingEvening = "শুভ সন্ধ্যা",
    homeSubtitle = "আজ আপনার কোন সেবা দরকার?",
    homePopularServices = "জনপ্রিয় সেবাসমূহ",
    homeTrendingNearYou = "আপনার কাছে ট্রেন্ডিং",
    homeTopRated = "শীর্ষ রেটেড প্রদানকারী",
    homeSearchPlaceholder = "সেবা খুঁজুন...",
    homeSearchHint1 = "'প্লাম্বার' খুঁজুন",
    homeSearchHint2 = "'মেড' খুঁজুন",

    bookingsTitle = "আমার বুকিং",
    bookingsActiveTab = "সক্রিয় বুকিং",
    bookingsHistoryTab = "ইতিহাস",
    bookingsEmpty = "এখনও কোনো বুকিং নেই",
    bookingsEmptySub = "শুরু করতে একটি সেবা খুঁজুন এবং বুক করুন",
    bookingsExploreNow = "এখনই অন্বেষণ করুন",
    bookingsActiveRequests = "সক্রিয় অনুরোধ",
    bookingsActiveBookings = "সক্রিয় বুকিং",

    profileTitle = "অ্যাকাউন্ট",
    profileEditProfile = "প্রোফাইল সম্পাদনা",
    profileSaveChanges = "পরিবর্তন সংরক্ষণ করুন",
    profileCancel = "বাতিল করুন",
    profileName = "পুরো নাম",
    profilePhone = "ফোন নম্বর",
    profileAddress = "ঠিকানা",
    profileLanguage = "ভাষা",
    profileChooseLanguage = "ভাষা নির্বাচন করুন",
    profileLogout = "লগ আউট",
    profileLogoutConfirm = "আপনি কি সত্যিই লগ আউট করতে চান?",
    profileMyBookings = "আমার বুকিং",
    profilePaymentModes = "পেমেন্ট পদ্ধতি",
    profileReferEarn = "রেফার করুন ও উপার্জন করুন",
    profileHelpSupport = "সাহায্য ও সহায়তা",
    profileNestoraMoney = "নেস্টোরা মানি",
    profileAdminPayments = "অ্যাডমিন পেমেন্ট",
    profileAdminMedia = "অ্যাডমিন মিডিয়া",
    profileSwitchToProvider = "প্রদানকারী মোডে যান",
    profileSwitchToCustomer = "গ্রাহক মোডে যান",
    profileProviderMode = "প্রদানকারী মোড",
    profileCustomerMode = "গ্রাহক মোড",

    moneyTitle = "নেস্টোরা মানি",
    moneyAvailableBalance = "উপলব্ধ ব্যালেন্স",
    moneyAddBalance = "ব্যালেন্স যোগ করুন",
    moneyTransactionHistory = "লেনদেনের ইতিহাস",
    moneyHowItWorks = "এটি কীভাবে কাজ করে",
    moneySecurePayments = "নিরাপদ পেমেন্ট",
    moneyInstantCredit = "তাৎক্ষণিক ক্রেডিট",
    moneyUsedAt = "যেকোনো নেস্টোরা সেবায় ব্যবহার করুন",
    moneyProceedToAdd = "ব্যালেন্স যোগ করতে এগিয়ে যান",
    moneyAmount = "পরিমাণ",
    moneyEnterAmount = "পরিমাণ লিখুন",
    moneySelectAmount = "পরিমাণ নির্বাচন করুন",
    moneyNoteTitle = "দয়া করে মনে রাখুন",
    moneyNote1 = "এই পরিমাণ আপনার নেস্টোরা ওয়ালেটে যোগ হবে",
    moneyNote2 = "ওয়ালেট ব্যালেন্স নেস্টোরায় যেকোনো সেবার পেমেন্টে ব্যবহার করা যাবে",
    moneyNote3 = "রিফান্ড, প্রযোজ্য হলে, এই ওয়ালেটে ফেরত দেওয়া হবে",
    moneyNote4 = "ওয়ালেট ব্যালেন্স হস্তান্তরযোগ্য নয় এবং ব্যাংক অ্যাকাউন্টে ফেরতযোগ্য নয়",
    moneyLoading = "ব্যালেন্স লোড হচ্ছে...",

    bookingSchedule = "সময়সূচী",
    bookingFlexible = "নমনীয়",
    bookingSelectDate = "তারিখ নির্বাচন করুন",
    bookingSelectTime = "সময় নির্বাচন করুন",
    bookingServiceLocation = "সেবার অবস্থান",
    bookingChangeLocation = "অবস্থান পরিবর্তন করুন",
    bookingSendRequest = "অনুরোধ পাঠান",
    bookingServiceQuestions = "সেবা সংক্রান্ত প্রশ্ন",
    bookingSafetyInfo = "নিরাপত্তা তথ্য",
    bookingStatus = "বুকিং স্ট্যাটাস",
    bookingStatusPending = "অপেক্ষারত",
    bookingStatusConfirmed = "নিশ্চিত",
    bookingStatusEnRoute = "প্রদানকারী পথে আছেন",
    bookingStatusCompleted = "সম্পন্ন",
    bookingStatusCancelled = "বাতিল",

    finderTitle = "সেবা খুঁজুন",
    finderSearchPlaceholder = "প্লাম্বার, মেড, ইলেকট্রিশিয়ান খুঁজুন...",
    finderNearby = "কাছাকাছি",
    finderNoResults = "কোনো ফলাফল পাওয়া যায়নি",
    finderTryPlumber = "'প্লাম্বার' খুঁজুন",
    finderTryMaid = "'মেড' খুঁজুন",

    locationTitle = "অবস্থান অ্যাক্সেস অনুমতি দিন",
    locationSubtitle = "আপনার কাছের সেবাগুলি দেখাতে আমাদের আপনার অবস্থান প্রয়োজন",
    locationAllow = "অবস্থান অনুমতি দিন",
    locationSkip = "এখন এড়িয়ে যান",
    notificationTitle = "আপডেটেড থাকুন",
    notificationSubtitle = "বুকিং আপডেট পেতে বিজ্ঞপ্তির অনুমতি দিন",
    notificationAllow = "বিজ্ঞপ্তি অনুমতি দিন",
    notificationSkip = "এখন এড়িয়ে যান",

    commonLoading = "লোড হচ্ছে...",
    commonError = "কিছু একটা ভুল হয়েছে",
    commonRetry = "আবার চেষ্টা করুন",
    commonDone = "হয়ে গেছে",
    commonNext = "পরবর্তী",
    commonBack = "পিছনে",
    commonClose = "বন্ধ করুন",
    commonSearch = "খুঁজুন",
    commonSave = "সংরক্ষণ করুন",
    commonEdit = "সম্পাদনা",
    commonDelete = "মুছুন",
    commonConfirm = "নিশ্চিত করুন",
    commonBookNow = "এখনই বুক করুন",
    commonViewDetails = "বিবরণ দেখুন",
    commonViewAll = "সব দেখুন",
    commonRating = "রেটিং",
    commonReviews = "রিভিউ",
    commonMinutes = "মিনিট",
    commonHours = "ঘণ্টা",
    commonFrom = "থেকে",
    commonPerHour = "প্রতি ঘণ্টা",
    commonOff = "ছাড়",

    langEnglish = "English",
    langHindi = "हिन्दी",
    langBengali = "বাংলা",
)

// ─────────────────────────────────────────────────────────────────────────────
// Language Registry
// ─────────────────────────────────────────────────────────────────────────────

enum class NestoraLanguage(val code: String, val displayName: String, val nativeName: String) {
    English("en", "English", "English"),
    Hindi("hi", "Hindi", "हिन्दी"),
    Bengali("bn", "Bengali", "বাংলা");

    companion object {
        fun fromCode(code: String): NestoraLanguage =
            entries.firstOrNull { it.code == code } ?: English
    }
}

fun stringsForLanguage(language: NestoraLanguage): NestoraStrings = when (language) {
    NestoraLanguage.Hindi -> HindiStrings
    NestoraLanguage.Bengali -> BengaliStrings
    else -> EnglishStrings
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Access the current locale's strings anywhere in the composition:
 *   val str = LocalNestoraStrings.current
 *   Text(str.navExplore)
 */
val LocalNestoraStrings = compositionLocalOf<NestoraStrings> { EnglishStrings }
