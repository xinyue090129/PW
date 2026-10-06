package com.example.shellconsole.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.example.shellconsole.ui.theme.ColorMode

/**
 * ============================================================
 *  外观相关设置的状态与回调
 * ============================================================
 *  对标 KernelSU Manager 的 `SettingsUiState` + `SettingsScreenActions`：
 *  把"状态"和"动作"分成两个不可变对象往下传，而不是给每层 Composable 塞一大串参数。
 *  MainActivity 持有唯一状态源（remember + SharedPreferences 持久化），
 *  设置页与主题配色页只负责读写。
 *
 *  【这些是真正生效的】
 *    colorMode                —— 明暗模式
 *    keyColor                 —— 主色（0 = 动态取色）
 *    pageScale                —— 页面缩放（通过 LocalDensity 换算）
 *    floatingBottomBar        —— 悬浮底栏（圆角 + 左右留白）
 *    glassBottomBar           —— 底栏毛玻璃（常驻底栏）
 *    glassFloatingBottomBar   —— 悬浮底栏毛玻璃（仅悬浮底栏开启时有效）
 *  其余条目（手势模式、导航角标、色板样式…）没有可落地的实现，仍在页面上置灰。
 * ============================================================
 */
@Immutable
data class AppearanceState(
    /** 明暗模式：跟随系统 / 深色 / AMOLED 纯黑 */
    val colorMode: ColorMode = ColorMode.SYSTEM,

    /** 主色：0 表示动态取色（从壁纸提取），否则是某个 0xAARRGGBB */
    val keyColor: Int = 0,

    /** 页面缩放倍率，1.0 = 标准 */
    val pageScale: Float = 1.0f,

    /** 悬浮底栏：底栏脱离屏幕边缘、加圆角 */
    val floatingBottomBar: Boolean = false,

    /** 底栏毛玻璃：常驻底栏背后的内容做模糊 */
    val glassBottomBar: Boolean = false,

    /** 悬浮底栏毛玻璃：只在「悬浮底栏」开启时生效（与 KernelSU 的两个开关一一对应） */
    val glassFloatingBottomBar: Boolean = false,
)

@Immutable
data class AppearanceActions(
    val onColorModeChange: (ColorMode) -> Unit = {},
    val onKeyColorChange: (Int) -> Unit = {},
    val onPageScaleChange: (Float) -> Unit = {},
    val onFloatingBottomBarChange: (Boolean) -> Unit = {},
    val onGlassBottomBarChange: (Boolean) -> Unit = {},
    val onGlassFloatingBottomBarChange: (Boolean) -> Unit = {},
)

/**
 * 当前底栏是否需要做毛玻璃。
 * 常驻与悬浮两种形态各有一个开关（与 KernelSU 一致），按当前形态取用。
 */
val AppearanceState.bottomBarBlurEnabled: Boolean
    get() = if (floatingBottomBar) glassFloatingBottomBar else glassBottomBar

/** 页面缩放的档位与展示文案；设置页与取值逻辑共用一份，避免两处写死 */
val PageScaleOptions: List<Pair<Float, String>> = listOf(
    0.8f to "80%",
    0.9f to "90%",
    1.0f to "100%",
    1.1f to "110%",
    1.2f to "120%",
)

/** 把当前倍率换算成下拉框下标；取不到就回到 100% */
fun pageScaleIndex(scale: Float): Int {
    val index = PageScaleOptions.indexOfFirst { kotlin.math.abs(it.first - scale) < 0.001f }
    return if (index >= 0) index else 2
}
