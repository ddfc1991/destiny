# =====================================================================
# Destiny 原生包 · R8 混淆/压缩规则
# 开启方式：gradle assembleRelease -Pdestiny.shrink=true
# =====================================================================

# ---------- 通用注解保留 ----------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault, Exceptions
-keepattributes SourceFile, LineNumberTable

# ---------- kotlinx.serialization ----------
# 反射式生成的 serializer 会被 R8 当成无用代码裁掉，
# 导致运行期 decodeFromString 抛 "Serializer for class ... not found"。
# 这里同时保留类本体、companion serializer 字段和 $$serializer 内部类。
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.serialization.json.**

-keep,includedescriptorclasses class com.destiny.agent.data.** { *; }
-keep,includedescriptorclasses class com.destiny.agent.core.** { *; }

-keepclassmembers class com.destiny.agent.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.destiny.agent.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 枚举在序列化时按名称匹配，混淆名字会导致反序列化失败
-keepclassmembers enum com.destiny.agent.data.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Compose ----------
# Compose 自带 consumer rules，这里只兜底关掉缺失类的告警。
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.runtime.** { *; }

# ---------- Kotlin 元数据 ----------
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata { *; }

# ---------- 崩溃栈可读性 ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
