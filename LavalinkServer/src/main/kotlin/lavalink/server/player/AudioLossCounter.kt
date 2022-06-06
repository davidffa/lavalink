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

package lavalink.server.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import org.slf4j.LoggerFactory

class AudioLossCounter : AudioEventAdapter() {
  companion object {
    val EXPECTED_PACKET_COUNT_PER_MIN = (60 * 1000) / 20 // 20ms packets
    private val log = LoggerFactory.getLogger(AudioLossCounter::class.java)
  }

  private val ACCEPTABLE_TRACK_SWITCH_TIME = 100 //ms

  private var curMinute = 0L
  private var curLoss = 0
  private var curSucc = 0

  var lastLoss = 0
  var lastSucc = 0

  private var playingSince = Long.MAX_VALUE
  private var lastTrackStarted = Long.MAX_VALUE / 2
  private var lastTrackEnded = Long.MAX_VALUE

  fun onLoss() {
    checkTime()
    curLoss++
  }

  fun onSuccess() {
    checkTime()
    curSucc++
  }

  fun isDataUsable(): Boolean {
    //log.info("\n" + lastTrackStarted + "\n" + lastTrackEnded + "\n" + playingSince)

    // Check that there isn't a significant gap in playback. If no track has ended yet, we can look past that
    if (lastTrackStarted - lastTrackEnded > ACCEPTABLE_TRACK_SWITCH_TIME
      && lastTrackEnded != Long.MAX_VALUE) return false

    // Check that we have at least stats for last minute
    val lastMin = System.currentTimeMillis() / 60000 - 1
    //log.info((playingSince < lastMin * 60000) + "")
    return playingSince < lastMin * 60000
  }

  private fun checkTime() {
    val actualMinute = System.currentTimeMillis() / 60000

    if (curMinute != actualMinute) {
      lastLoss = curLoss
      lastSucc = curSucc
      curLoss = 0
      curSucc = 0
      curMinute = actualMinute
    }
  }
  
  override fun onTrackEnd(audioPlayer: AudioPlayer?, audioTrack: AudioTrack?, endReason: AudioTrackEndReason?) {
    lastTrackEnded = System.currentTimeMillis()
  }

  override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
    lastTrackStarted = System.currentTimeMillis()

    if (lastTrackStarted - lastTrackEnded > ACCEPTABLE_TRACK_SWITCH_TIME
      || playingSince == Long.MAX_VALUE) {
      playingSince = System.currentTimeMillis()
      lastTrackEnded = Long.MAX_VALUE
    }
  }

  override fun onPlayerPause(player: AudioPlayer) {
    onTrackEnd(null, null, null)
  }

  override fun onPlayerResume(player: AudioPlayer) {
    onTrackStart(null, null)
  }

  override fun toString(): String {
    return "AudioLossCounter{" +
      "lastLoss=" + lastLoss +
      ", lastSucc=" + lastSucc +
      ", total=" + (lastSucc + lastLoss) +
      '}'
  }
}
