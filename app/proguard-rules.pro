# ============================================================
#  R8 / ProGuard 规则
# ============================================================
#  当前 release 未开启代码压缩（isMinifyEnabled = false），本文件暂为占位。
#  若日后开启压缩，注意保留 Kotlin 元数据相关规则：
# ============================================================

# Kotlin 元数据（反射/序列化需要）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ViewModel 由反射创建，构造方法不能被裁掉
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
