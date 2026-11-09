# Third-party notices

Original app code and store artwork © 2026 PT Deploy Dulu Pulang Nanti, all rights reserved. Third-party components are not relicensed by this notice.

| Component | Use | License |
| --- | --- | --- |
| Kotlin / kotlinx.coroutines | Language and concurrency | Apache 2.0 |
| AndroidX / Jetpack Compose / Material icons | Native UI and lifecycle | Apache 2.0 |
| Kadb 2.1.1 by Flyfish Xu / Flyfish Studio | Direct ADB connection and shell protocol | Apache 2.0 |
| spake2-java Android 2.2.1 + spake2-c, Muntashir Al-Islam | Native Wireless pairing SPAKE2; complete source in `pairing-native/` | LGPL-3.0, with retained per-file BoringSSL notices |
| Bouncy Castle 1.83 | Certificates and pairing HKDF | Bouncy Castle license (MIT style) |
| Okio | Buffered ADB I/O | Apache 2.0 |
| HiddenApiBypass 6.1, LSPosed | Platform Conscrypt TLS exporter access required for Android pairing | Apache 2.0 |
| Space Grotesk, Florian Karsten / Space Grotesk Project Authors | Headline and store typography | SIL Open Font License 1.1 |

Full license texts are in `third-party-licenses/` and bundled in the app's offline licenses screen. `WirelessPairing.kt` adapts the SPAKE2/HKDF/AES-GCM sequence from Kadb's Apache-licensed pairing implementation (Copyright 2024 Flyfish-Xu) and AOSP's ADB pairing protocol; it adds bounded/cancellable sockets and uses this installation's own identity. No ADB executable is bundled. AndroidX can include native graphics support.

Upstream projects:

- https://github.com/flyfishxu/Kadb
- https://github.com/MuntashirAkon/spake2-java
- https://github.com/LSPosed/AndroidHiddenApiBypass
- https://www.bouncycastle.org/
- https://github.com/square/okio
- https://github.com/floriankarsten/space-grotesk

Store images and the silent promotional video are original compositions rendered from real emulator captures. No stock photos, device-brand logos or third-party audio are included. Device frames are original geometric illustrations.
