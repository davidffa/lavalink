package lavalink.server.recorder

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder
import lavalink.server.natives.mp3.Mp3Encoder
import moe.kyokobot.koe.handler.AudioReceiveHandler
import moe.kyokobot.koe.internal.util.AudioPacket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class AudioReceiver(
  guildId: String,
  bitrate: Int
) : AudioReceiveHandler {
  companion object {
    const val FRAME_SIZE = 960
    const val BUFF_CAP = FRAME_SIZE * 2 * 2 // 2 channels with 960 samples each, in bytes
  }

  private val mp3Encoder = Mp3Encoder(48000, 2, bitrate)

  // ssrc <-> list of 20ms pcm buffers
  private val audioQueue = ConcurrentHashMap<Long, ConcurrentLinkedQueue<ShortBuffer>>()
  private val opusDecoders = HashMap<Long, OpusDecoder>()

  private val mixerExecutor = Executors.newSingleThreadScheduledExecutor()

  // ------------ TEMP --------------
  private val channel = FileChannel.open(Path("output.mp3"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)

  private val opusBuf = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())
  private val mixedAudioFrame = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())
  private val mp3Buf = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())

  init {
    mixerExecutor.scheduleAtFixedRate({
      mixedAudioFrame.clear()

      if (audioQueue.size > 0) {
        val currentFrames = audioQueue.map {
          val currFrame = it.value.poll()

          if (it.value.size == 0) audioQueue.remove(it.key)

          currFrame
        }

        for (i in 0 until BUFF_CAP / 2) {
          var sample = 0

          currentFrames.forEach {
            sample += it.get(i)
          }

          if (sample > Short.MAX_VALUE)
            mixedAudioFrame.putShort(Short.MAX_VALUE)
          else if (sample < Short.MIN_VALUE)
            mixedAudioFrame.putShort(Short.MIN_VALUE)
          else mixedAudioFrame.putShort(sample.toShort())
        }
      }

      mixedAudioFrame.flip()
      mp3Encoder.encode(mixedAudioFrame.asShortBuffer(), FRAME_SIZE, mp3Buf)
      channel.write(mp3Buf)
    }, 0, 20, TimeUnit.MILLISECONDS)
  }

  fun close() {
    mixerExecutor.shutdown()

    opusDecoders.values.forEach { it.close() }
    opusDecoders.clear()
    audioQueue.clear()

    // 7200 bytes recommended on lame.h#L868
    val finalMp3Frames = ByteBuffer.allocateDirect(7200)
      .order(ByteOrder.nativeOrder())

    mp3Encoder.flush(finalMp3Frames)
    mp3Encoder.close()

    channel.write(finalMp3Frames)
    channel.close()
  }

  override fun handleAudio(packet: AudioPacket) {
    val opus = packet.opusAudio
    val opusBuf = opusBuf
      .clear()
      .put(opus)
      .flip()

    // v Huge memory allocation
    val pcmBuf = ByteBuffer.allocateDirect(BUFF_CAP)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()

    getDecoder(packet.ssrc).decode(opusBuf, pcmBuf)
    getAudioQueue(packet.ssrc).add(pcmBuf)
  }

  private fun getDecoder(ssrc: Long) =
    opusDecoders.computeIfAbsent(ssrc) {
      OpusDecoder(48000, 2)
    }

  private fun getAudioQueue(ssrc: Long) =
    audioQueue.computeIfAbsent(ssrc) {
      ConcurrentLinkedQueue()
    }
}