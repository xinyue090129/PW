package com.example.shellconsole.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * 一个可用于"选文件"的应用。
 *
 * @param packageName  包名
 * @param activityName 具体 Activity 类名（用于显式启动，绕开系统自带选择器）
 * @param label        展示给用户看的应用名
 */
data class PickerTarget(
    val packageName: String,
    val activityName: String,
    val label: String,
)

/**
 * ============================================================
 *  文件管理器枚举
 * ============================================================
 *  【为什么不用 Intent.createChooser】
 *  用 createChooser 时列表由系统按"谁能处理这个 Intent"来拼。我们带了
 *  CATEGORY_OPENABLE、类型又是 * / *（通配），多数第三方文件管理器（MT管理器、ES 等）
 *  的声明与之不匹配，列表里往往只剩系统自带的 com.google.android.documentsui，
 *  用户根本没得选。
 *
 *  【现在怎么做】
 *  自己用 PackageManager 查出设备上所有能选文件的应用，让用户明确挑一个，
 *  再用"显式组件"直接启动它 —— 系统就不会再插一层自己的选择器。
 *  选中后的返回值照旧通过 ActivityResult 回传，后续载入逻辑完全一致。
 *
 *  【坑】Kotlin 的块注释是可嵌套的，注释正文里绝不能出现斜杠加星号（/ + *），
 *  否则会被当成内层注释的开始，导致本段 KDoc 提前"多吞"到下一个星号加斜杠，
 *  把后面的真实代码一起注释掉。所以下面描述通配类型时写成 * / *（中间留空格）。
 *
 *  【前置条件】
 *  Android 11+ 有包可见性限制，Manifest 里必须声明 <queries>（GET_CONTENT / OPEN_DOCUMENT），
 *  否则这里枚举出来的永远只有系统自带应用。
 * ============================================================
 */
object FilePicker {

    /**
     * 查询设备上所有能用来选文件的应用，按包名去重。
     *
     * 两种声明方式都查一遍取并集：
     *   ① 带 CATEGORY_OPENABLE —— 系统自带「文件」及按标准写法实现的管理器
     *   ② 不带 CATEGORY_OPENABLE —— 不少第三方管理器只声明了 ACTION_GET_CONTENT
     */
    fun findTargets(pm: PackageManager): List<PickerTarget> {
        val result = ArrayList<PickerTarget>()
        val seenPackages = HashSet<String>()

        for (intent in listOf(buildPickIntent(true), buildPickIntent(false))) {
            val found: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            for (info in found) {
                val pkg = info.activityInfo.packageName ?: continue
                if (seenPackages.add(pkg)) {
                    result.add(
                        PickerTarget(
                            packageName = pkg,
                            activityName = info.activityInfo.name,
                            label = info.loadLabel(pm).toString(),
                        )
                    )
                }
            }
        }
        return result
    }

    /** 用指定应用去选文件（显式组件，系统不会再弹它自己的选择器） */
    fun buildIntent(target: PickerTarget): Intent =
        buildPickIntent(true).apply {
            component = ComponentName(target.packageName, target.activityName)
        }

    /** 直接调系统默认选择器（没有任何候选应用时的兜底） */
    fun buildFallbackIntent(): Intent = buildPickIntent(true)

    /** 存储授权设置页（Android 11+ 的「所有文件访问」） */
    fun buildAllFilesAccessIntent(context: Context): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(android.net.Uri.parse("package:${context.packageName}"))

    /**
     * 构造"选一个文件"的 Intent。
     *
     * @param openable 是否附加 CATEGORY_OPENABLE（只让用户选"确实能打开"的文件）
     */
    private fun buildPickIntent(openable: Boolean): Intent =
        Intent(Intent.ACTION_GET_CONTENT).apply {
            // 脚本后缀五花八门（.sh / .txt / 无后缀），用通配类型最省事
            type = "*/*"
            if (openable) addCategory(Intent.CATEGORY_OPENABLE)
        }
}
