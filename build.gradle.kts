plugins {
    id("org.jetbrains.intellij.platform") version "2.13.1"
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("java")
}

group = "com.miladnalbandi"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // ZPL rendering engine
    implementation("com.github.Milimarty:zpl-renderer:v1.0.0")

    // HTTP client for Labelary API fallback
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    intellijPlatform {
        phpstorm("2026.1")
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
    }

    signPlugin {
        certificateChainFile.set(file("chain.crt"))
        privateKeyFile.set(file("private.pem"))
        password.set(providers.environmentVariable("SIGN_PASSWORD").orElse(""))
    }

    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN").orElse(""))
    }
}
