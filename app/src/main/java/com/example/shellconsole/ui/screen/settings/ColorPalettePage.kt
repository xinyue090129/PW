package com.example.shellconsole.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shellconsole.ui.PageContentWindowInsets
import com.example.shellconsole.ui.component.SegmentedColumn
import com.example.shellconsole.ui.component.SettingDropdownItem
import com.example.shellconsole.ui.component.SettingSwitchItem
import com.example.shellconsole.ui.component.UNSUPPORTED_SUMMARY
import com.example.shellconsole.ui.theme.ColorMode
import com.example.shellconsole.ui.theme.keyColorOptions

/**
 * ============================================================
 *  二级页：主题配色（对标 KernelSU Manager 的 ColorPaletteScreen）
 * ============================================================
 *  KernelSU 这一页的顺序是：
 *    ① 主题预览卡    ② 主色横向色盘（动态取色 + 15 色）
 *    ③ 明暗模式按钮组   ④ 色板样式 / 色规范   ⑤ 底栏 / 导航 / 页面 各项开关
 *
 *  【本页哪些是真生效的】
 *    主题预览卡      —— 用当前 MaterialTheme.colorScheme 实时渲染
 *    主色色盘        —— 16 个圆点（动态取色 + 15 色）
 *    明暗模式        —— 三档分段按钮
 *    悬浮底栏        —— 底栏脱离屏幕边缘、加圆角
 *    底栏毛玻璃      —— 底栏背后的内容做真实模糊（见 MainScreen 的图层实现）
 *    悬浮底栏毛玻璃  —— 仅「悬浮底栏」开启时可选
 *    页面缩放        —— LocalDensity 换算，整个界面一起缩放
 *  其余（色板样式、色规范、导航角标、手势模式、滑动返回、预测性返回）没有可落地的
 *  实现，按原样列出但置灰。
 * ============================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPalettePage(
    appearance: AppearanceState,
    actions: AppearanceActions,
    onBack: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = PageContentWindowInsets,
        topBar = {
            LargeTopAppBar(
                title = { Text("主题配色", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(top = 4.dp),
        ) {
            // ---------- ① 主题预览 ----------
            Column(Modifier.padding(horizontal = 16.dp)) {
                ThemePreviewCard(colorMode = appearance.colorMode)
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ② 主色色盘 ----------
            Column {
                Text(
                    text = "主色",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 第一个是"动态取色"：用当前主色画个圆点，表示"由壁纸决定"
                    item {
                        ColorDot(
                            argb = null,
                            label = "动态",
                            selected = appearance.keyColor == 0,
                            onClick = { actions.onKeyColorChange(0) },
                        )
                    }
                    items(keyColorOptions) { color ->
                        ColorDot(
                            argb = color,
                            label = null,
                            selected = appearance.keyColor == color,
                            onClick = { actions.onKeyColorChange(color) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ③ 明暗模式 ----------
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "明暗模式",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
                val modes = listOf(
                    ColorMode.SYSTEM to "跟随系统",
                    ColorMode.DARK to "深色",
                    ColorMode.AMOLED to "AMOLED",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = appearance.colorMode == mode,
                            onClick = { actions.onColorModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = modes.size,
                            ),
                            label = { Text(label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ④ 底栏 ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "底栏") {
                item {
                    SettingSwitchItem(
                        title = "悬浮底栏",
                        summary = "底栏脱离屏幕边缘，加圆角与左右留白",
                        icon = Icons.Filled.Clear,
                        checked = appearance.floatingBottomBar,
                        onCheckedChange = actions.onFloatingBottomBarChange,
                    )
                }
                item {
                    SettingSwitchItem(
                        title = "底栏毛玻璃",
                        summary = "把底栏背后的内容做高斯模糊垫在底栏下（Android 12 以下自动退化为纯色）",
                        icon = Icons.Filled.Clear,
                        checked = appearance.glassBottomBar,
                        onCheckedChange = actions.onGlassBottomBarChange,
                    )
                }
                item {
                    SettingSwitchItem(
                        title = "悬浮底栏毛玻璃",
                        summary = if (appearance.floatingBottomBar) {
                            "悬浮形态下使用毛玻璃"
                        } else {
                            "需要先开启「悬浮底栏」"
                        },
                        icon = Icons.Filled.Clear,
                        checked = appearance.glassFloatingBottomBar,
                        // 悬浮底栏没开时这条没有意义，直接禁用（KernelSU 同样只在悬浮形态下用得到）
                        enabled = appearance.floatingBottomBar,
                        onCheckedChange = actions.onGlassFloatingBottomBarChange,
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ⑤ 页面 ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "页面") {
                item {
                    SettingDropdownItem(
                        title = "页面缩放",
                        summary = "整个界面按倍率整体缩放",
                        items = PageScaleOptions.map { it.second },
                        selectedIndex = pageScaleIndex(appearance.pageScale),
                        icon = Icons.Filled.Settings,
                        onItemSelected = { index ->
                            PageScaleOptions.getOrNull(index)?.let {
                                actions.onPageScaleChange(it.first)
                            }
                        },
                    )
                }
                item {
                    SettingDropdownItem(
                        title = "手势模式",
                        summary = UNSUPPORTED_SUMMARY,
                        items = listOf("原生", "跨轴拦截", "类 iOS"),
                        selectedIndex = 1,
                        icon = Icons.Filled.Settings,
                        enabled = false,
                        onItemSelected = {},
                    )
                }
                item {
                    SettingSwitchItem(
                        title = "滑动返回",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.Clear,
                        checked = true,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
                item {
                    SettingSwitchItem(
                        title = "预测性返回",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.Clear,
                        checked = false,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ⑥ 色板（依赖 material-kolor 库） ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "色板") {
                item {
                    SettingDropdownItem(
                        title = "色板样式",
                        summary = "需要 material-kolor 库（本工程刻意不引第三方依赖）",
                        items = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive"),
                        selectedIndex = 0,
                        icon = Icons.Filled.Star,
                        enabled = false,
                        onItemSelected = {},
                    )
                }
                item {
                    SettingDropdownItem(
                        title = "色规范",
                        summary = "需要 material-kolor 库",
                        items = listOf("SPEC_2021", "SPEC_2025"),
                        selectedIndex = 1,
                        icon = Icons.Filled.Star,
                        enabled = false,
                        onItemSelected = {},
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- ⑦ 导航 ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "导航") {
                item {
                    SettingSwitchItem(
                        title = "导航角标",
                        summary = "本 App 暂无计数来源，角标恒为空",
                        icon = Icons.Filled.Warning,
                        checked = true,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
            }

            Spacer(Modifier.height(bottomInnerPadding + 16.dp))
        }
    }
}

/**
 * 主题预览卡。
 *
 * KernelSU 用 material-kolor 渲染一整张"迷你 App"预览。这里改用**当前真实的
 * MaterialTheme.colorScheme** 画几块色板 —— 换主色/明暗模式时会立即跟着变，
 * 效果比静态示意图更直观，而且零依赖。
 */
