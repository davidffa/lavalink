rootProject.name = "Lavalink-Parent"

include(":Lavalink-Natives")
include(":Lavalink-Server")

project(":Lavalink-Natives").projectDir = File("$rootDir/LavalinkNatives")
project(":Lavalink-Server").projectDir = File("$rootDir/LavalinkServer")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("kotlin", "1.9.23")
      version("koe", "f2c8267")
      version("yt-asm", "1.4.0")
      version("spring", "3.3.1")
      version("prometheus", "0.16.0")

      version("gradleGit", "2.3.2")
      version("testLogger", "3.1.0")

      plugin("spring", "org.springframework.boot").versionRef("spring")
      plugin("gradlegitproperties", "com.gorylenko.gradle-git-properties").versionRef("gradleGit")
      plugin("grgit", "org.ajoberstar.grgit").version("4.1.1")
      plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
      plugin("kotlin-spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
      plugin("test-logger", "com.adarshr.test-logger").versionRef("testLogger")

      library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")

      library("koe-udpqueue", "com.github.davidffa.koe", "ext-udpqueue").versionRef("koe")
      library("koe-core", "com.github.davidffa.koe", "core").versionRef("koe")

      library("netty-epoll", "io.netty", "netty-transport-native-epoll").version("4.1.111.Final")
      library("netty-kqueue", "io.netty", "netty-transport-native-kqueue").version("4.1.111.Final")

      library("lavaplayer-main", "com.github.davidffa", "lavaplayer-fork").version("474b2a1")
      library("lavaplayer-yt", "dev.lavalink.youtube", "common").versionRef("yt-asm")
      library("lavaplayer-yt-thumbnail", "dev.lavalink.youtube", "v2").versionRef("yt-asm")
      library("lavaplayer-iprotator", "com.sedmelluq", "lavaplayer-ext-youtube-rotator").version("0.2.3")

      library("lavadsp", "com.github.davidffa", "lavadsp-fork").version("0.7.9")

      library("spring-ws", "org.springframework", "spring-websocket").version("6.1.10")
      library("spring-web", "org.springframework.boot", "spring-boot-starter-web").versionRef("spring")
      library("spring-undertow", "org.springframework.boot", "spring-boot-starter-undertow").versionRef("spring")

      library("logback", "ch.qos.logback", "logback-classic").version("1.5.6")
      library("sentry", "io.sentry", "sentry-logback").version("7.11.0")
      library("prometheus-client", "io.prometheus", "simpleclient").versionRef("prometheus")
      library("prometheus-hotspot", "io.prometheus", "simpleclient_hotspot").versionRef("prometheus")
      library("prometheus-logback", "io.prometheus", "simpleclient_logback").versionRef("prometheus")
      library("prometheus-servlet", "io.prometheus", "simpleclient_servlet").versionRef("prometheus")

      library("oshi", "com.github.oshi", "oshi-core").version("6.6.1")

      library("jsonorg", "org.json", "json").version("20240303")
      library("gson", "com.google.code.gson", "gson").version("2.11.0")

      // Test libs
      library("spotbugs-annotations", "com.github.spotbugs", "spotbugs-annotations").version("4.7.3")
      library("spring-test", "org.springframework.boot", "spring-boot-starter-test").versionRef("spring")

      // Build Script libs
      library("gradle-git", "com.gorylenko.gradle-git-properties", "gradle-git-properties").versionRef("gradleGit")
      library("spring-gradle", "org.springframework.boot", "spring-boot-gradle-plugin").versionRef("spring")
      library("sonarqube", "org.sonarsource.scanner.gradle", "sonarqube-gradle-plugin").version("3.3")
      library("kotlin-gradle", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
      library("kotlin-allopen", "org.jetbrains.kotlin", "kotlin-allopen").versionRef("kotlin")
      library("test-logger", "com.adarshr", "gradle-test-logger-plugin").versionRef("testLogger")
    }
  }
}