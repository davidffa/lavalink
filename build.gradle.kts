/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.filters.ReplaceTokens
import org.ajoberstar.grgit.Grgit

buildscript {
  val kotlinVersion     = "1.6.21"
  val gradleGitVersion  = "2.3.2"
  val springBootVersion = "2.7.0"
  val sonarqubeVersion  = "3.3"
  val testLoggerVersion = "3.1.0"

  repositories {
    mavenLocal()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
    maven("https://jitpack.io")
  }

  dependencies {
    classpath("com.gorylenko.gradle-git-properties:gradle-git-properties:$gradleGitVersion")
    classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarqubeVersion")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    classpath("com.adarshr:gradle-test-logger-plugin:$testLoggerVersion")
  }
}

plugins {
  application
  idea
  id("org.springframework.boot") version "2.7.0"
  id("com.gorylenko.gradle-git-properties") version "2.3.2"
  id("org.ajoberstar.grgit") version "4.1.1"
  kotlin("jvm") version "1.6.21"
  kotlin("plugin.spring") version "1.6.21"
  id("com.adarshr.test-logger") version "3.1.0"
}

group = "lavalink"
description = "Play audio to discord voice channels"
version = versionFromTag()

application {
  mainClass.set("lavalink.server.Launcher")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

repositories {
  mavenCentral()
  jcenter()
  mavenLocal()
  maven("https://jitpack.io")
  maven("https://m2.dv8tion.net/releases")
}

val kotlinVersion              = "1.6.21"

val lavaplayerVersion          = "ff39f53"
val lavaplayerIpRotatorVersion = "0.2.3"
val nettyEpollVersion          = "4.1.77.Final"
val lavadspVersion             = "0.7.8"

val springBootVersion          = "2.7.0"
val springWebSocketVersion     = "5.3.20"
val prometheusVersion          = "0.15.0"
val koeVersion                 = "890ee59"
val logbackVersion             = "1.2.11"
val sentryVersion              = "5.7.4"
val oshiVersion                = "6.1.6"
val jsonOrgVersion             = "20220320"
val gsonVersion                = "2.9.0"
val spotbugsAnnotationsVersion = "4.6.0"

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

  // Audio Sending
  implementation("com.github.davidffa.koe:ext-udpqueue:$koeVersion") {
    exclude("com.sedmelluq", "lavaplayer")
    exclude("com.sedmelluq", "lava-common")
  }
  implementation("com.github.davidffa.koe:core:$koeVersion") {
    exclude("org.slf4j", "slf4j-api")
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Transport
  implementation("io.netty:netty-transport-native-epoll:$nettyEpollVersion:linux-x86_64")
  implementation("io.netty:netty-transport-native-kqueue:$nettyEpollVersion:osx-aarch_64")

  // Audio Player
  implementation("com.github.davidffa:lavaplayer-fork:$lavaplayerVersion")
  implementation("com.sedmelluq:lavaplayer-ext-youtube-rotator:$lavaplayerIpRotatorVersion") {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Filters
  implementation("com.github.davidffa:lavadsp-fork:$lavadspVersion") {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Spring
  implementation("org.springframework:spring-websocket:$springWebSocketVersion")
  implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("org.springframework.boot:spring-boot-starter-undertow:$springBootVersion")

  // Logging and Statistics
  implementation("ch.qos.logback:logback-classic:$logbackVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")
  implementation("io.prometheus:simpleclient:$prometheusVersion")
  implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
  implementation("io.prometheus:simpleclient_logback:$prometheusVersion")
  implementation("io.prometheus:simpleclient_servlet:$prometheusVersion")

  // Native System Stuff
  implementation("com.github.oshi:oshi-core:$oshiVersion")

  // Json
  implementation("org.json:json:$jsonOrgVersion")
  implementation("com.google.code.gson:gson:$gsonVersion")

  // Test stuff
  compileOnly("com.github.spotbugs:spotbugs-annotations:$spotbugsAnnotationsVersion")
  testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

tasks {
  bootJar {
    archiveFileName.set("Lavalink.jar")
  }

  bootRun {
    dependsOn(compileTestKotlin)
  }

  processResources {
    filesMatching("**/app.properties") {
      val tokens = mapOf(
        "project.version" to project.version,
        "project.groupId" to project.group,
        "project.artifactId" to project.name,
        "env.BUILD_NUMBER" to if (System.getenv("CI") != null) System.getenv("BUILD_NUMBER") else "Unofficial",
        "env.BUILD_TIME" to System.currentTimeMillis().toString()
      )

      filter<ReplaceTokens>("tokens" to tokens)
    }
  }

  build {
    doLast {
      println("Version: $version")
    }
  }

  test {
    useJUnitPlatform()
  }

  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
  }
}

fun versionFromTag(): String {
  val grgit = Grgit.open(mapOf("currentDir" to rootDir))

  val headTag = grgit.tag.list().find {
    it.commit.id == grgit.head().id
  }

  // Uncommitted changes? -> should be SNAPSHOT
  // Also watch out for false positives in the CI build
  val clean = grgit.status().isClean || System.getenv("CI") != null

  if (!clean) {
    println("Git state is dirty, setting version as snapshot")
  }

  return if (headTag != null && clean) {
    headTag.name
  } else {
    "${grgit.head().id}-SNAPSHOT"
  }
}

//create a simple version file that we will be reading to create appropriate docker tags

fun versionTxt() {
  File("$projectDir/VERSION.txt").appendText("$version\n")
}

versionTxt()