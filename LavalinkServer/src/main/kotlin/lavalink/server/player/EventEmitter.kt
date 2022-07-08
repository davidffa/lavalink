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
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import lavalink.server.io.SocketServer
import lavalink.server.util.Util
import org.json.JSONObject
import org.slf4j.LoggerFactory

import java.io.IOException

class EventEmitter(
  private val audioPlayerManager: AudioPlayerManager,
  private val linkPlayer: Player
) : AudioEventAdapter() {
  companion object {
    private val log = LoggerFactory.getLogger(EventEmitter::class.java)
  }

  override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
    val out = JSONObject()
    out.put("op", "event")
    out.put("type", "TrackStartEvent")
    out.put("guildId", linkPlayer.guildId)

    try {
      out.put("track", Util.encodeAudioTrack(audioPlayerManager, track))
    } catch (e: IOException) {
      out.put("track", JSONObject.NULL)
    }

    linkPlayer.socketContext.send(out)
  }

  override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
    val out = JSONObject()
    out.put("op", "event")
    out.put("type", "TrackEndEvent")
    out.put("guildId", linkPlayer.guildId)

    try {
      out.put("track", Util.encodeAudioTrack(audioPlayerManager, track))
    } catch (e: IOException) {
      out.put("track", JSONObject.NULL)
    }

    out.put("reason", endReason.toString())

    linkPlayer.socketContext.send(out)
  }

  // These exceptions are already logged by Lavaplayer
  override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
    val out = JSONObject()
    out.put("op", "event")
    out.put("type", "TrackExceptionEvent")
    out.put("guildId", linkPlayer.guildId)

    try {
      out.put("track", Util.encodeAudioTrack(audioPlayerManager, track))
    } catch (e: IOException) {
      out.put("track", JSONObject.NULL)
    }

    out.put("error", exception.message)

    val exceptionJson = JSONObject()
      .put("message", exception.message)
      .put("severity", exception.severity.toString())
      .put("cause", Util.getRootCause(exception).toString())

    out.put("exception", exceptionJson)

    linkPlayer.socketContext.send(out)
  }

  override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
    log.warn(track.info.title + " got stuck! Threshold surpassed: " + thresholdMs)

    val out = JSONObject()
    out.put("op", "event")
    out.put("type", "TrackStuckEvent")
    out.put("guildId", linkPlayer.guildId)

    try {
      out.put("track", Util.encodeAudioTrack(audioPlayerManager, track))
    } catch (e: IOException) {
      out.put("track", JSONObject.NULL)
    }

    out.put("thresholdMs", thresholdMs)

    linkPlayer.socketContext.send(out)
    SocketServer.sendPlayerUpdate(linkPlayer.socketContext, linkPlayer)
  }

}
