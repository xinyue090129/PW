package com.example.shellconsole.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ============================================================
 *  主题色与明暗模式定义（复刻 KernelSU Manager 的主题观感）
 * ============================================================
 *  色值来源：KernelSU 的 ui/theme/Colors.kt，15 个 Material 标准色，逐一照抄。
 *  为什么照抄色值而不是复制文件：该仓库是 GPL-3.0，整文件复制会带来传染性许可义务；
 *  色值本身属于通用设计常量，自行编码使用没有这个问题。
 * ============================================================
 */

/**
 * 明暗模式三档（对齐 KernelSU 的 colorMode）。
 *
 * - [SYSTEM]：跟随系统深色开关
 * - [DARK]  ：强制深色
 * - [AMOLED]：深色 + 背景纯黑（省电，OLED 屏观感更"沉"）
 */
enum class ColorMode {
    SYSTEM,
    DARK,
    AMOLED;

    /** 是否为深色（AMOLED 也属于深色） */
    val isDark: Boolean get() = this != SYSTEM
}

/**
 * 待机（种子）色选项：15 个 Material 标准色，与 KernelSU 完全一致。
 *
 * 约定同 KernelSU：设置值 **0 表示"动态取色"**（Android 12+ 从壁纸提取配色），
 * 非 0 时取本列表中的某个 0xAARRGGBB 值。
 */
val keyColorOptions: List<Int> = listOf(
    0xFFF44336.toInt(), // Red
    0xFFE91E63.toInt(), // Pink
    0xFF9C27B0.toInt(), // Purple
    0xFF673AB7.toInt(), // Deep Purple
    0xFF3F51B5.toInt(), // Indigo
    0xFF2196F3.toInt(), // Blue
    0xFF00BCD4.toInt(), // Cyan
    0xFF009688.toInt(), // Teal
    0xFF4FAF50.toInt(), // Green（沿用 KernelSU 原值）
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFFC107.toInt(), // Amber
    0xFFFF9800.toInt(), // Orange
    0xFF795548.toInt(), // Brown
    0xFF607D8F.toInt(), // Blue Grey（沿用 KernelSU 原值）
    0xFFFF9CA8.toInt(), // Soft Pink
)

/**
 * 与 [keyColorOptions] 一一对应的中文名，用于设置面板展示。
 */
val keyColorNames: List<String> = listOf(
    "红", "粉", "紫", "深紫", "靛蓝",
    "蓝", "青", "蓝绿", "绿", "黄",
    "琥珀", "橙", "棕", "蓝灰", "浅粉",
)

/** 开启状态栏 / 导航栏图标反色时使用的纯白与纯黑 */
internal val PureWhite = Color(0xFFFFFFFF)
internal val PureBlack = Color(0xFF000000)

/** AMOLED 模式下用于替代 surfaceVariant 的近黑色（纯黑会让卡片完全"糊"在一起） */
internal val AmoledSurfaceVariant = Color(0xFF1A1A1A)

/** AMOLED 模式下的描边色 */
internal val AmoledOutline = Color(0xFF3A3A3A)
