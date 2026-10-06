package com.example.shellconsole.data

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

/**
 * ============================================================
 *  常驻 root shell 会话（本应用最核心的一块）
 * ============================================================
 *  与旧 Java 版逐条对齐的三个关键设计，改动前务必理解：
 *
 *  ① **stdin 永不关闭、也绝不发送 `exit`**
 *     脚本执行到 `read -p "是否立即跳转网盘? Y/N" ans` 时会挂起等输入。
 *     如果 stdin 被关掉，脚本会立刻收到 EOF，等同于"用户什么都没输入"，
 *     交互式脚本就永远走不下去。保持打开，用户输入的 y/n 才能续上。
 *
 *  ② **用 InputStreamReader 按"字符"读，而不是按行读**
 *     `read -p "提示语"` 这种提示是不带换行的；用 readLine() 会一直阻塞到出现换行，
 *     用户就永远看不到提示。按字符读只要有字符可读就立刻返回。
 *
 *  ③ **stdout 与 stderr 各起一个线程搬运**
 *     两者共用一个读线程会互相阻塞，输出量大时直接死锁。
 *
 *  @param onOutput 输出回调。可能在任意子线程被调用，界面层需自行切回主线程。
 */
class ConsoleSession(private val onOutput: (String) -> Unit) {

    /** 串行化"启动会话"与"写入 stdin"，避免多线程同时操作同一个流 */
    private val lock = Any()

    private var process: Process? = null
    private var stdin: OutputStream? = null

    /**
     * 上一条输出是否以换行结尾。
     * 用途：判断 shell 是"停在提示符后等输入"（false）还是"正常执行中"（true）。
     * 停在提示符后时，回显用户输入就不该再加 "$ " 前缀 —— 这样才像真实终端。
     */
    @Volatile
    var atLineStart: Boolean = true
        private set

    /**
     * 把一行文本送进 root shell。
     * 输入内容既可以是新命令，也可以是对脚本交互提示的应答（如 y / n），两者走同一条通道。
     */
    fun send(line: String) {
        // 回显：停在交互提示后 → 只回显输入本身（如 y）；否则以 "$ " 开头，模拟一条新命令
        emit(if (atLineStart) "$ $line\n" else "$line\n")

        // 写入操作放在子线程：首次启动 su 可能耗时，且 su 授权弹窗会阻塞
        Thread {
            try {
                synchronized(lock) {
                    ensureStarted()
                    val out = stdin ?: throw IOException("root shell 不可用")
                    out.write((line + "\n").toByteArray(Charsets.UTF_8))
                    // 必须 flush：否则内容留在缓冲区里，脚本读不到，表现为"输入了没反应"
                    out.flush()
                }
            } catch (e: Exception) {
                emit(
                    "执行失败：${e.message}\n" +
                        "可能原因：设备未获取 ROOT 权限，或系统中不存在 su 命令。\n"
                )
            }
        }.start()
    }

    /**
     * 清空显示后重置"是否停在行首"的状态。
     * 与旧版 clearTerminal() 保持一致：清屏后下一条输入重新带上 "$ " 前缀。
     */
    fun resetLineStart() {
        atLineStart = true
    }

    /** 主动结束会话（例如界面销毁时）；下次 send 会自动重开一个 */
    fun shutdown() {
        synchronized(lock) {
            try {
                stdin?.close()
            } catch (ignored: IOException) {
                // 关闭失败无需处理
            }
            stdin = null
            process?.destroy()
            process = null
        }
    }

    // ================================================================
    //  内部实现
    // ================================================================

    /**
     * 确保存在一个可用的 root shell；没有就启动一个。
     * 【注意】这里不关闭 stdin、也不发送 exit —— 会话需要长期存活以便随时交互。
     */
    private fun ensureStarted() {
        if (isAlive()) return

        val p = Runtime.getRuntime().exec("su")
        process = p
        stdin = p.outputStream

        // stdout 与 stderr 分别实时搬运到界面
        startPump(p.inputStream)
        startPump(p.errorStream)

        // 监听退出：退出后清空引用，下次输入会自动重开会话
        watchExit(p)
    }

    /** 搬运子进程的一条流（stdout 或 stderr）到界面 */
    private fun startPump(input: InputStream) {
        Thread {
            try {
                val reader = InputStreamReader(input, Charsets.UTF_8)
                val buffer = CharArray(512)
                while (true) {
                    val len = reader.read(buffer)
                    if (len == -1) break
                    emit(String(buffer, 0, len))
                }
            } catch (ignored: Exception) {
                // 会话结束时读取会抛异常，属于正常现象
            }
        }.start()
    }

    /** 监听 shell 退出 */
    private fun watchExit(p: Process) {
        Thread {
            var code = -1
            try {
                code = p.waitFor()
            } catch (ignored: InterruptedException) {
                // 正常退出即可
            }
            synchronized(lock) {
                if (process === p) {
                    process = null
                    stdin = null
                }
            }
            emit("\n[控制台会话结束，退出码 $code]\n")
        }.start()
    }

    /**
     * 会话是否还活着。
     * 这里用 exitValue() + 异常判断：能取到退出码说明进程已结束，
     * 抛 IllegalThreadStateException 说明仍在运行。
     */
    private fun isAlive(): Boolean {
        val p = process ?: return false
        return try {
            p.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    /** 输出统一出口：先记录"是否停在行首"，再交给界面 */
    private fun emit(text: String) {
        atLineStart = text.isNotEmpty() && text.last() == '\n'
        onOutput(text)
    }
}
