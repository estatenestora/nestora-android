# Network payloads are decoded by Gson from field names at runtime.
-keep class com.estatenestora.app.data.model.** { *; }

# TDLib's generated Java API is called from the native bridge. Keep its public
# surface stable while R8 shrinks the rest of the application and dependencies.
-keep class org.drinkless.tdlib.** { *; }
