package lavalink.server.recorder

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder
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
  private val guildId: String,
) : AudioReceiveHandler {
  companion object {
    const val BUFF_CAP = 960 * 2 * 2 // 2 channels with 960 samples each, in bytes
  }

  // ssrc <-> list of 20ms pcm buffers
  private val audioQueue = ConcurrentHashMap<Long, ConcurrentLinkedQueue<ShortBuffer>>()
  private val opusDecoders = HashMap<Long, OpusDecoder>()
  private val tempOpusBuffers = HashMap<Long, ByteBuffer>()
  private val mixerExecutor = Executors.newSingleThreadScheduledExecutor()

  // ------------ TEMP --------------
  private val channel = FileChannel.open(Path("output.pcm"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)

  private val mixedAudioFrame = ByteBuffer.allocate(BUFF_CAP)

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
      channel.write(mixedAudioFrame)
    }, 0, 20, TimeUnit.MILLISECONDS)
  }

  fun close() {
    mixerExecutor.shutdown()

    opusDecoders.values.forEach { it.close() }
    opusDecoders.clear()
    audioQueue.clear()
    channel.close()
  }

  override fun handleAudio(packet: AudioPacket) {
    val opus = packet.opusAudio
    val opusBuf = getTempOpusBuf(packet.ssrc)
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

  private fun getTempOpusBuf(ssrc: Long) =
    tempOpusBuffers.computeIfAbsent(ssrc) {
      ByteBuffer.allocateDirect(BUFF_CAP)
        .order(ByteOrder.nativeOrder())
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