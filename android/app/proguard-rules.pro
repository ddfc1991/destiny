# kotlinx Serialization —— 模型全量保留，避免 R8 之后反序列化找不到 serializer
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**

-keep class com.destiny.agent.data.** { *; }
-keep class com.destiny.agent.core.** { *; }
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-dontwarn androidx.compose.**
