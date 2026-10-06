package com.example.shellconsole.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shellconsole.data.FilePicker
import com.example.shellconsole.data.PickerTarget
import com.example.shellconsole.ui.ConsoleViewModel
import com.example.shellconsole.ui.PageContentWindowInsets
import com.example.shellconsole.ui.RootState
import com.example.shellconsole.ui.component.TonalCard
import com.example.shellconsole.ui.theme.MonoTerminalStyle

/**
 * 终端面板固定用的深色（终端永远是深色，不跟随主题切换，与真实终端一致）
 */
private val TerminalBackground = Color(0xFF0E1113)
private val TerminalForeground = Color(0xFFECEFF1)
private val TerminalPrompt = Color(0xFF66BB6A)
private val TerminalDivider = Color(0xFF37474F)

/**
 * ============================================================
 *  首页（第 0 页）：ROOT / 存储 / 脚本 / 控制台
 * ============================================================
 *  内容与旧版单页界面完全一致，改动只有两点：
 *    ① 外面套一层 Scaffold 提供顶部标题栏（与 KernelSU 每个主页面各自带 TopAppBar 一致）；
 *    ② 卡片从"比底色暗"的 surfaceContainerHigh 换成"比底色亮"的 TonalCard，
 *       层次方向与 KernelSU 一致（底色 surfaceContainer → 卡片 surfaceBright）。
 *
 *  【键盘避让】
 *  根 Column 加 Modifier.imePadding()：软键盘弹起时整页可用高度自动缩短，
 *  下方控制台始终可见。底部导航栏在键盘弹出时由 MainScreen 自动隐藏，
 *  所以这里的 imePadding 不会被导航栏高度干扰。
 * ============================================================
 *
 * @param bottomInnerPadding 底部导航栏占用的高度；由 MainScreen 传入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    viewModel: ConsoleViewModel,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current

    val rootState by viewModel.rootState.collectAsStateWithLifecycle()
    val storageGranted by viewModel.storageGranted.collectAsStateWithLifecycle()
    val scriptText by viewModel.scriptText.collectAsStateWithLifecycle()
    val pickedFileName by viewModel.pickedFileName.collectAsStateWithLifecycle()
    val output by viewModel.output.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    /** 非 null 时展示"选择文件管理器"面板 */
    var pickerTargets by remember { mutableStateOf<List<PickerTarget>?>(null) }

    // ---------- 一次性提示：显示后立即消费掉，避免旋转屏幕重复弹 ----------
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    // ---------- 文件选择结果回传 ----------
    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { viewModel.loadScript(it) }
        }
    }

    // ---------- 存储权限（Android 10 及以下用运行时权限弹窗）----------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshStorage() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = PageContentWindowInsets,
        topBar = {
            TopAppBar(title = { Text("Shell Console", fontWeight = FontWeight.Bold) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                // 键盘避让：IME 高度自动变成底部内边距
                .imePadding()
                // 底部导航栏占位：内容停在导航栏上方，而不是被压在下面
                .padding(bottom = bottomInnerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ============ 上方卡片区：weight 占剩余空间，内容超高时自身可滚动 ============
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ① ROOT 状态卡
                RootCard(state = rootState, onCheck = { viewModel.checkRoot() })

                Spacer(Modifier.height(8.dp))

                // ② 存储权限卡
                StorageCard(
                    granted = storageGranted,
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            // Android 11+：「所有文件访问」是特殊权限，只能到系统设置页手动开启
                            try {
                                context.startActivity(FilePicker.buildAllFilesAccessIntent(context))
                            } catch (e: Exception) {
                                // 个别定制 ROM 没有该设置页，退回到"所有文件访问"的应用列表页
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    )
                                } catch (e2: Exception) {
                                    Toast.makeText(
                                        context,
                                        "无法打开设置页：${e2.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                )
                            )
                        }
                    },
                )

                Spacer(Modifier.height(12.dp))

                // ③ 自定义脚本卡
                ScriptCard(
                    script = scriptText,
                    pickedFileName = pickedFileName,
                    onScriptChange = { viewModel.updateScript(it) },
                    onPickFile = {
                        // 自己枚举"能选文件的应用"，让用户明确挑一个；
                        // 只有系统默认选择器的话就直接拉起它，避免多一层无意义的选择
                        val targets = runCatching {
                            FilePicker.findTargets(context.packageManager)
                        }.getOrDefault(emptyList())

                        when {
                            targets.isEmpty() ->
                                pickFileLauncher.launch(FilePicker.buildFallbackIntent())

                            targets.size == 1 ->
                                pickFileLauncher.launch(FilePicker.buildIntent(targets[0]))

                            else -> pickerTargets = targets
                        }
                    },
                    onRun = {
                        if (!viewModel.runScript()) {
                            Toast.makeText(context, "请先输入或选择一个脚本文件", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ============ ④ 控制台：weight 略大，保证输出与输入行始终可见 ============
            ConsoleCard(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxWidth(),
                output = output,
                input = input,
                onInputChange = { viewModel.updateInput(it) },
                onSubmit = {
                    if (!viewModel.submitInput()) {
                        Toast.makeText(context, "请输入要执行的命令", Toast.LENGTH_SHORT).show()
                    }
                },
                onPrevious = {
                    if (!viewModel.previousCommand()) {
                        Toast.makeText(context, "暂无历史命令", Toast.LENGTH_SHORT).show()
                    }
                },
                onClear = { viewModel.clearOutput() },
            )
        }
    }

    // ---------- 文件管理器选择面板 ----------
    pickerTargets?.let { targets ->
        ModalBottomSheet(onDismissRequest = { pickerTargets = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "选择文件管理器",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                targets.forEach { target ->
                    Text(
                        text = target.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickerTargets = null
                                pickFileLauncher.launch(FilePicker.buildIntent(target))
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

// ================================================================
//  ① ROOT 状态卡
// ================================================================

@Composable
private fun RootCard(state: RootState, onCheck: () -> Unit) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "ROOT 权限",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (state) {
                        RootState.CHECKING -> "检测中…"
                        RootState.GRANTED -> "已获取权限（uid=0）"
                        RootState.DENIED -> "未获取权限 / 无 su"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    // 已授权=绿、未授权=红（与旧版配色一致），检测中沿用主题色
                    color = when (state) {
                        RootState.GRANTED -> Color(0xFF4CAF50)
                        RootState.DENIED -> Color(0xFFEF5350)
                        RootState.CHECKING -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium,
                )
            }
            FilledTonalButton(
                onClick = onCheck,
                enabled = state != RootState.CHECKING,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("重新检测")
            }
        }
    }
}

// ================================================================
//  ② 存储权限卡
// ================================================================

@Composable
private fun StorageCard(granted: Boolean, onGrant: () -> Unit) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "存储权限",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (granted) "已获得「所有文件访问」" else "未授权，点右侧按钮授予",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (granted) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    fontWeight = FontWeight.Medium,
                )
            }
            FilledTonalButton(onClick = onGrant, enabled = !granted) {
                Text(if (granted) "已授权" else "去授权")
            }
        }
    }
}

// ================================================================
//  ③ 自定义脚本卡
// ================================================================

@Composable
private fun ScriptCard(
    script: String,
    pickedFileName: String,
    onScriptChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRun: () -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "自定义脚本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onPickFile) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("选择文件")
                }
            }

            OutlinedTextField(
                value = script,
                onValueChange = onScriptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text("输入 Shell 脚本；或点右上角「选择文件」填入脚本路径", style = MonoTerminalStyle)
                },
                textStyle = MonoTerminalStyle,
                shape = RoundedCornerShape(8.dp),
            )

            // 执行规则：让用户一眼知道输入框里放"路径"和放"脚本"的区别
            Text(
                text = "以 / 开头的路径 → 用 sh 执行该文件（路径里的换行会被自动忽略）；否则按内联脚本执行",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 6.dp),
            )

            // 已选文件的路径 / 载入结果（未选择时为空，不占高度）
            if (pickedFileName.isNotEmpty()) {
                Text(
                    text = pickedFileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Button(
                onClick = onRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("一键执行", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================================================================
//  ④ 控制台
// ================================================================

@Composable
private fun ConsoleCard(
    modifier: Modifier,
    output: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPrevious: () -> Unit,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()

    // 有新输出就贴到底部。
    // 这里用 LazyListState 而不是 ScrollView.fullScroll —— 后者内部会 requestFocus()，
    // 会把输入框的焦点抢走导致软键盘收起（旧版就踩过这个坑）。
    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            listState.scrollToItem(output.lastIndex)
        }
    }

    Surface(
        modifier = modifier,
        color = TerminalBackground,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---------- 标题行 ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "控制台",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF90A4AE),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        Modifier.size(16.dp),
                        tint = Color(0xFFB0BEC5),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("清空", color = Color(0xFFB0BEC5))
                }
            }

            // ---------- 输出区 ----------
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                if (output.isEmpty()) {
                    item {
                        Text(
                            text = "在下方输入命令后回车执行",
                            style = MonoTerminalStyle,
                            color = Color(0xFF546E7A),
                        )
                    }
                }
                // 每个片段单独一项；Text 内部自带换行渲染，多行片段也能正常显示
                itemsIndexed(output) { _, chunk ->
                    Text(
                        text = chunk,
                        style = MonoTerminalStyle,
                        color = TerminalForeground,
                    )
                }
            }

            HorizontalDivider(color = TerminalDivider)

            // ---------- 输入行 ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\$",
                    style = MonoTerminalStyle.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = TerminalPrompt,
                )
                Spacer(Modifier.width(6.dp))

                // 用 BasicTextField 而不是 OutlinedTextField：终端里不要边框和标签
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MonoTerminalStyle.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = TerminalForeground,
                    ),
                    cursorBrush = SolidColor(TerminalPrompt),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    // 软键盘回车 = 提交；焦点不转移，键盘不会意外收起
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                )
                Spacer(Modifier.width(6.dp))

                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "上一条命令",
                        tint = Color(0xFFB0BEC5),
                    )
                }
                IconButton(onClick = onSubmit) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "执行",
                        tint = TerminalPrompt,
                    )
                }
            }
        }
    }
}
