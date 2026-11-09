-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn org.bouncycastle.**
# Preserve the core client API for release-signed integration tests. This does
# not add a runtime test interface, exported component or debug permission.
-keep class com.deploydulupulangnanti.adblocalshellpro.adb.AdbEngine { *; }
