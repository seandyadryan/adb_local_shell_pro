-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
# Kadb's unused pairing adapter targets a different SPAKE2 provider.
# Our bounded pairing adapter uses the source-included LGPL provider instead.
-dontwarn com.flyfish233.crypto.spake2.**
# Preserve the core client API for release-signed integration tests. This does
# not add a runtime test interface, exported component or debug permission.
-keep class com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine { *; }
# Pinned Kadb no-retry OPEN adapter; never replay a potentially executed command.
-keepclassmembers class com.flyfishxu.kadb.Kadb {
    private kotlin.Pair openStream(java.lang.String, boolean);
}
