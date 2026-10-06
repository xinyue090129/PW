package com.example.shellconsole.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * ============================================================
 *  字体排版
 * ============================================================
 *  KernelSU 只覆盖了 bodyLarge 一项（其余沿用 Material3 默认），
 *  这里照做，保证观感一致：正文字号稍大、行高宽松、字距 0.5sp。
 * ============================================================
 */
val ShellTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
)

/**
 * 控制台专用的等宽字体样式。
 * 终端输出必须等宽，否则列对齐（如 ls -l、表格类输出）会错乱。
 */
val MonoTerminalStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.sp,
)
