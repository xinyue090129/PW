package com.example.shellconsole.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shellconsole.data.ConsoleSession
import com.example.shellconsole.data.RootRepository
import com.example.shellconsole.data.ScriptLoader
import com.example.shellconsole.data.ScriptPathResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** ROOT 权限检测结果 */
enum class RootState { CHECKING, GRANTED, DENIED }

/**
 * ============================================================
 *  界面状态与业务桥接（ViewModel）
 * ============================================================
 *  职责：
 *    - 持有常驻 root 会话，把子线程的输出收进 StateFlow（Compose 会自动重组界面）
 *    - 维护 root 状态 / 存储权限 / 脚本内容 / 命令历史 / 输入行文本
 *    - 所有耗时操作（su 检测、读文件）统一切到 IO 线程
 *
 *  用 AndroidViewModel 是因为需要 contentResolver 读脚本文件。
 * ============================================================
 */
class ConsoleViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        /** 控制台最多保留的输出片段数；终端跑久了无上限增长会吃内存 */
        const val MAX_CHUNKS = 2000

        /** 探测换行风格时最多读多少字节：只看开头，不整文件读 */
        const val CRLF_PROBE_BYTES = 64 * 1024

        /** 内联脚本落缓存时用的固定文件名（每次覆盖，不堆积垃圾文件） */
        const val INLINE_SCRIPT_FILE = "inline_script.sh"
    }

    /** 常驻 root 会话；输出回调可能来自任意子线程，这里直接写 StateFlow 是线程安全的 */
    private val session = ConsoleSession { text -> appendOutput(text) }
    private val loader = ScriptLoader(app.contentResolver)

    // ---------- 控制台输出 ----------
    private val _output = MutableStateFlow<List<String>>(emptyList())
    val output: StateFlow<List<String>> = _output.asStateFlow()

    // ---------- ROOT 状态 ----------
    private val _rootState = MutableStateFlow(RootState.CHECKING)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    // ---------- 存储权限 ----------
    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted.asStateFlow()

    // ---------- 脚本 ----------
    private val _scriptText = MutableStateFlow("")
    val scriptText: StateFlow<String> = _scriptText.asStateFlow()

    private val _pickedFileName = MutableStateFlow("")
    val pickedFileName: StateFlow<String> = _pickedFileName.asStateFlow()

    /**
     * 「选择文件」解析出来的**权威路径**。
     *
     * 存在的意义：不要再靠"输入框长什么样"去猜是不是路径。
     * 框里的内容可能被换行、被粘贴带进来的杂字符污染，但只要它还对应这个路径，
     * 执行时就以它为准。用户手动改了框里的内容就把它清空，回到猜测逻辑。
     */
    private val _pickedPath = MutableStateFlow<String?>(null)
    val pickedPath: StateFlow<String?> = _pickedPath.asStateFlow()

    // ---------- 终端输入行与历史 ----------
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    /** 历史游标：等于列表长度时表示"还没翻过历史" */
    private var historyIndex = 0

    // ---------- 一次性提示（Toast 用） ----------
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    // ================================================================
    //  ROOT
    // ================================================================

    /** 检测 ROOT 权限（后台线程执行 su + id） */
    fun checkRoot() {
        _rootState.value = RootState.CHECKING
        viewModelScope.launch {
            val granted = withContext(Dispatchers.IO) { RootRepository.isRootGranted() }
            _rootState.value = if (granted) RootState.GRANTED else RootState.DENIED
        }
    }

    // ================================================================
    //  存储权限
    // ================================================================

    /**
     * 刷新存储权限状态。
     * - Android 11(API 30)+：「所有文件访问」只能在系统设置页手动开启，以它为准
     * - Android 6 ~ 10        ：用传统运行时权限 READ_EXTERNAL_STORAGE 判断
     */
    fun refreshStorage() {
        val app = getApplication<Application>()
        _storageGranted.value = when {
            Build.VERSION.SDK_INT >= 30 -> Environment.isExternalStorageManager()
            else -> app.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    // ================================================================
    //  脚本
    // ================================================================

    fun updateScript(text: String) {
        _scriptText.value = text
        // 用户手动动过内容后，之前"选择文件"记住的路径就不再权威了，回到猜测逻辑
        if (text != _pickedPath.value) _pickedPath.value = null
    }

    /**
     * 处理「选择文件」的结果。
     *
     * 优先解析出文件的**真实绝对路径**并填进脚本输入框 —— 拿到路径后，
     * 一键执行就是 `sh '<路径>'`，脚本作为"一个文件"被执行，多行结构不会被破坏。
     * 只有解析不出路径（第三方文件管理器用自己的私有 provider）时，
     * 才退回老路子：把文件内容读出来填进输入框。
     */
    fun loadScript(uri: Uri) {
        viewModelScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                runCatching {
                    ScriptPathResolver.resolve(getApplication<Application>().contentResolver, uri)
                }.getOrNull()
            }
            if (!resolved.isNullOrEmpty()) {
                applyScriptPath(resolved)
            } else {
                loadScriptContent(uri)
            }
        }
    }

    /** 路径解析成功：把路径填进输入框，并记为权威路径 */
    private fun applyScriptPath(path: String) {
        _scriptText.value = path
        _pickedPath.value = path
        // App 自己不一定有权限 stat 这个文件（没给「所有文件访问」时就是如此），
        // 所以这里只是尽力而为，真正的存在性校验交给执行时的 root shell
        val visibleToApp = runCatching { File(path).exists() }.getOrDefault(false)
        _pickedFileName.value = if (visibleToApp) {
            "已载入脚本：$path"
        } else {
            "已填入路径：$path（App 无法确认文件存在，执行时由 root 校验）"
        }
        _message.value = "脚本路径已填入，点「一键执行」以 root 运行"
    }

    /** 路径解析失败时的兜底：读文件内容填进输入框（不是路径，执行时按内联脚本处理） */
    private suspend fun loadScriptContent(uri: Uri) {
        val name = loader.displayName(uri)
        _message.value = "无法获取真实路径，改为载入脚本内容：$name …"
        val result = withContext(Dispatchers.IO) {
            runCatching { loader.load(uri) }
        }
        result.onSuccess { text ->
            _scriptText.value = text
            _pickedFileName.value = "已载入内容：$name（${text.length} 字符，未拿到路径）"
            _message.value = "脚本内容已载入，点「一键执行」运行"
        }.onFailure { e ->
            _pickedFileName.value = ""
            _message.value = "载入失败：${e.message}"
        }
    }

    /**
     * 执行脚本框里的内容。返回 false 表示内容为空（由界面提示用户）。
     *
     * 【两种输入形态，最终都收敛成"跑一个脚本文件"】
     *  ① 是脚本路径 → 执行 `sh '<路径>'`（判定见 [resolveScriptPath]）
     *  ② 否则视为**内联脚本**，先写进缓存目录的临时文件，再 `sh '<临时文件>'`。
     *     不再逐行喂 stdin —— 那样 for / if / 函数定义这类跨行结构会被拆坏，
     *     开头的 `#!/system/bin/sh` 也会被当成命令去执行而报错。
     *
     * 【Windows 换行（CRLF）】
     *  .sh 从 Windows / 聊天软件传过来常常是 \r\n。以前"读内容"时 ScriptLoader 会顺手
     *  把 \r\n 规范化成 \n，改成直接跑文件后这一步没了 —— 而 mksh 遇到行尾的 \r 会把
     *  `fi` / `done` / `then` 认成不存在的命令，表现就是满屏 not found。
     *  所以这里执行前先探一下，含 \r 就把规范化的副本写到缓存目录再跑，**原文件不动**。
     *
     * 两种情况都在常驻 root shell 里执行，脚本里的 read 依然可以从控制台输入行应答。
     */
    fun runScript(): Boolean {
        val content = _scriptText.value.trim()
        if (content.isEmpty()) return false

        viewModelScope.launch {
            // 探测/读写/落盘都是 IO，放子线程；只把最终命令串丢给会话
            val command = withContext(Dispatchers.IO) { buildRunCommand(content) }
            session.send(command)
        }
        return true
    }

    /** 在 IO 线程拼出最终要送进 shell 的命令 */
    private fun buildRunCommand(content: String): String {
        val scriptPath = resolveScriptPath(content)

        // ---------- 内联脚本：写临时文件后当文件跑 ----------
        if (scriptPath == null) {
            val temp = writeScriptToCache(INLINE_SCRIPT_FILE, content)
            return if (temp != null) {
                "echo '[脚本内容已写入临时文件后执行]'; sh " + shellQuote(temp)
            } else {
                // 连缓存都写不进去（几乎不可能）：退回逐行送 stdin
                content
            }
        }

        // ---------- 脚本路径 ----------
        // 无 CR 就直接跑原文件（$0、dirname 语义完全正确）
        if (!hasCrlf(scriptPath)) return "sh " + shellQuote(scriptPath)

        // 含 CRLF：规范化后的副本落缓存再跑，原文件保持不动
        val text = readAndNormalize(scriptPath)
            ?: return "sh " + shellQuote(scriptPath) // 读不到就照原样跑，让 shell 自己报错
        val normalized = writeScriptToCache(File(scriptPath).name, text)
            ?: return "sh " + shellQuote(scriptPath)
        return "echo '[检测到 Windows 换行(CRLF)，已用规范化副本执行]'; sh " + shellQuote(normalized)
    }

    /**
     * 判断输入框内容是不是"脚本文件路径"；是就返回**规整后的路径**，否则返回 null。
     *
     * 【为什么需要"规整"】
     * 从聊天软件 / 文档里复制路径时经常把换行一起带进来，例如框里显示成：
     *     /data/adb/yvyan/
     *     LinYuDriverLoader.Ver.7.4.sh
     * 这个换行并不是文件名的一部分。旧逻辑要求"整段只有一行"才算路径，
     * 一遇到换行就落到内联脚本分支 —— 于是这两行被当成两条命令执行，
     * `/data/adb/yvyan/` 那条报找不到文件，文件名那条报语法错误。
     *
     * 判定顺序（从最可信到最宽松）：
     *  ① **选择文件记住的路径最权威**：只要框里内容与它"去掉换行后"一致就用它。
     *     这样即便显示被换行扰动过，也照样能正确执行。
     *  ② 去掉所有换行后以 "/" 开头、且不含任何空白 → 就是被换行拆开的路径，用去换行的结果。
     *  ③ 去换行后以 "/" 开头但后面还跟了别的东西（粘贴时带了说明文字）：
     *     取第一个空白前那段，**且它必须像文件名**（末段含 "."，即有扩展名）才认，否则放过。
     *  ④ 本来就是单行、以 "/" 开头 → 原样使用（这一条允许路径里带空格）。
     *  其余情况返回 null，按内联脚本处理 —— 避免把真正的多行脚本误当成路径。
     */
    private fun resolveScriptPath(content: String): String? {
        val squashed = squashLineBreaks(content)

        // ① 选择文件时记住的权威路径
        val remembered = _pickedPath.value
        if (remembered != null && squashLineBreaks(remembered) == squashed) return remembered

        // ② 被换行拆开的路径
        if (squashed.startsWith("/") && squashed.none { it.isWhitespace() }) return squashed

        // ③ 路径后面跟了说明文字：只有第一段确实像文件名才认
        //    （放在 ④ 之前：单行带杂字时，"整段当路径"一定是错的，截取反而更可能对）
        val firstToken = squashed.substringBefore(' ').substringBefore('\t')
        if (firstToken.startsWith("/") && looksLikeFileName(firstToken)) return firstToken

        // ④ 本来就是单行路径（允许带空格）
        if (content.startsWith("/") && !containsLineBreak(content)) return content

        return null
    }

    /** 去掉所有换行（\r 与 \n）；用于把"被换行拆开的路径"拼回一整条 */
    private fun squashLineBreaks(text: String): String =
        text.replace("\r", "").replace("\n", "")

    private fun containsLineBreak(text: String): Boolean =
        text.contains('\n') || text.contains('\r')

    /** 粗略判断像不像文件名：末段含 "."（有扩展名），且不是 "." / ".." */
    private fun looksLikeFileName(token: String): Boolean {
        val lastSegment = token.substringAfterLast('/')
        return lastSegment.contains('.') && lastSegment != "." && lastSegment != ".."
    }

    /**
     * 只读文件开头一小段判断是否含 CR。
     * 不整文件读 —— 用户要求不限制文件大小，这里更要克制，64KB 足够发现换行风格。
     * 读不到（App 无存储权限）时返回 false，交给 sh 自己报错。
     */
    private fun hasCrlf(path: String): Boolean = runCatching {
        File(path).inputStream().use { input ->
            val buffer = ByteArray(CRLF_PROBE_BYTES)
            val read = input.read(buffer)
            (0 until maxOf(read, 0)).any { buffer[it] == '\r'.code.toByte() }
        }
    }.getOrDefault(false)

    /** 读全文件并把 CRLF / CR 统一成 LF；读不到返回 null */
    private fun readAndNormalize(path: String): String? = runCatching {
        File(path).readBytes().toString(Charsets.UTF_8)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
    }.getOrNull()

    /**
     * 把脚本文本写进 App 缓存目录，返回绝对路径。
     * 放在 cacheDir 而不是用户目录，避免往用户文件夹里丢垃圾；
     * 运行它的 root shell 有权限读取 App 私有目录。
     */
    private fun writeScriptToCache(fileName: String, text: String): String? = runCatching {
        val dir = File(getApplication<Application>().cacheDir, "scripts").apply { mkdirs() }
        val file = File(dir, fileName.ifEmpty { INLINE_SCRIPT_FILE })
        file.writeText(text, Charsets.UTF_8)
        file.absolutePath
    }.getOrNull()

    /** 路径用单引号包住，避免空格被拆成多个参数；路径内的单引号按 shell 规则转义 */
    private fun shellQuote(raw: String): String =
        "'" + raw.replace("'", "'\\''") + "'"

    // ================================================================
    //  终端输入
    // ================================================================

    fun updateInput(text: String) {
        _input.value = text
    }

    /** 提交输入行：既可以是新命令，也可以是对脚本交互提示的应答（如 y / n） */
    fun submitInput(): Boolean {
        val cmd = _input.value.trim()
        if (cmd.isEmpty()) return false

        _history.update { it + cmd }
        historyIndex = _history.value.size   // 游标复位到末尾，下次「↑」从最后一条开始翻
        _input.value = ""                    // 清空输入行，方便连续敲下一条
        session.send(cmd)
        return true
    }

    /** 点「↑」时回填上一条历史命令；翻到头就停住不动 */
    fun previousCommand(): Boolean {
        val list = _history.value
        if (list.isEmpty()) return false
        if (historyIndex > 0) historyIndex--
        _input.value = list[historyIndex]
        return true
    }

    /** 清空控制台输出 */
    fun clearOutput() {
        session.resetLineStart()
        _output.value = emptyList()
    }

    // ================================================================
    //  内部
    // ================================================================

    /** 追加一段输出（可能来自任意子线程） */
    private fun appendOutput(text: String) {
        _output.update { current ->
            val next = current + text
            // 超出上限时丢最老的片段，避免长时间运行把内存吃满
            if (next.size > MAX_CHUNKS) next.takeLast(MAX_CHUNKS) else next
        }
    }

    override fun onCleared() {
        super.onCleared()
        session.shutdown()
    }
}
