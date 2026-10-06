package com.example.shellconsole.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

/**
 * ============================================================
 *  ShellConsole 主题
 * ============================================================
 *  复刻 KernelSU Manager 主题层的三个核心行为：
 *    ① 动态取色：keyColor == 0 且系统为 Android 12+ 时，从壁纸提取配色；
 *    ② 15 色种子：keyColor 为某个具体色值时，用它派生主色系；
 *    ③ 三档明暗：跟随系统 / 强制深色 / AMOLED 纯黑。
 *
 *  【与 KernelSU 的差异（有意为之）】
 *  KernelSU 用 MaterialExpressiveTheme + MotionScheme.expressive()（Material3 较新 API）
 *  和 material-kolor 库做完整配色派生。本方案刻意不引入二者：
 *    - MaterialExpressiveTheme 属于较新/实验 API，会带来依赖抖动；
 *    - material-kolor 是额外的第三方依赖。
 *  这里改用稳定版 MaterialTheme，并以"纯 Compose 实现"近似派生主色系
 *  （只覆盖 primary/secondary/tertiary 家族，中性色沿用 Material3 基线）。
 * ============================================================
 *
 * @param colorMode 明暗模式（跟随系统 / 深色 / AMOLED）
 * @param keyColor  主色：0 表示动态取色，否则取 [keyColorOptions] 中的某个 0xAARRGGBB
 * @param content   主题作用域内的界面内容
 */
@Composable
fun ShellConsoleTheme(
    colorMode: ColorMode,
    keyColor: Int,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // 是否走深色：AMOLED 天然是深色；DARK 强制深色；SYSTEM 交给系统判断
    val darkTheme = when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.DARK, ColorMode.AMOLED -> true
    }
    val amoled = colorMode == ColorMode.AMOLED

    val colorScheme = buildColorScheme(
        context = context,
        dynamicColor = keyColor == 0,
        keyColor = keyColor,
        dark = darkTheme,
        amoled = amoled,
    )

    // ---------- 状态栏 / 导航栏图标明暗切换 ----------
    // 深色底 → 状态栏图标用浅色；浅色底 → 用深色。不处理的话浅色主题下图标会"消失"。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShellTypography,
        content = content,
    )
}

/**
 * 组装最终配色方案。
 *
 * 分支顺序：
 *  1) 动态取色 + Android 12(API 31) 及以上 → 交给系统的 Monet 取色
 *  2) 其余情况（含低版本想用动态取色）→ 用种子色自行派生
 *  3) 若为 AMOLED → 在结果之上把背景类颜色压成纯黑
 */
private fun buildColorScheme(
    context: Context,
    dynamicColor: Boolean,
    keyColor: Int,
    dark: Boolean,
    amoled: Boolean,
): ColorScheme {
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // 动态取色在 Android 12 以下不可用，回落到默认蓝作为种子
        val seed = if (keyColor == 0) Color(0xFF2196F3) else Color(keyColor)
        seededScheme(seed, dark)
    }
    return if (amoled) base.toAmoled() else base
}

/**
 * 用一个种子色派生主色系。
 *
 * 说明：这里做的是轻量近似，不是完整的 HCT 配色量化（那需要材料色彩工具库）。
 * 做法是把种子色作为 primary，并按明暗把它与黑/白插值出容器色，
 * 好处是零依赖、可预测，观感上与 KernelSU 的"选中某主色后整体色调随之变化"一致。
 */
private fun seededScheme(seed: Color, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()

    // 种子色偏亮时用黑字，偏暗时用白字，保证 primary 上的文字可读
    val onSeed = if (seed.luminance() > 0.5f) PureBlack else PureWhite

    // 容器色：深色模式往黑里压，浅色模式往白里提
    val container = if (dark) lerp(seed, PureBlack, 0.55f) else lerp(seed, PureWhite, 0.80f)
    val onContainer = if (dark) lerp(seed, PureWhite, 0.72f) else lerp(seed, PureBlack, 0.62f)

    return base.copy(
        primary = seed,
        onPrimary = onSeed,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        // 次要色让种子色与 M3 基线轻微混合，避免整屏只有一种色
        secondary = lerp(seed, base.secondary, if (dark) 0.70f else 0.55f),
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        tertiary = lerp(seed, base.tertiary, 0.40f),
    )
}

/**
 * AMOLED 化：把背景与各级 surface 全部压成纯黑。
 *
 * 注意 surfaceVariant 不能也压成纯黑 —— M3 里不少组件（输入框容器、卡片描边区）
 * 依赖它比 surface 稍亮来区分层次，全黑会让界面"糊"成一片，所以给一个近黑色。
 */
private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = PureBlack,
    surface = PureBlack,
    surfaceDim = PureBlack,
    surfaceBright = PureBlack,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = PureBlack,
    surfaceContainer = PureBlack,
    surfaceContainerHigh = PureBlack,
    surfaceContainerHighest = PureBlack,
    surfaceVariant = AmoledSurfaceVariant,
    outline = AmoledOutline,
    outlineVariant = AmoledOutline,
)
