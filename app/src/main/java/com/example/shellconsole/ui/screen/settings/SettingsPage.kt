package com.example.shellconsole.ui.screen.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shellconsole.ui.PageContentWindowInsets
import com.example.shellconsole.ui.component.SegmentedColumn
import com.example.shellconsole.ui.component.SettingDropdownItem
import com.example.shellconsole.ui.component.SettingNavItem
import com.example.shellconsole.ui.component.SettingSwitchItem
import com.example.shellconsole.ui.component.UNSUPPORTED_SUMMARY
import com.example.shellconsole.ui.theme.ColorMode
import com.example.shellconsole.ui.theme.keyColorNames
import com.example.shellconsole.ui.theme.keyColorOptions

/**
 * ============================================================
 *  第 3 页：设置（对标 KernelSU Manager 的 SettingPager + SettingPagerMaterial）
 * ============================================================
 *  结构与 KernelSU 完全一致：
 *    ① Scaffold + 可折叠大标题顶栏（LargeTopAppBar，往上滑标题收成一行）
 *    ② 一列「分段卡片组」：组内条目首尾外侧大圆角、相邻处小圆角，组间留空隙
 *    ③ 条目分三类：开关、下拉选择、点击跳转二级页
 *    ④ 点击条目进入二级页，返回键逐级退回
 *
 *  【二级页的导航】
 *  KernelSU 用 miuix-nav（navigation3）+ 完整路由表。本工程**不引入任何导航库**，
 *  用一个 `mutableStateListOf<SettingsRoute>` 当返回栈 + BackHandler 就够了：
 *  只有两级、页面固定，导航库带来的收益还不如它多出来的依赖和构建开销。
 *
 *  【关于"照搬全部条目"】
 *  KernelSU 的设置项大多对应它自己的内核功能（SuCompat、SELinux 隐藏、ADB Root、
 *  软重启、自动越狱……）。这些条目在本 App 里全是置灰的，没有任何可操作性，
 *  只会把页面撑得很长 —— 按需求已把「超级用户 / 调试 / 维护」三组**整体删除**。
 *  保留的四组里，真正生效的仍然是「主题配色」与「关于」两级。
 * ============================================================
 */

/** 设置页的二级页面 */
private enum class SettingsRoute {
    /** 主题配色（对标 KernelSU 的 ColorPaletteScreen） */
    THEME,

    /** 关于（对标 KernelSU 的 AboutScreen） */
    ABOUT,
}

@Composable
fun SettingsPage(
    appearance: AppearanceState,
    actions: AppearanceActions,
    bottomInnerPadding: Dp,
) {
    // 轻量返回栈：空 = 停留在设置主页
    val backStack = remember { mutableStateListOf<SettingsRoute>() }
    fun pop() {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
    }

    // 只在本页有二级页时拦截返回键；栈空时放行，交回 MainScreen（回首页）或系统（退出）
    BackHandler(enabled = backStack.isNotEmpty()) { pop() }

    when (backStack.lastOrNull()) {
        null -> SettingsHome(
            appearance = appearance,
            onOpenTheme = { backStack.add(SettingsRoute.THEME) },
            onOpenAbout = { backStack.add(SettingsRoute.ABOUT) },
            bottomInnerPadding = bottomInnerPadding,
        )

        SettingsRoute.THEME -> ColorPalettePage(
            appearance = appearance,
            actions = actions,
            onBack = { pop() },
            bottomInnerPadding = bottomInnerPadding,
        )

        SettingsRoute.ABOUT -> AboutPage(
            onBack = { pop() },
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}

// ================================================================
//  设置主页
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHome(
    appearance: AppearanceState,
    onOpenTheme: () -> Unit,
    onOpenAbout: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = PageContentWindowInsets,
        topBar = {
            LargeTopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = topBarColors,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                // 让滚动驱动顶栏折叠 / 展开
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
        ) {
            // ---------- ① 更新检查（KernelSU 内核功能） ----------
            SegmentedColumn(title = "更新检查") {
                item {
                    SettingSwitchItem(
                        title = "检查更新",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.Refresh,
                        checked = true,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
                item {
                    SettingSwitchItem(
                        title = "检查模块更新",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.Refresh,
                        checked = true,
                        enabled = false,
                        onCheckedChange = {},
                    )
                }
            }

            // ---------- ② 界面 ----------
            SegmentedColumn(title = "界面") {
                item {
                    SettingDropdownItem(
                        title = "界面模式",
                        summary = "本 App 只提供 Material 一套界面",
                        items = listOf("小米（Miuix）", "Material"),
                        selectedIndex = 1,
                        icon = Icons.Filled.Settings,
                        enabled = false,
                        onItemSelected = {},
                    )
                }
                item {
                    SettingNavItem(
                        title = "主题配色",
                        summary = themeSummary(appearance.colorMode, appearance.keyColor),
                        icon = Icons.Filled.Star,
                        onClick = onOpenTheme,
                    )
                }
            }

            // ---------- ③ 配置模板 ----------
            SegmentedColumn(title = "配置") {
                item {
                    SettingNavItem(
                        title = "配置模板",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.List,
                        enabled = false,
                        onClick = {},
                    )
                }
            }

            // ---------- ④ 其他 ----------
            SegmentedColumn(title = "其他") {
                item {
                    SettingNavItem(
                        title = "发送日志",
                        summary = UNSUPPORTED_SUMMARY,
                        icon = Icons.Filled.Send,
                        enabled = false,
                        onClick = {},
                    )
                }
                item {
                    SettingNavItem(
                        title = "关于",
                        summary = "版本信息与界面参考来源",
                        icon = Icons.Filled.Info,
                        onClick = onOpenAbout,
                    )
                }
            }

            // 底部留白：避开底部导航栏（高度由 MainScreen 传入）
            Spacer(Modifier.height(bottomInnerPadding + 8.dp))
        }
    }
}

/** 「主题配色」条目右侧/下方的状态摘要，让用户不进二级页也知道当前是什么 */
private fun themeSummary(colorMode: ColorMode, keyColor: Int): String {
    val modeText = when (colorMode) {
        ColorMode.SYSTEM -> "跟随系统"
        ColorMode.DARK -> "深色"
        ColorMode.AMOLED -> "AMOLED 纯黑"
    }
    val colorText = if (keyColor == 0) {
        "动态取色"
    } else {
        val index = keyColorOptions.indexOf(keyColor)
        if (index >= 0) keyColorNames.getOrElse(index) { "自定义" } else "自定义"
    }
    return "$modeText · $colorText"
}
