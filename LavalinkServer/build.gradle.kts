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
  repositories {
    mavenLocal()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
    maven("https://jitpack.io")
  }

  dependencies {
    classpath(libs.gradle.git)
    classpath(libs.spring.gradle)
    classpath(libs.sonarqube)
    classpath(libs.kotlin.gradle)
    classpath(libs.kotlin.allopen)
    classpath(libs.test.logger)
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  idea
  alias(libs.plugins.spring)
  alias(libs.plugins.gradlegitproperties)
  alias(libs.plugins.grgit)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.test.logger)
}

description = "Play audio to discord voice channels"
version = versionFromTag()

application {
  mainClass.set("lavalink.server.Launcher")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  implementation(libs.kotlin.reflect)

  // Audio Sending
  implementation(libs.koe.udpqueue) {
    exclude("com.sedmelluq", "lavaplayer")
    exclude("com.sedmelluq", "lava-common")
  }
  implementation(libs.koe.core) {
    exclude("org.slf4j", "slf4j-api")
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Native Transport
  implementation(libs.netty.epoll)
  implementation(libs.netty.kqueue)

  // Audio Player
  implementation(libs.lavaplayer.main)
  implementation(libs.lavaplayer.iprotator) {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Filters
  implementation(libs.lavadsp) {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Spring
  implementation(libs.spring.ws)
  implementation(libs.spring.web) {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation(libs.spring.undertow)

  // Logging and Statistics
  implementation(libs.logback)
  implementation(libs.sentry)
  implementation(libs.prometheus.client)
  implementation(libs.prometheus.hotspot)
  implementation(libs.prometheus.logback)
  implementation(libs.prometheus.servlet)

  // Native System Stuff
  implementation(libs.oshi)

  // Json
  implementation(libs.jsonorg)
  implementation(libs.gson)

  // Test stuff
  compileOnly(libs.spotbugs.annotations)
  testImplementation(libs.spring.test)
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
        "env.BUILD_NUMBER" to "Unofficial",
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
    kotlinOptions.jvmTarget = "17"
  }
}

fun versionFromTag(): String {
  val grgit = Grgit.open(mapOf("currentDir" to rootDir))

  val headTag = grgit.tag.list().find {
    it.commit.id == grgit.head().id
  }

  // Uncommitted changes? -> should be SNAPSHOT
  val clean = grgit.status().isClean

  if (!clean) {
    println("Git state is dirty, setting version as snapshot")
  }

  return if (headTag != null && clean) {
    headTag.name
  } else {
    "${grgit.head().id}-SNAPSHOT"
  }
}
