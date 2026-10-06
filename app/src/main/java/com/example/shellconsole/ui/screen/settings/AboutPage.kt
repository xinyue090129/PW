package com.example.shellconsole.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.shellconsole.R
import com.example.shellconsole.ui.PageContentWindowInsets
import com.example.shellconsole.ui.component.SegmentedColumn
import com.example.shellconsole.ui.component.SettingItem
import com.example.shellconsole.ui.component.SettingNavItem

/**
 * ============================================================
 *  二级页：关于（对标 KernelSU Manager 的 AboutScreen）
 * ============================================================
 *  KernelSU 的结构：居中大图标 + 应用名 + 版本，下面一组可点击的链接条目。
 *  这里照搬这个版式；链接换成对本 App 真正有意义的：
 *    - 应用包名 / 版本 / 运行环境：纯信息行，不可点
 *    - KernelSU Manager：点开项目主页 —— 本 App 的界面（主题、四页布局、设置页
 *      分段列表）是照着它做的，把出处写在"关于"里比藏在注释里合适。
 * ============================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onBack: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifEmpty { "1.0" }
    }

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = PageContentWindowInsets,
        topBar = {
            LargeTopAppBar(
                title = { Text("关于", fontWeight = FontWeight.Bold) },
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
            // ---------- 顶部：图标 + 应用名 + 版本 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(80.dp),
                    )
                }
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------- 应用信息 ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "应用信息") {
                item {
                    SettingItem(
                        title = "包名",
                        summary = context.packageName,
                        leading = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )
                }
                item {
                    SettingItem(
                        title = "版本",
                        summary = versionName,
                        leading = { Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )
                }
                item {
                    SettingItem(
                        title = "界面立足点",
                        summary = "root 常驻 shell + 脚本控制台",
                        leading = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )
                }
            }

            Spacer(Modifier.height(13.dp))

            // ---------- 界面参考来源 ----------
            SegmentedColumn(modifier = Modifier.padding(horizontal = 16.dp), title = "界面参考") {
                item {
                    SettingNavItem(
                        title = "KernelSU Manager",
                        summary = "主题、四页布局与设置页分段列表均对标该项目",
                        icon = Icons.Filled.Star,
                        onClick = { openUrl("https://github.com/tiann/KernelSU") },
                    )
                }
            }

            Spacer(Modifier.height(bottomInnerPadding + 16.dp))
        }
    }
}
