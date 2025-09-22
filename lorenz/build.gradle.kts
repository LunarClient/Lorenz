plugins {
    `java-library`
}

val bombeVersion: String by rootProject
val asmVersion: String by rootProject

dependencies {
    api("org.cadixdev:bombe:$bombeVersion")
    compileOnly("org.ow2.asm:asm-commons:${asmVersion}")
}
