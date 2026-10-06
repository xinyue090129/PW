package com.example.shellconsole.data

import android.content.Context
import com.example.shellconsole.ui.screen.settings.AppearanceState
import com.example.shellconsole.ui.theme.ColorMode

/**
 * ============================================================
 *  主题偏好持久化
 * ============================================================
 *  只存两项：明暗模式、主色。
 *  主色 0 表示"动态取色"（与 KernelSU 的约定一致）。
 *  用 SharedPreferences 存，不上 DataStore —— 只有两个键，没必要引额外依赖。
 * ============================================================
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shell_console_settings", Context.MODE_PRIVATE)

    /** 明暗模式：默认跟随系统 */
    var colorMode: ColorMode
        get() = runCatching {
            ColorMode.valueOf(prefs.getString(KEY_COLOR_MODE, ColorMode.SYSTEM.name)!!)
        }.getOrDefault(ColorMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_COLOR_MODE, value.name).apply()

    /** 主色：默认 0（动态取色） */
    var keyColor: Int
        get() = prefs.getInt(KEY_KEY_COLOR, 0)
        set(value) = prefs.edit().putInt(KEY_KEY_COLOR, value).apply()

    /** 页面缩放倍率：1.0 为标准 */
    var pageScale: Float
        get() = prefs.getFloat(KEY_PAGE_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PAGE_SCALE, value).apply()

    /** 悬浮底栏：底栏脱离屏幕边缘、加圆角 */
    var floatingBottomBar: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BOTTOM_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BOTTOM_BAR, value).apply()

    /** 底栏毛玻璃：常驻底栏背后的内容做模糊 */
    var glassBottomBar: Boolean
        get() = prefs.getBoolean(KEY_GLASS_BOTTOM_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_GLASS_BOTTOM_BAR, value).apply()

    /** 悬浮底栏毛玻璃：只在「悬浮底栏」开启时生效 */
    var glassFloatingBottomBar: Boolean
        get() = prefs.getBoolean(KEY_GLASS_FLOATING_BOTTOM_BAR, false)
        set(value) = prefs.edit().putBoolean(KEY_GLASS_FLOATING_BOTTOM_BAR, value).apply()

    /** 一次性读出全部外观设置，供 MainActivity 组装初始状态 */
    fun readAppearance(): AppearanceState = AppearanceState(
        colorMode = colorMode,
        keyColor = keyColor,
        pageScale = pageScale,
        floatingBottomBar = floatingBottomBar,
        glassBottomBar = glassBottomBar,
        glassFloatingBottomBar = glassFloatingBottomBar,
    )

    private companion object {
        const val KEY_COLOR_MODE = "color_mode"
        const val KEY_KEY_COLOR = "key_color"
        const val KEY_PAGE_SCALE = "page_scale"
        const val KEY_FLOATING_BOTTOM_BAR = "floating_bottom_bar"
        const val KEY_GLASS_BOTTOM_BAR = "glass_bottom_bar"
        const val KEY_GLASS_FLOATING_BOTTOM_BAR = "glass_floating_bottom_bar"
    }
}