@Composable
private fun ThemePreviewCard(colorMode: ColorMode) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = when (colorMode) {
                    ColorMode.SYSTEM -> "跟随系统"
                    ColorMode.DARK -> "深色"
                    ColorMode.AMOLED -> "AMOLED 纯黑"
                },
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // 主色系三档 + 一档底色，一眼看出当前配色关系
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatch("主色", scheme.primary, scheme.onPrimary)
                Swatch("次色", scheme.secondaryContainer, scheme.onSecondaryContainer)
                Swatch("三色", scheme.tertiary, scheme.onTertiary)
                Swatch("底色", scheme.surfaceContainer, scheme.onSurface)
            }
        }
    }
}

@Composable
private fun RowScope.Swatch(label: String, background: Color, foreground: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.labelMedium,
                color = foreground,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 色盘上的一个圆点。
 *
 * @param argb  null 表示"动态取色"（由壁纸决定），用主题主色画一个圆点示意
 * @param label 圆点下方的文字，只有"动态"那一个需要
 */
@Composable
private fun ColorDot(
    argb: Int?,
    label: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fill = if (argb == null) MaterialTheme.colorScheme.primary else Color(argb)
    val ring = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(fill)
                .border(width = if (selected) 3.dp else 1.dp, color = ring, shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "已选中",
                    modifier = Modifier.size(22.dp),
                    tint = if (argb == null) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        Color.White
                    },
                )
            }
        }
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
