// ============================================================
//  根构建脚本：只声明插件版本，不在此处配置具体模块
// ============================================================
//  版本组合说明（已避开 JDK 24 的坑）：
//    Gradle 8.9  +  AGP 8.7.2  +  Kotlin 2.0.21
//  这三个版本互相兼容，且都要求 JDK 17（不要用 JDK 24，AGP 会直接报错）。
//
//  Kotlin 2.0 起，Compose 编译器由独立的 Gradle 插件提供，
//  所以必须同时声明 org.jetbrains.kotlin.plugin.compose，
//  否则 @Composable 无法编译（这是 Kotlin 2.0 与 1.x 最大的差异）。
// ============================================================

plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
