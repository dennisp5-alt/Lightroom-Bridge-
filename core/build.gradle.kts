plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }

// Runs actual preset serialization through the JDK XML parser on every APK build.
tasks.register<JavaExec>("smokeXmp") {
    dependsOn(tasks.named("testClasses"))
    mainClass.set("com.dennis.lightroombridge.core.CoreSmokeTestKt")
    classpath = sourceSets["test"].runtimeClasspath
}
