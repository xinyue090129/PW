package com.example.shellconsole

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.shellconsole.data.SettingsStore
import com.example.shellconsole.ui.ConsoleViewModel
import com.example.shellconsole.ui.MainScreen
import com.example.shellconsole.ui.screen.settings.AppearanceActions
import com.example.shellconsole.ui.screen.settings.AppearanceState
import com.example.shellconsole.ui.theme.ShellConsoleTheme

/**
 * ============================================================
 *  唯一 Activity：只做三件事
 * ============================================================
 *  ① 开启 edge-to-edge（让内容延伸到状态栏/导航栏下，配合 imePadding 做键盘避让）
 *  ② 从 SharedPreferences 读主题偏好，包上 ShellConsoleTheme
 *  ③ 把 ViewModel 交给 Compose 界面；onResume 时刷新存储权限状态
 *
 *  所有业务逻辑在 ConsoleViewModel；界面外壳（导航 + 分页）在 MainScreen，
 *  四个页面的具体内容在 ui/screen/ 下。
 * ============================================================
 */
class MainActivity : ComponentActivity() {

    /** 界面状态容器；由 Activity 持有，配置变更（旋转屏幕）时不会丢 */
    private val viewModel: ConsoleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 边到边显示：必须在 super.onCreate 之前调用
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 主题偏好：只有两个键，直接读 SharedPreferences
        val store = SettingsStore(this)

        setContent {
            // 外观设置的唯一状态源：改一次就立刻重组生效，同时写回 SharedPreferences
            var appearance by remember { mutableStateOf(store.readAppearance()) }

            // 页面缩放：把系统密度乘上倍率后再往下提供，整个界面（含底栏）一起缩放。
            // 这正是 KernelSU 的做法 —— 用 LocalDensity 而不是去改每个组件的尺寸。
            val systemDensity = LocalDensity.current
            val scaledDensity = remember(systemDensity, appearance.pageScale) {
                Density(systemDensity.density * appearance.pageScale, systemDensity.fontScale)
            }

            val appearanceActions = AppearanceActions(
                onColorModeChange = {
                    appearance = appearance.copy(colorMode = it)
                    store.colorMode = it
                },
                onKeyColorChange = {
                    appearance = appearance.copy(keyColor = it)
                    store.keyColor = it
                },
                onPageScaleChange = {
                    appearance = appearance.copy(pageScale = it)
                    store.pageScale = it
                },
                onFloatingBottomBarChange = {
                    appearance = appearance.copy(floatingBottomBar = it)
                    store.floatingBottomBar = it
                },
                onGlassBottomBarChange = {
                    appearance = appearance.copy(glassBottomBar = it)
                    store.glassBottomBar = it
                },
                onGlassFloatingBottomBarChange = {
                    appearance = appearance.copy(glassFloatingBottomBar = it)
                    store.glassFloatingBottomBar = it
                },
            )

            ShellConsoleTheme(
                colorMode = appearance.colorMode,
                keyColor = appearance.keyColor,
            ) {
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    MainScreen(
                        viewModel = viewModel,
                        appearance = appearance,
                        actions = appearanceActions,
                    )
                }
            }
        }

        // 进入界面先检测一次 ROOT
        viewModel.checkRoot()
    }

    override fun onResume() {
        super.onResume()
        // onResume 在 onCreate 之后、以及每次从系统设置页/权限弹窗返回时都会执行，
        // 用它刷新存储权限状态，"授权后返回"状态就能即时更新
        viewModel.refreshStorage()
    }
}
