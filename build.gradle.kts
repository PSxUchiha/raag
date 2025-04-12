// Top-level build.gradle.kts
buildscript {
    extra.apply {
        set("compose_version", "1.5.4")
    }
}

plugins {
    id("com.android.application") version "8.9.1" apply false
    id("com.android.library") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
