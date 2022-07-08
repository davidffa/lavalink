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

package lavalink.server.info

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.io.IOException
import java.util.Properties

/**
 * Created by napster on 25.06.18.
 * - Edited by davidffa on 07.05.21.
 * <p>
 * Requires app.properties to be populated with values during the gradle build
 */
@Component
class AppInfo {
  companion object {
    private val log = LoggerFactory.getLogger(AppInfo::class.java)
  }

  final val version: String
  private val groupId: String
  private val artifactId: String
  final val buildNumber: String
  final val buildTime: Long

  init {
    val resourceAsStream = this.javaClass.getResourceAsStream("/app.properties")
    val prop = Properties()

    try {
      prop.load(resourceAsStream)
    } catch (e: IOException) {
      log.error("Failed to load app.properties", e)
    }

    this.version = prop.getProperty("version")
    groupId = prop.getProperty("groupId")
    artifactId = prop.getProperty("artifactId")
    buildNumber = prop.getProperty("buildNumber")
    var bTime = -1L
    try {
      bTime = prop.getProperty("buildTime").toLong()
    } catch (ignored: NumberFormatException) {
    }
    this.buildTime = bTime
  }

  fun getVersionBuild(): String {
    return this.version + "_" + this.buildNumber
  }
}
