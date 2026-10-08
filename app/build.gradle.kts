plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

// Gradle observes these command outputs, so the label refreshes on commit/dirty-state changes.
fun gitOutput(vararg args: String): String = runCatching {
    providers.exec {
        workingDir(rootProject.projectDir)
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("")
val gitCommit = gitOutput("rev-parse", "--short=8", "HEAD").ifBlank { "source archive" }
val gitRevision = gitCommit + if (gitCommit != "source archive" && gitOutput("status", "--porcelain").isNotBlank()) "-dirty" else ""

android {
    namespace = "io.github.iamtoolino.dayline"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.iamtoolino.dayline"
        minSdk = 30
        targetSdk = 36
        versionCode = 14
        versionName = "0.3.10"
        buildConfigField("String", "GIT_REVISION", "\"$gitRevision\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
