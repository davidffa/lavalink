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

package lavalink.server.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.filter.ThresholdFilter
import io.sentry.Sentry
import io.sentry.logback.SentryAppender
import lavalink.server.Launcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

import java.util.Properties

/**
 * Created by napster on 25.04.18.
 * - Edited by davidffa on 07.05.21.
 */

@Configuration
class SentryConfiguration(
  sentryConfig: SentryConfigProperties
) {
  private val log = LoggerFactory.getLogger(SentryConfiguration::class.java)
  private val SENTRY_APPENDER_NAME = "SENTRY"

  init {
    val dsn = sentryConfig.dsn

    if (dsn.isNotEmpty()) {
      turnOn(dsn, sentryConfig.tags, sentryConfig.environment)
    } else {
      turnOff()
    }
  }

  //programmatically creates a sentry appender
  @Synchronized
  private fun getSentryLogbackAppender(): SentryAppender {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)

    var sentryAppender = root.getAppender(SENTRY_APPENDER_NAME) as SentryAppender?
    if (sentryAppender == null) {
      sentryAppender = SentryAppender()
      sentryAppender.name = SENTRY_APPENDER_NAME

      val warningsOrAboveFilter = ThresholdFilter()
      warningsOrAboveFilter.setLevel(Level.WARN.levelStr)
      warningsOrAboveFilter.start()
      sentryAppender.addFilter(warningsOrAboveFilter)

      sentryAppender.context = loggerContext
      root.addAppender(sentryAppender)
    }
    return sentryAppender
  }

  private fun turnOn(dsn: String, tags: Map<String, String>?, environment: String) {
    log.info("Turning on sentry")

    Sentry.init {
      it.dsn = dsn

      if (environment.isNotBlank()) {
        it.environment = environment
      }

      if (tags != null && tags.isNotEmpty()) {
        tags.forEach(it::setTag)
      }

      // set the git commit hash this was build on as the release
      val gitProps = Properties()
      try {
        gitProps.load(Launcher::class.java.classLoader.getResourceAsStream("git.properties"))
      } catch (e: Exception) {
        log.error("Failed to load git repo information", e)
      }

      val commitHash = gitProps.getProperty("git.commit.id")
      if (commitHash != null && commitHash.isNotEmpty()) {
        log.info("Setting sentry release to commit hash {}", commitHash)
        it.release = commitHash
      } else {
        log.warn("No git commit hash found to set up sentry release")
      }
    }

    getSentryLogbackAppender().start()
  }

  private fun turnOff() {
    log.warn("Turning off sentry")
    Sentry.close()
    getSentryLogbackAppender().stop()
  }

}
