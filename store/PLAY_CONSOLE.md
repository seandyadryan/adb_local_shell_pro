# Play Console publication notes

App name: **ADB Local Shell PRO** (19 characters).  
Developer: **PT Deploy Dulu Pulang Nanti**  
Support: **seandyadryan2@gmail.com**  
Package: **com.deploydulupulangnanti.adblocalshellpro**  
Release: **1.0.0**, version code **1**, target API **36**, minimum API **30**.

1. Create the app with English as the default listing language, app category **Tools**, and paid distribution. Choose the price and countries in Play Console. Choose paid distribution before the first production release: Google limits switching a previously free app to paid.
2. Upload the signed AAB from the completed workflow or local release output. Enroll in Play App Signing; preserve the provided keystore as the upload key unless you choose a different key arrangement in Console.
3. Paste listing text from `listing/`. Upload `artwork/app-icon-512.png`, `artwork/feature-graphic-1024x500.png` and the device-specific artwork folders. Screenshot sets show the same native Android app on phone, tablet and wide Android windows, including emulator-generated diagnostic output.
4. Privacy policy URL after Pages deployment: https://seandyadryan.github.io/adb_local_shell_pro/. `docs/index.html` is the source. Check that the URL is public before entering it in Console.
5. Ads declaration: **No**. No account or login is required. For app access/reviewer instructions, explain that Wireless debugging needs Developer options, Wi-Fi, split screen for pairing and separate pairing/connection ports. Full setup instructions are available inside the Connect tab.
6. Data safety: this version has no automatic off-device data collection or sharing by the app or its SDKs. Local commands, key, preferences and optional history are processed/stored on-device. User-initiated export, clipboard and support email are disclosed in the privacy policy. Review the current Console questions and exceptions before submitting your declarations. Data accessible to commands can be sensitive; this is not an assertion that commands can never cause network access.
7. Complete the content-rating questionnaire and target-audience declarations honestly. This technical utility is not designed for children. Do not select a rating without completing the questionnaire.
8. Upload `video/adb-local-shell-pro-promo.mp4` to YouTube as public or unlisted. Disable monetization/ads and age restriction. Paste the resulting YouTube URL into the store listing. There is no YouTube URL until you upload the video.
9. Complete the testing track and device checks required for your developer account. Test manufacturer-specific Wireless debugging behavior on physical devices, Android 11/16 and at least one tablet before selling broadly. The included QA report records exactly what was tested locally.

The ADB client uses Kotlin/Java and Android's platform TLS. The pairing arithmetic is built from included C/JNI source with NDK 28.2 and explicit 16 KB alignment. AndroidX also supplies native graphics support. The release package's actual ELF alignment and ZIP alignment are checked in the QA report. The app does not request accessibility, device administrator, all-files access, root, or package inventory permissions.

Official references checked during creation:

- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [App icon and preview asset specifications](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Data safety definitions and local-processing exception](https://support.google.com/googleplay/android-developer/answer/10787469)
- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842)

These notes describe the supplied build; they are not a guarantee of Play approval. Update declarations whenever behavior or dependencies change.
