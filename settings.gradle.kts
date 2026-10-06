// ============================================================
//  工程级设置：声明插件仓库、依赖仓库与包含的模块
// ============================================================
//  为什么要加阿里云镜像：
//  Compose 的全部依赖都来自 Google Maven / Maven Central，
//  国内直连经常超时或极慢（首次同步要下载 300MB+）。
//  镜像放在前面优先命中，官方仓库留作兜底，保证能拉到即可。
// ============================================================

pluginManagement {
    repositories {
        // ① 阿里云镜像（国内加速，优先）
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

        // ② 官方仓库（兜底）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 禁止各模块自己声明仓库，统一在这里管理，避免版本来源混乱
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "ShellConsole"

// 唯一的应用模块
include(":app")
