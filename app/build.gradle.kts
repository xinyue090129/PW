// ============================================================
//  应用模块构建脚本
// ============================================================
//  与旧 Java 工程的关键差异（务必知悉）：
//    ① minSdk 由 19 提升到 26：Compose 最低要求 21，取 26 与 KernelSU Manager 一致；
//    ② 引入 Compose / Material3 / lifecycle，不再是"零第三方依赖"；
//    ③ 出包只用 Gradle（assembleDebug），不再走 一键打包.bat 那套 aapt2 手工流水线。
// ============================================================

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.0 起必须显式应用 Compose 编译器插件，否则 @Composable 无法编译
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // 命名空间：决定 R 类与 BuildConfig 的包名（Manifest 里不再写 package 属性）
    namespace = "com.example.shellconsole"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shellconsole"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // JDK 17：AGP 8.x 要求，不要用 JDK 24（会直接构建失败）
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ---------- Compose（版本由 BOM 统一管理，下面各条目都不写版本号）----------
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material3 稳定版：本方案用它复刻 KernelSU 的观感（不用实验性的 MaterialExpressiveTheme）
    implementation("androidx.compose.material3:material3")
    // 图标只取 core 集（Settings / Clear / KeyboardArrowUp / PlayArrow / Refresh 等），
    // 不引 material-icons-extended —— 那个包体积很大，本项目用不着
    implementation("androidx.compose.material:material-icons-core")

    // ---------- 基础组件 ----------
    implementation("androidx.core:core-ktx:1.13.1")
    // ComponentActivity + enableEdgeToEdge() + rememberLauncherForActivityResult（文件选择回传）
    implementation("androidx.activity:activity-compose:1.9.3")
    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // 协程（StateFlow / viewModelScope）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ---------- 仅调试期使用 ----------
    debugImplementation("androidx.compose.ui:ui-tooling")
}
