package com.example.shellconsole.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/**
 * ============================================================
 *  content:// URI → 文件真实绝对路径
 * ============================================================
 *  【为什么需要它】
 *  Android 的"选择文件"回调给的是 content:// URI，不是路径。
 *  但脚本要用 root 跑，就得拿到真实路径去做 `sh /path/to/x.sh` ——
 *  把内容读进输入框再逐行喂给 stdin 会破坏多行结构（for/if 块），
 *  交互式 `read` 也更容易错位。所以这里专门做一次路径还原。
 *
 *  【覆盖的几种来源】
 *    ① file://                     —— 部分第三方文件管理器直接返回路径，最省事
 *    ② com.android.externalstorage.documents
 *        系统「文件」选择器走这个，docId 形如 "primary:Download/a.sh"
 *        或 "1A2B-3C4D:dir/a.sh"（SD 卡），可由卷名拼回 /storage/...
 *    ③ com.android.providers.downloads.documents
 *        docId 是纯数字，用 downloads/public_downloads/<id> 查 _data 列
 *    ④ com.android.providers.media.documents
 *        docId 形如 "image:123"（查 MediaStore）或 "raw:/绝对路径"（直接可用）
 *    ⑤ 其它任意 provider  —— 统一尝试查 _data 列兜底
 *
 *  【失败是正常的】
 *  有些文件管理器用自己的私有 provider，不给 _data 也不给可解析的 docId，
 *  这时解析结果为 null，调用方应退回"读文件内容"的老路子，而不是报错。
 * ============================================================
 */
object ScriptPathResolver {

    private const val AUTHORITY_EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    private const val AUTHORITY_DOWNLOADS = "com.android.providers.downloads.documents"
    private const val AUTHORITY_MEDIA = "com.android.providers.media.documents"

    /** DocumentsProvider 的 docId 分隔符：卷名与相对路径之间 */
    private const val DOC_ID_SEPARATOR = ':'

    /** DownloadsProvider 的两个虚拟集合，docId 是数字时按 id 拼过去查 _data */
    private val DOWNLOADS_PUBLIC_URI = Uri.parse("content://downloads/public_downloads")
    private val DOWNLOADS_ALL_URI = Uri.parse("content://downloads/all_downloads")

    /**
     * 尽力还原 URI 指向文件的真实绝对路径。
     *
     * @return 绝对路径；无法还原时返回 null（调用方需自行兜底）
     */
    fun resolve(resolver: ContentResolver, uri: Uri): String? {
        when (uri.scheme) {
            // file:// 本身就是路径
            ContentResolver.SCHEME_FILE -> return normalize(uri.path)
            ContentResolver.SCHEME_CONTENT -> Unit
            else -> return null
        }

        val authority = uri.authority ?: return null

        // 按 provider 分派；都不是的话走最后的通用兜底
        val byProvider = when (authority) {
            AUTHORITY_EXTERNAL_STORAGE -> fromExternalStorage(uri)
            AUTHORITY_DOWNLOADS -> fromDownloads(resolver, uri)
            AUTHORITY_MEDIA -> fromMedia(resolver, uri)
            else -> null
        }
        if (!byProvider.isNullOrEmpty()) return normalize(byProvider)

        // 通用兜底：不少 provider 虽然没实现 DocumentsContract，但 Query 里带 _data 列
        return normalize(queryDataColumn(resolver, uri))
    }

    // ================================================================
    //  各家 provider 的还原逻辑
    // ================================================================

    /**
     * 外部存储：docId = "primary:Download/a.sh" 或 "1A2B-3C4D:dir/a.sh"。
     * 冒号前是卷名（primary = 内置存储，其余是 SD 卡 UUID），冒号后是相对路径。
     */
    @Suppress("DEPRECATION")
    private fun fromExternalStorage(uri: Uri): String? {
        val docId = documentIdOf(uri) ?: return null
        val separator = docId.indexOf(DOC_ID_SEPARATOR)
        if (separator <= 0) return null

        val volume = docId.substring(0, separator)
        val relative = docId.substring(separator + 1)
        if (relative.isEmpty()) return null

        val volumeRoot = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            // 可移动存储统一挂载在 /storage/<卷 UUID>
            "/storage/$volume"
        }
        return File(volumeRoot, relative).absolutePath
    }

    /**
     * 下载目录：docId 是纯数字（老版本）或 "msf:<数字>"（Android 10+ 常见）。
     * 数字部分取出来，依次试 downloads 的两个虚拟集合。
     */
    private fun fromDownloads(resolver: ContentResolver, uri: Uri): String? {
        val raw = documentIdOf(uri) ?: return null
        val id = raw.substringAfterLast(DOC_ID_SEPARATOR).toLongOrNull() ?: return null
        return queryDataColumn(resolver, ContentUris.withAppendedId(DOWNLOADS_PUBLIC_URI, id))
            ?: queryDataColumn(resolver, ContentUris.withAppendedId(DOWNLOADS_ALL_URI, id))
    }

    /** 媒体库：docId 形如 "raw:/绝对路径" 或 "image:123" */
    private fun fromMedia(resolver: ContentResolver, uri: Uri): String? {
        val docId = documentIdOf(uri) ?: return null
        val parts = docId.split(DOC_ID_SEPARATOR, limit = 2)
        if (parts.size != 2) return null

        // "raw:" 后面直接就是绝对路径，不用查库
        if (parts[0] == "raw") return parts[1]

        val id = parts[1].toLongOrNull() ?: return null
        val fileUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
        return queryDataColumn(resolver, fileUri)
    }

    // ================================================================
    //  工具
    // ================================================================

    private fun documentIdOf(uri: Uri): String? =
        runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()

    /** 查 provider 的 _data 列（老牌做法，很多文件管理器都支持） */
    private fun queryDataColumn(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (index < 0) null else cursor.getString(index)
        }
    }.getOrNull()

    /** 转成规范绝对路径；非绝对路径一律视为解析失败 */
    private fun normalize(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        return if (file.isAbsolute) file.absolutePath else null
    }
}
