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

package lavalink.server.io

import lavalink.server.Launcher
import lavalink.server.player.AudioLossCounter
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import oshi.SystemInfo

class StatsTask(
  private val context: SocketContext,
  private val socketServer: SocketServer
) : Runnable {
  companion object {
    val log: Logger = LoggerFactory.getLogger(StatsTask::class.java)
  }

  private val si = SystemInfo()
  private var uptime = 0.0
  private var cpuTime = 0.0

  /* stuff */
  private var lastSystemCpuLoadTicks: LongArray? = null

  override fun run() {
    try {
      sendStats()
    } catch (e: Exception) {
      log.error("Exception while sending stats", e)
    }
  }

  private fun sendStats() {
    if (context.sessionPaused) return

    val out = JSONObject()

    var playersTotal = 0
    var playersPlaying = 0

    socketServer.contexts.forEach {
      playersTotal += it.players.size
      playersPlaying += it.playingPlayers.size
    }

    out.put("op", "stats")
    out.put("players", playersTotal)
    out.put("playingPlayers", playersPlaying)
    out.put("uptime", System.currentTimeMillis() - Launcher.startTime)

    // In bytes
    val mem = JSONObject()
    mem.put("free", Runtime.getRuntime().freeMemory())
    mem.put("used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
    mem.put("allocated", Runtime.getRuntime().totalMemory())
    mem.put("reservable", Runtime.getRuntime().maxMemory())
    out.put("memory", mem)

    val cpu = JSONObject()
    var load = getProcessRecentCpuUsage()
    if (load.isInfinite()) {
      load = 0.0
    }

    cpu.put("cores", Runtime.getRuntime().availableProcessors())
    cpu.put("systemLoad", getSystemRecentCpuUsage())
    cpu.put("lavalinkLoad", load)

    out.put("cpu", cpu)

    var totalSent = 0
    var totalNulled = 0
    var players = 0

    for (player in context.playingPlayers) {
      val counter = player.audioLossCounter
      if (!counter.isDataUsable()) continue

      players++
      totalSent += counter.lastSucc
      totalNulled += counter.lastLoss
    }

    val totalDeficit = players * AudioLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (totalSent + totalNulled)

    // We can't divide by 0
    if (players != 0) {
      val frames = JSONObject()
      frames.put("sent", totalSent / players)
      frames.put("nulled", totalNulled / players)
      frames.put("deficit", totalDeficit / players)
      out.put("frameStats", frames)
    }

    context.send(out)
  }

  private fun getSystemRecentCpuUsage(): Double {
    val hal = si.hardware
    val processor = hal.processor

    if (lastSystemCpuLoadTicks == null) {
      lastSystemCpuLoadTicks = processor.systemCpuLoadTicks
    }

    return processor.getSystemCpuLoadBetweenTicks(lastSystemCpuLoadTicks)
  }

  private fun getProcessRecentCpuUsage(): Double {
    val output: Double
    val hal = si.hardware
    val os = si.operatingSystem
    val p = os.getProcess(os.processId)

    output = if (cpuTime != 0.0) {
      val uptimeDiff = p.upTime - uptime
      val cpuDiff = (p.kernelTime + p.userTime) - cpuTime
      cpuDiff / uptimeDiff
    } else {
      ((p.kernelTime + p.userTime).toDouble()) / (p.userTime.toDouble())
    }

    // Record for next invocation
    uptime = p.upTime.toDouble()
    cpuTime = (p.kernelTime + p.userTime).toDouble()
    return output / hal.processor.logicalProcessorCount
  }

}
