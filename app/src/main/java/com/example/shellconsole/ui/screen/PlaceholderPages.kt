package com.example.shellconsole.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shellconsole.ui.PageContentWindowInsets
import com.example.shellconsole.ui.component.TonalCard

/**
 * ============================================================
 *  第 1 / 2 页：超级用户、模块（当前为占位骨架）
 * ============================================================
 *  布局按 KernelSU 的四页结构照搬，本 App 暂时没有对应功能，所以这两页
 *  先给出明确标注的占位页，而不是偷偷省略 —— 这样导航结构完整、后续接功能
 *  时只需替换页面内容，导航栏、Pager、返回键行为都不用再动。
 * ============================================================
 */

/** 第 1 页：超级用户。KernelSU 在此展示"已授权的应用列表"，本 App 尚未接入。 */
@Composable
fun SuperUserPage(bottomInnerPadding: Dp) {
    PlaceholderPage(
        title = "超级用户",
        icon = Icons.Filled.Lock,
        headline = "应用授权列表尚未接入",
        description = "这一页将用于管理哪些应用被允许获取 root 权限。" +
            "当前版本请直接在「首页」的控制台里使用 root 终端。",
        bottomInnerPadding = bottomInnerPadding,
    )
}

/** 第 2 页：模块。KernelSU 在此展示 KernelSU 模块，本 App 尚未接入。 */
@Composable
fun ModulePage(bottomInnerPadding: Dp) {
    PlaceholderPage(
        title = "模块",
        icon = Icons.Filled.Build,
        headline = "模块管理尚未接入",
        description = "这一页将用于安装、启用与更新模块（含可更新数量角标）。" +
            "本 App 目前只提供 shell 控制台能力，暂不包含模块体系。",
        bottomInnerPadding = bottomInnerPadding,
    )
}

/**
 * 占位页的公共骨架：顶部标题栏 + 居中的空状态卡片。
 * 空状态卡用 TonalCard，与其余页面保持同一套层次语言。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderPage(
    title: String,
    icon: ImageVector,
    headline: String,
    description: String,
    bottomInnerPadding: Dp,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = PageContentWindowInsets,
        topBar = {
            TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(bottom = bottomInnerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "占位页面 · 导航结构已就位",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
