package com.example.shellconsole.data

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * ============================================================
 *  脚本文件读取
 * ============================================================
 *  用法与旧 Java 版一致，两步兜底：
 *    ① 先走 ContentResolver.openInputStream（content:// 与 file:// 都支持）；
 *    ② 读不到时（第三方文件管理器返回 file:// 路径、又没有存储权限），改走 root `cat`。
 *       本应用本来就有 root，正好用上，也省得申请存储权限。
 * ============================================================
 *
 *  读文件属于 IO，必须放在子线程调用。
 */
class ScriptLoader(private val resolver: ContentResolver) {

    /**
     * 读取 URI 指向文件的文本内容。
     *
     * 【不做大小限制】
     * 按需求不设文件大小上限 —— 脚本多大都照读。代价是会一次性载入内存，
     * 若选到几百 MB 的文件可能触发 OOM；真要防御，在上面套一层长度检查即可。
     *
     * @return 已统一换行符（\n）的文本
     * @throws IOException 文件不可读或不是文本
     */
    @Throws(IOException::class)
    fun load(uri: Uri): String {
        var primaryError: IOException? = null
        try {
            val input = resolver.openInputStream(uri)
                ?: throw IOException("无法打开所选文件")
            input.use {
                return toScriptText(readAllBytes(it))
            }
        } catch (e: IOException) {
            primaryError = e
        } catch (e: SecurityException) {
            primaryError = IOException("没有读取该文件的权限")
        }

        // 普通方式读不到：若拿到的是 file:// 路径，改用 root 读
        if ("file" == uri.scheme) {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                return readViaRoot(path)
            }
        }
        throw primaryError ?: IOException("无法读取所选文件")
    }

    /** 取一个用于界面展示的文件名（简单取路径最后一段，不额外查询 ContentResolver） */
    fun displayName(uri: Uri): String {
        val path = uri.path
        if (path != null) {
            val index = path.lastIndexOf('/')
            if (index in 0 until path.length - 1) {
                return path.substring(index + 1)
            }
        }
        return uri.toString()
    }

    // ================================================================
    //  内部实现
    // ================================================================

    /**
     * 用 root 权限直接读指定路径的文本。
     * 第三方文件管理器返回 file:// 路径时，普通读取会被系统以权限为由拒绝，
     * 这里改走 su + cat，绕过存储权限限制。
     */
    @Throws(IOException::class)
    private fun readViaRoot(path: String): String {
        var process: Process? = null
        try {
            // 用非空局部变量持有进程，避免对可空属性反复判断（Kotlin 无法对可变属性智能转换）
            val p = Runtime.getRuntime().exec("su")
            process = p

            // 路径用单引号包住，避免空格被 Shell 拆成多个参数；路径内的单引号按 Shell 规则转义
            val quoted = "'" + path.replace("'", "'\\''") + "'"

            p.outputStream.use { stdin ->
                stdin.write("cat $quoted\n".toByteArray(Charsets.UTF_8))
                stdin.write("exit\n".toByteArray(Charsets.UTF_8))
                stdin.flush()
            }

            val bytes = readAllBytes(p.inputStream)
            val exitCode = p.waitFor()

            // cat 失败（文件不存在 / 不可读）时退出码非 0
            if (exitCode != 0) {
                throw IOException("读取失败（文件不存在或不可读）")
            }
            return toScriptText(bytes)
        } catch (e: InterruptedException) {
            throw IOException("读取文件被中断")
        } finally {
            process?.destroy()
        }
    }

    /** 把输入流读成字节数组（不设上限） */
    @Throws(IOException::class)
    private fun readAllBytes(input: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val len = input.read(chunk)
            if (len == -1) break
            buffer.write(chunk, 0, len)
        }
        return buffer.toByteArray()
    }

    /**
     * 原始字节 → 可直接执行的脚本文本。
     * ① 拦截二进制文件（误选图片 / APK 时给出明确提示）
     * ② 统一换行符（Windows 的 \r\n 会让 Shell 把 \r 当成命令的一部分而报错）
     */
    @Throws(IOException::class)
    private fun toScriptText(bytes: ByteArray): String {
        val checkLength = minOf(bytes.size, 8192)
        for (i in 0 until checkLength) {
            if (bytes[i] == 0.toByte()) {
                throw IOException("所选文件不是文本脚本（检测到二进制内容）")
            }
        }
        return String(bytes, Charsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n")
    }
}
