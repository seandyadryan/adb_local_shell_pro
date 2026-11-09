# Release verification

ADB Local Shell PRO 1.0.1 (version code 2) uses `com.deploydulupulangnanti.adblocalshellpro`. The minimum SDK is Android 11 / API 30, and the target is Android 16 / API 36.

## Android checks

| Check | Result |
| --- | --- |
| Kotlin unit tests | Ten passed. They cover input limits, command review, UTF-8 boundaries, malformed ADB frames, and the shell output budget. |
| Upgrade with an existing pairing | A same-signer update of the installed 1.0.0 app connected with the previously authorized key. The stored PEM was unchanged after the test. |
| Live Android shell | The secure Android 16 emulator paired with the system Wireless debugging service. The app ran `id` as the Android `shell` user with its ordinary app permissions. |
| Stream behavior | Live checks covered Unicode, exit status 7, twelve short commands in succession, EOF on stdin, shell quoting, stopping a command, and running another command without re-pairing. |

The emulator fixture uses shell permissions to prepare Android's pairing service. It drops those permissions before the application client pairs, connects and runs a command. The fixture runs only in the instrumentation APK.

The runtime checks use an x86_64 Android 16 emulator. Physical-device pairing, manufacturer Settings screens, older Android versions, and ARM runtime behavior remain part of the device testing for Play publication.

## Signed package checks

The release signing tasks reject either unsupported SDK in the runtime dependency graph. The GitHub workflow also inspects every DEX file in the APK and AAB for the removed SDKs and the former Android hidden API calls. The DEX check detected the bypass SDK in version 1.0.0 and passed on the 1.0.1 development APK.

`tools/verify_release.py` checks release signatures, package and version metadata, DEX contents, Bundletool validation, and 16 KB alignment in the APK, AAB, and native libraries. The release folder includes SHA-256 checksums and the detailed verification output.

## Store media checks

The listing has 78 characters in the short description and 2,725 in the full description. Validation checks the 512 × 512 RGBA icon, the 1,024 × 500 RGB feature graphic, five phone images, and four images each for 7-inch tablets, 10-inch tablets, and wide Android windows. Every upload image meets its size and format limits. The per-file dimensions and byte counts are in `store/manifest.json`.

The 30-second video decodes successfully as 1,920 × 1,080 H.264 at 24 frames per second. The five chapters use English captions and live app screenshots. The video is silent.

The listing uses the complete, verified captures from the Android 16 QA build. The signed APK adds stronger status bar contrast and uses the final charcoal and lime controls.
