plugins { id("com.android.library") }
android {
    namespace = "io.github.muntashirakon.crypto.spake2"
    compileSdk = 36
    ndkVersion = "28.2.13676358"
    defaultConfig { minSdk = 30 }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes { release { consumerProguardFiles("consumer-rules.pro") } }
}
dependencies { implementation("androidx.annotation:annotation:1.9.1") }
