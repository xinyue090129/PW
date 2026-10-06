package com.example.shellconsole.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * ============================================================
 *  卡片（对标 KernelSU Manager 的 TonalCard）
 * ============================================================
 *  与直接用 Material3 Card 的差别只有两处，但正是这两处决定了整体观感：
 *    ① 默认容器色是 surfaceBright 而不是 surfaceVariant —— 在 surfaceContainer
 *       这一层底色上，卡片是"更亮"的一档，形成向上的层次，而不是压下去。
 *    ② 默认圆角用 shapes.large（16dp）—— KernelSU 的卡片明显比 M3 默认的更圆。
 *
 *  同时把"可点 / 可长按 / 纯展示"三种形态收敛到一个组件里：M3 的 Card 有
 *  无参和带 onClick 两个重载，混用时颜色与形状容易不一致，这里统一。
 * ============================================================
 *
 * @param containerColor 卡片底色，默认比页面底色亮一档
 * @param shape          圆角形状
 * @param enabled        是否可交互
 * @param onClick        单击回调；为 null 则不可点
 * @param onLongClick    长按回调；非 null 时走 combinedClickable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = contentColorFor(containerColor),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
    when {
        // 需要长按：Card 本身没有长按重载，用 clip + combinedClickable 自己接
        onLongClick != null -> Card(
            modifier = modifier
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                ),
            colors = colors,
            shape = shape,
        ) { content() }

        onClick != null -> Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            shape = shape,
            interactionSource = interactionSource,
        ) { content() }

        else -> Card(
            modifier = modifier,
            colors = colors,
            shape = shape,
        ) { content() }
    }
}
