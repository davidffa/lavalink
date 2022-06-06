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

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import io.netty.buffer.ByteBuf
import lavalink.server.config.ServerConfig
import lavalink.server.io.SocketContext
import lavalink.server.io.SocketServer
import lavalink.server.player.filters.FilterChain
import moe.kyokobot.koe.MediaConnection
import moe.kyokobot.koe.media.OpusAudioFrameProvider
import org.json.JSONObject

import java.nio.ByteBuffer
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Player(
  val socketContext: SocketContext,
  val guildId: String,
  audioPlayerManager: AudioPlayerManager,
  private val serverConfig: ServerConfig
) : AudioEventAdapter() {
  private val player = audioPlayerManager.createPlayer()
  val audioLossCounter = AudioLossCounter()
  var realPosition: Double? = null

  /**
   * The player update interval.
   */
  private val interval: Int
  get() = serverConfig.playerUpdateInterval

  /**
   * The current audio track that is playing.
   */
  val playingTrack: AudioTrack?
  get() = player.playingTrack

  /**
   * Whether this player is playing something.
   */
  val isPlaying: Boolean
  get() = player.playingTrack != null && !player.isPaused

  private var myFuture: ScheduledFuture<*>? = null

  var filters: FilterChain? = null
    set(value) {
      field = value

      if (value!!.isEnabled) {
        player.setFilterFactory(value)
      } else {
        player.setFilterFactory(null)
      }
    }


  init {
    player.addListener(this)
    player.addListener(EventEmitter(audioPlayerManager, this))
    player.addListener(audioLossCounter)
  }

  fun getState(): JSONObject {
    val json = JSONObject()

    if (player.playingTrack != null)
      json.put("position", realPosition!!.toLong())
    json.put("time", System.currentTimeMillis())

    return json
  }

  /**
   * Destroys the AudioPlayer
   */
  fun destroy() = player.destroy()

  /**
   * Sets the pause state.
   * @param state The pause state.
   */
  fun setPause(state: Boolean) {
    player.isPaused = state
  }

  /**
   * Sets the volume of this player.
   * @param volume The volume to use.
   */
  fun setVolume(volume: Int) {
    player.volume = volume
  }

  /**
   * Plays an audio track.
   * @param track The track to play.
   */
  fun play(track: AudioTrack) {
    player.playTrack(track)
    SocketServer.sendPlayerUpdate(socketContext, this)
  }

  /**
   * Stops the currently playing track.
   */
  fun stop() = player.stopTrack()

  /**
   * Seek to the specified position in the current playing song.
   * @param position The position to seek to.
   */
  fun seekTo(position: Long) {
    val track = playingTrack ?: throw RuntimeException("Can't seek when not playing anything")

    track.position = position
    realPosition = position.toDouble()
  }

  override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
    myFuture?.cancel(false)
  }

  override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
    realPosition = track.position.toDouble()

    if (myFuture == null || myFuture!!.isCancelled) {
      myFuture = socketContext.playerUpdateService.scheduleAtFixedRate(Runnable {
        if (socketContext.sessionPaused) return@Runnable

        SocketServer.sendPlayerUpdate(socketContext, this)
      }, 0, this.interval.toLong(), TimeUnit.SECONDS)
    }
  }

  fun provideTo(connection: MediaConnection) {
    connection.audioSender = Provider(connection)
  }

  inner class Provider(
    connection: MediaConnection
  ) : OpusAudioFrameProvider(connection) {
    /**
     * The last frame that was sent.
     */
    private val lastFrame = MutableAudioFrame()

    init {
      lastFrame.setBuffer(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()))
    }

    override fun canProvide(): Boolean {
      val sent = player.provide(lastFrame)

      if (sent) {
        audioLossCounter.onSuccess()

        val speed = filters?.timescale?.speed ?: 1.0f
        val rate = filters?.timescale?.rate ?: 1.0f

        realPosition = realPosition?.plus(20 * speed * rate)
      } else {
        audioLossCounter.onLoss()
      }

      return sent
    }

    @Override
    override fun retrieveOpusFrame(buf: ByteBuf) {
      buf.writeBytes(lastFrame.data)
    }
  }

}
