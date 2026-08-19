# TDLib integration — setup notes

What changed, code-wise, is done: `TdLibManager.kt`, `TdLibConfig.kt`, `TelegramAuthScreen.kt`,
`NestoraRepository.kt`, and `MainActivity.kt` are wired end-to-end. **Backend: zero changes** —
`router.go`'s `GetUpdatesChan()` and `messageHandler.go`'s `handleAndroidAppMessage` already treat
any incoming `AAPP::` message as an ordinary user message and reply to `update.Message.Chat.ID`,
which is exactly the private chat the app's Telegram user account now has with Dev1 Bot.

Three things I could not verify from this sandbox (no Android SDK, no network access to
jitpack.io or Telegram) — please check these before you trust it compiles:

## 1. Get real API credentials (2 minutes, free)
Go to https://my.telegram.org → "API development tools" → create an app → copy `api_id` and
`api_hash` into `TdLibConfig.kt`. Also set `BOT_USERNAME` to Dev1 Bot's `@username` (not its
token — TDLib resolves chats by username via `SearchPublicChat`).

## 2. The TDLib dependency needs a real binary — verify the JitPack coordinate
I wired `implementation("com.github.tdlibx:td:latest.release")` in `app/build.gradle.kts` because
it's a known community JitPack mirror of TDLib's Java bindings, but I have no network path to
jitpack.io from here to confirm it currently builds or that `latest.release` resolves to something
current. **Check https://jitpack.io/#tdlibx/td before your first build** — click "Get it", pick a
pinned version (not `latest.release`) once you see it resolves green.

If that mirror is stale or broken, fall back to the manual path, which is what TDLib's own docs
describe and is guaranteed to work:
1. Clone https://github.com/tdlib/td and follow `example/java/README.md` (Android section) to
   either build `libtdjson.so` yourself with the NDK, or grab prebuilt `.so` files per ABI from
   a maintained mirror such as https://github.com/up9cloud/android-libtdjson.
2. Copy the Java wrapper sources (`org/drinkless/tdlib/Client.java`, `TdApi.java`) from the
   `td` repo's `example/java` directory straight into
   `app/src/main/java/org/drinkless/tdlib/` (source, not a jar — that's the officially
   documented way to consume it).
3. Put the `.so` files under `app/src/main/jniLibs/<abi>/libtdjson.so` for each ABI you ship
   (`arm64-v8a` is enough for a real device; add `x86_64` for the emulator).
4. Remove the JitPack dependency line if you go this route.

Either way, the import in `TdLibManager.kt` — `org.drinkless.tdlib.Client` / `TdApi` — matches
TDLib's current (post-1.8.x) package name. If your binary predates that rename, its package is
`org.drinkless.td.libcore.telegram` instead; update the two imports at the top of
`TdLibManager.kt` accordingly.

## 3. First-run login is a real Telegram login
`TelegramAuthScreen` asks for a phone number, the SMS/app code, and — only if the account has
2FA — the cloud password. This happens once per install; TDLib persists the session under the
app's private storage (`filesDir/tdlib`). Use whichever Telegram account you want the app to act
as (a dedicated "service" account is cleaner than a personal one, since every request the app
sends will show up in that account's Dev1 Bot chat history).

## Sanity-check before shipping
- Send a query in the app, then check the Go backend's existing logs for
  `[AndroidBridge] requestID=... query=...` — if that line appears, the whole loop is proven end
  to end with zero backend changes.
- `router.go` keeps calling `GetUpdatesChan` exactly as before — you should see no 409s now, since
  the app never calls `getUpdates` on the bot token anymore.
