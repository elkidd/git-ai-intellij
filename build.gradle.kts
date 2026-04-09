plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.gitai.blame"
version = "0.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        instrumentationTools()
    }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.gitai.blame"
        name = "Git AI Blame"
        version = "0.2.0"
        description = "Shows git-ai blame annotations in the editor gutter, highlighting AI-authored lines with agent and model info."
        vendor {
            name = "ElKidd"
        }
        ideaVersion {
            sinceBuild = "243"
        }
    }
}
