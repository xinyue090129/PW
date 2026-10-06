package com.example.shellconsole.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * ============================================================
 *  分段列表（对标 KernelSU Manager 的 SegmentedList.kt）
 * ============================================================
 *  【为什么是"重新实现"而不是"照搬"】
 *  KernelSU 那套组件基于 material3 1.4.x 的新 API：
 *  `SegmentedListItem`、`ListItemDefaults.segmentedShapes/segmentedColors/SegmentedGap`、
 *  `MenuDefaults.itemShape`、`SelectableDropdownMenuItem`、`LargeFlexibleTopAppBar`……
 *  本工程是 Compose BOM 2024.10.01（material3 1.3.1），上面这些**一个都没有**（逐个 javap 查过）。
 *  所以这里用稳定 API 复刻同样的观感与交互：
 *    - 分组卡片：外层圆角 16dp、组内条目间距 2dp、首尾条目外侧大圆角 / 内侧小圆角
 *    - 条目底色 surfaceBright（比页面底色亮一档，与 TonalCard 同一套层次）
 *    - 三类条目：可点击跳转（带右箭头）、开关、下拉选择
 *  代价只是少了 KernelSU 那两处展开/收起动画，视觉结果一致。
 *
 *  【图标说明】
 *  material-icons-core 只有 49 个图标，KernelSU 用的 Palette / Security / Adb /
 *  Brightness4 等都在 extended 包里（那是十几 MB 的体积，本工程刻意不引）。
 *  这里一律换成语义最接近的 core 图标，调用处都注明了原图标名。
 * ============================================================
 */

/** 分组外圆角 */
private val GroupOuterRadius = 16.dp

/** 分组内（相邻条目之间）的圆角 */
private val GroupInnerRadius = 4.dp

/** 组内条目之间的缝隙 */
private val ItemGap = 2.dp

/** 当前条目应使用的外形，由 [SegmentedColumn] 通过 CompositionLocal 下发 */
@Stable
private class ItemShapeHolder(val shape: Shape)

private val LocalItemShape = compositionLocalOf<ItemShapeHolder?> { null }

/** 按条目在组内的位置算出圆角：首尾外侧大圆角，中间小圆角 */
private fun shapeFor(index: Int, count: Int): Shape {
    if (count <= 1) return RoundedCornerShape(GroupOuterRadius)
    val isFirst = index == 0
    val isLast = index == count - 1
    return RoundedCornerShape(
        topStart = if (isFirst) GroupOuterRadius else GroupInnerRadius,
        topEnd = if (isFirst) GroupOuterRadius else GroupInnerRadius,
        bottomStart = if (isLast) GroupOuterRadius else GroupInnerRadius,
        bottomEnd = if (isLast) GroupOuterRadius else GroupInnerRadius,
    )
}

/**
 * 一组设置条目。
 *
 * @param title  分组标题（显示在组上方，主色小字）；为空则不显示
 * @param content 条目内容列表，顺序即显示顺序
 */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(ItemGap)) {
            content.forEachIndexed { index, itemContent ->
                CompositionLocalProvider(
                    LocalItemShape provides ItemShapeHolder(shapeFor(index, content.size))
                ) {
                    itemContent()
                }
            }
        }
    }
}

/**
 * 带"可见性"的 DSL 版本。
 *
 * 用途与 KernelSU 一致：有些条目只在特定条件下出现（例如"卸载"只在 LKM 模式下显示），
 * 用 `item(visible = ...)` 让**圆角自动重算** —— 隐藏的条目不会被算进首尾，
 * 可见的最后一条仍然是大圆角。若直接在调用处写 `if (cond) { ... }`，
 * 配合 [content] 列表版本其实也能得到正确圆角，两种写法等价，按可读性挑。
 */
class SegmentedColumnScope {
    internal data class Entry(val visible: Boolean, val content: @Composable () -> Unit)

    internal val entries = mutableListOf<Entry>()

    fun item(visible: Boolean = true, content: @Composable () -> Unit) {
        entries.add(Entry(visible, content))
    }
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SegmentedColumnScope.() -> Unit,
) {
    val entries = SegmentedColumnScope().apply(content).entries.filter { it.visible }
    if (entries.isEmpty()) return
    SegmentedColumn(modifier = modifier, title = title, content = entries.map { it.content })
}

// ================================================================
//  三类条目
// ================================================================

/**
 * 分组里的一个条目。
 *
 * @param onClick  为 null 表示不可点击（纯展示）
 * @param enabled  false 时整体置灰，且不响应点击 —— KernelSU 用它表示"该功能不支持"
 * @param leading  左侧内容，通常是图标
 * @param trailing 右侧内容，通常是开关 / 箭头 / 当前值
 */
@Composable
fun SettingItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = LocalItemShape.current?.shape ?: RoundedCornerShape(GroupOuterRadius)
    val clickable = onClick != null && enabled

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (clickable) Modifier.clickable(onClick = onClick!!) else Modifier),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) { leading() }
                Spacer(Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                    },
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

/** 条目左侧图标的统一着色；不支持时自动置灰 */
@Composable
fun SettingIcon(icon: ImageVector, enabled: Boolean = true) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
        },
    )
}

/** 开关条目：点整行即可切换（与 KernelSU 一致，不必精确点到开关上） */
@Composable
fun SettingSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingItem(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        leading = icon?.let { { SettingIcon(it, enabled) } },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null, // 整行已经处理点击，开关只做展示避免双触发
                enabled = enabled,
            )
        },
    )
}

/** 可点击跳转条目：右侧一个箭头，点击进入二级页 */
@Composable
fun SettingNavItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingItem(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        leading = icon?.let { { SettingIcon(it, enabled) } },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                },
            )
        },
    )
}

/** 下拉选择条目：右侧显示当前项，点击弹出菜单 */
@Composable
fun SettingDropdownItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = if (items.isEmpty()) -1 else selectedIndex.coerceIn(0, items.lastIndex)

    Box {
        SettingItem(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = { if (enabled) expanded = true },
            modifier = modifier,
            leading = icon?.let { { SettingIcon(it, enabled) } },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (safeIndex >= 0) items[safeIndex] else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
                        },
                    )
                }
            },
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    },
                    leadingIcon = if (index == safeIndex) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** 单选条目：左侧一个单选圆点 */
@Composable
fun SettingRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingItem(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        leading = {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        },
    )
}

/** 多选条目：左侧一个复选框 */
@Composable
fun SettingCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingItem(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        leading = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

/** 不支持的条目统一用的说明文案 */
const val UNSUPPORTED_SUMMARY = "暂不支持 · 本 App 未接入该功能"

/** 置灰时的不透明度（与 Material3 的 disabled 一致） */
internal const val DisabledAlpha = 0.38f
