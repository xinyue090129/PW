package com.example.shellconsole.ui.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build as BuildLine
import androidx.compose.material.icons.outlined.Home as HomeLine
import androidx.compose.material.icons.outlined.Lock as LockLine
import androidx.compose.material.icons.outlined.Settings as SettingsLine
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/**
 * ============================================================
 *  底部导航 / 侧边导航（对标 KernelSU Manager 的 BottomBar）
 * ============================================================
 *  【图标为什么和 KernelSU 不一样】
 *  KernelSU 用 Shield（超级用户）和 Extension（模块），二者都在
 *  material-icons-extended 包里。本工程只依赖 material-icons-core
 *  （49 个图标，见 build.gradle.kts 的注释），所以换成语义最接近的：
 *      Shield    -> Lock    （都是"权限/管控"）
 *      Extension -> Build   （都是"插件/构建"）
 *  要追求像素级一致，把 build.gradle.kts 里的依赖换成 extended 即可。
 *
 *  【为什么 outlined 图标要起别名】
 *  Kotlin 里 `filled.Home` 与 `outlined.Home` 是**同名扩展属性**，
 *  同时导入会冲突，所以 outlined 一侧统一加 `Line` 后缀别名。
 *
 *  【与 KernelSU 的差异】
 *  KernelSU 用了 material3 较新的 ShortNavigationBar / WideNavigationRail
 *  （带展开收起动画）。本工程的 Compose BOM 是 2024.10.01（material3 1.3.1），
 *  没有这两个 API，改用稳定的 NavigationBar / NavigationRail —— 观感与交互一致，
 *  只是侧边栏固定为收起的图标态，不能展开成"图标+文字"。
 * ============================================================
 */

/** 四个主页面。ordinal 即 HorizontalPager 的页码，二者必须一一对应。 */
enum class MainDestination(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.HomeLine),
    SUPERUSER("超级用户", Icons.Filled.Lock, Icons.Outlined.LockLine),
    MODULE("模块", Icons.Filled.Build, Icons.Outlined.BuildLine),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.SettingsLine),
    ;

    companion object {
        /** 页数；MainScreen 的 Pager 与导航栏都用它，避免两处写死 4 */
        val PAGE_COUNT = entries.size
    }
}

/**
 * 导航角标的数据来源。
 *
 * 说明：本 App 目前没有"已授权应用数 / 模块更新数"这类计数，所以 MainScreen
 * 传进来的是全 0 的空状态，角标不会渲染。保留这套结构是为了后续接入功能时
 * 不用再改导航栏组件 —— KernelSU 正是靠它显示"有几个模块可更新"。
 */
@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleEnabledCount: Int = 0,
    val moduleUpdatableCount: Int = 0,
)

/** 角标配色：Alert 表示"有可更新"，用主题错误色；Accent 表示"有内容"，用主色 */
internal enum class BadgeTone { Alert, Accent }

@Immutable
internal data class NavBadge(val count: Int, val tone: BadgeTone)

/**
 * 计算某一页要显示的角标。
 * 规则与 KernelSU 一致：模块页优先显示"可更新数"（Alert），没有更新才显示"已启用数"（Accent）。
 */
internal fun badgeFor(index: Int, state: NavigationBadgeState): NavBadge? = when (index) {
    MainDestination.SUPERUSER.ordinal ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    MainDestination.MODULE.ordinal -> when {
        state.moduleUpdatableCount > 0 -> NavBadge(state.moduleUpdatableCount, BadgeTone.Alert)
        state.moduleEnabledCount > 0 -> NavBadge(state.moduleEnabledCount, BadgeTone.Accent)
        else -> null
    }

    else -> null
}

/** 带角标的导航图标；无角标时就是个普通 Icon，不产生多余布局 */
@Composable
internal fun NavigationIconWithBadge(
    icon: ImageVector,
    contentDescription: String?,
    badge: NavBadge?,
) {
    if (badge == null) {
        Icon(icon, contentDescription)
        return
    }
    BadgedBox(
        badge = {
            when (badge.tone) {
                BadgeTone.Alert -> Badge { Text(badge.count.toString()) }
                BadgeTone.Accent -> Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Text(badge.count.toString()) }
            }
        },
    ) {
        Icon(icon, contentDescription)
    }
}

/**
 * 手机竖屏用的底部导航栏。
 *
 * windowInsets 只取"水平 + 底部"：横向要给手势条/挖孔留位，纵向顶部不需要
 * （顶部由页面自己的 Scaffold 处理，导航栏在底部，加了顶部内边距反而会被顶起来）。
 *
 * @param containerColor 底色。做毛玻璃时由调用方传入带透明度的同色，
 *                       否则背后的模糊图层透不出来。
 */
@Composable
fun BottomBar(
    badgeState: NavigationBadgeState,
    mainPagerState: MainPagerState,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) {
        MainDestination.entries.forEach { dest ->
            val selected = mainPagerState.selectedPage == dest.ordinal
            NavigationBarItem(
                selected = selected,
                // 点已选中的项不做任何事，避免无意义的动画
                onClick = { if (!selected) mainPagerState.animateToPage(dest.ordinal) },
                icon = {
                    NavigationIconWithBadge(
                        icon = if (selected) dest.filledIcon else dest.outlinedIcon,
                        contentDescription = dest.label,
                        badge = badgeFor(dest.ordinal, badgeState),
                    )
                },
                label = {
                    Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

/**
 * 宽屏（平板 / 横屏）用的左侧导航栏。
 *
 * @param windowInsets 传给 NavigationRail 的安全区内边距。默认取"起点侧 + 垂直"；
 *                     当外层 Scaffold 已经把安全区算进 padding 时，传 [WindowInsets] 全 0 避免双重内边距。
 */
@Composable
fun SideRail(
    badgeState: NavigationBadgeState,
    mainPagerState: MainPagerState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = windowInsets,
    ) {
        MainDestination.entries.forEach { dest ->
            val selected = mainPagerState.selectedPage == dest.ordinal
            NavigationRailItem(
                selected = selected,
                onClick = { if (!selected) mainPagerState.animateToPage(dest.ordinal) },
                icon = {
                    NavigationIconWithBadge(
                        icon = if (selected) dest.filledIcon else dest.outlinedIcon,
                        contentDescription = dest.label,
                        badge = badgeFor(dest.ordinal, badgeState),
                    )
                },
                label = { Text(dest.label) },
            )
        }
    }
}
