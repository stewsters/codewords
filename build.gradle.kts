plugins {
    kotlin("jvm") version "2.2.20"
}

group = "com.stewsters"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.stewsters:kaiju:1.7")

    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")

    implementation("org.nd4j:nd4j-native:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-native:1.0.0-M2:linux-x86_64-compat")

    implementation("org.deeplearning4j:deeplearning4j-nlp:1.0.0-M2.1")
//    implementation("org.deeplearning4j:deeplearning4j-clustering:1.0.0-M2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}