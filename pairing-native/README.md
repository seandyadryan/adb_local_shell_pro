# Source-included Android SPAKE2 library

This module contains the native Android/JNI implementation from
[MuntashirAkon/spake2-java](https://github.com/MuntashirAkon/spake2-java), tag **2.2.1**,
and its pinned [spake2-c](https://github.com/MuntashirAkon/spake2-c) submodule.
Both exact upstream revisions are recorded in `UPSTREAM.txt`, in that order.

Copyright 2021 Muntashir Al-Islam and upstream contributors. The library is
LGPL-3.0; original per-file notices are retained. `COPYING` and `COPYING.LESSER`
contain the complete GPL and LGPL license texts. The C source retains the
applicable BoringSSL/Google notices in its headers. Full corresponding source,
including the C arithmetic, SHA-512, JNI wrapper, Java API and build scripts,
is included here. No downloaded native binary is used by the build.

Local changes: Android minimum/compile SDK configuration, consumer rules to
preserve JNI names, and a small replacement CMake file that builds with NDK
28.2 and explicitly links with 16 KB maximum page alignment. The JNI error paths
leave context cleanup to Java, Java release is idempotent, and JNI registration
errors are checked. These lifecycle changes prevent double-free on malformed
peer messages. Algorithm and wire-protocol code are not rewritten. The unused upstream Java arithmetic
implementation is not included.

To rebuild/relink: install Android SDK 36, NDK 28.2.13676358 and CMake 3.22.1,
edit this module and run `./gradlew :app:assembleDebug`. Sign with your own debug
key, uninstall any differently signed copy, then install your rebuilt APK.
Export needed app data before uninstalling. No developer signing key or Play
account is required. The root application license expressly permits these
library modifications, debugging and relinking for your own use.

The app replaces Kadb's transitive GPL SPAKE2 provider with this LGPL module.
Its own bounded pairing adapter calls this module directly. Release optimization
removes Kadb's unused pairing adapter. `:app:dependencies` verifies that the
excluded provider is absent from the runtime dependency graph.
