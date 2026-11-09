# Release verification

ADB Local Shell PRO 1.0.0 (version code 1) uses `com.deploydulupulangnanti.adblocalshellpro`. The minimum SDK is Android 11 / API 30, and the target is Android 16 / API 36.

## Android checks

| Check | Result |
| --- | --- |
| Kotlin unit tests | Seven passed, including input limits, command review, UTF-8 packet boundaries and byte-safe truncation. |
| Native SPAKE2 tests | Three passed, including valid exchanges, mismatched passwords and malformed peer data. |
| Compose UI and local storage | Four passed. Tested navigation, loading a saved command without executing it, the landscape composer, and bounded history persistence and clearing. |
| Live Android shell | The secure Android 16 emulator paired with the system Wireless debugging service. The app ran `id` as the Android `shell` user with its ordinary app permissions. |
| Stream behavior | Live checks covered Unicode, exit status 7, twelve short commands in succession, EOF on stdin, shell quoting, stopping a command, and running another command on the same connection. |
| Exact signed release | The APK installed and opened on Android 16. A framework instrumentation probe paired the signed app with the Android system service over TLS. It verified `uid=2000`, Unicode, exit status 7, stdin EOF and twelve fast commands using the optimized production client. |

The emulator fixture uses shell permissions to prepare Android's pairing service. It drops those permissions before the application client pairs, connects and runs a command. The fixture runs only in the instrumentation APK.

The runtime checks use an x86_64 Android 16 emulator. Physical-device pairing, manufacturer Settings screens, older Android versions, and ARM runtime behavior remain part of the device testing for Play publication.

## Signed package checks

The release APK and AAB passed signature verification. Bundletool validated the AAB and confirmed `PAGE_ALIGNMENT_16K`. `zipalign` validated the APK at 16 KB, and ELF inspection checked all eight native libraries across the four packaged ABIs. The APK is non-debuggable. Local release lint reported zero errors and 16 warnings.

The release folder includes SHA-256 checksums and the detailed verification output.

## Store media checks

The listing has 78 characters in the short description and 2,725 in the full description. Validation checks the 512 × 512 RGBA icon, the 1,024 × 500 RGB feature graphic, five phone images, and four images each for 7-inch tablets, 10-inch tablets, and wide Android windows. Every upload image meets its size and format limits. The per-file dimensions and byte counts are in `store/manifest.json`.

The 30-second video decodes successfully as 1,920 × 1,080 H.264 at 24 frames per second. The five chapters use English captions and live app screenshots. The video is silent.

The listing uses the complete, verified captures from the Android 16 QA build. The signed APK adds stronger status bar contrast and uses the final charcoal and lime controls.
