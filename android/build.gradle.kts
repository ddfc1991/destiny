plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // 缺这一行会导致 "Plugin [id: 'org.jetbrains.kotlin.plugin.serialization'] was not found"，
    // 整个 Kotlin 编译（含单测与 release 打包）全部起不来
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
