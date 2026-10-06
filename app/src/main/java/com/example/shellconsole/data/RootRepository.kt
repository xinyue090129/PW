package com.example.shellconsole.data

import java.util.concurrent.TimeUnit

/**
 * ============================================================
 *  ROOT 权限检测
 * ============================================================
 *  原理与旧 Java 版完全一致：后台起一个 su，喂进去 `id`，看输出里有没有 `uid=0`。
 *  这是判断"设备是否已 root 且已授权本应用"最直接的办法。
 * ============================================================
 */
object RootRepository {

    /**
     * 尝试以 root 身份执行 id。
     *
     * @return true 表示已拿到 root（输出中出现 uid=0）
     *
     * 注意：本方法会阻塞，必须放在子线程调用（su 未授权时会等待用户在弹窗上操作）。
     */
    fun isRootGranted(): Boolean {
        var process: Process? = null
        return try {
            // 用非空局部变量持有进程：process 需要可空（finally 里要销毁），
            // 而 Kotlin 对可空变量做判断后能直接智能转换，写成非空的 p 更省事、也更安全
            val p = Runtime.getRuntime().exec("su")
            process = p

            // 通过标准输入喂命令，最后用 exit 结束这个临时 shell
            p.outputStream.use { stdin ->
                stdin.write("id\n".toByteArray(Charsets.UTF_8))
                stdin.write("exit\n".toByteArray(Charsets.UTF_8))
                stdin.flush()
            }

            // 一次性读完 stdout（shell 执行完 exit 后会关闭，readText 随之返回）
            val output = p.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            // 兜底等待，避免个别 su 实现迟迟不回收进程
            p.waitFor(10, TimeUnit.SECONDS)

            // root 身份下 id 的输出形如：uid=0(root) gid=0(root) ...
            output.contains("uid=0")
        } catch (e: Exception) {
            // 设备没有 su、或用户在授权弹窗里点了拒绝，都会走到这里
            false
        } finally {
            process?.destroy()
        }
    }
}
