package lavalink.server.recorder

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder
import lavalink.server.natives.mp3.Mp3Encoder
import moe.kyokobot.koe.handler.AudioReceiveHandler
import moe.kyokobot.koe.internal.util.AudioPacket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class AudioReceiver(
  private val guildId: String,
  private val id: String,
  bitrate: Int
) : AudioReceiveHandler {
  companion object {
    const val FRAME_SIZE = 960
    const val BUFF_CAP = FRAME_SIZE * 2 * 2 // 2 channels with 960 samples each, in bytes
  }

  // ssrc <-> list of 20ms pcm buffers
  private val audioQueue = HashMap<Long, Queue<DecodedAudioPacket>>()
  private val opusDecoders = HashMap<Long, OpusDecoder>()

  private val mp3Encoder = Mp3Encoder(48000, 2, bitrate)
  private val mixerExecutor = Executors.newSingleThreadScheduledExecutor {
    val t = Thread(it, "$guildId - Mixer Thread")
    t.isDaemon = true
    t
  }

  private val mp3Buf = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())
  private val mixedAudioFrame = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())

  private lateinit var outputChannel: FileChannel

  private var finished = false

  fun start() {
    Files.createDirectories(Paths.get("./records"))
    outputChannel = FileChannel.open(Path("./records/record-$guildId-$id.mp3"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)

    mixerExecutor.scheduleAtFixedRate({
      mixedAudioFrame.clear()

      if (audioQueue.isNotEmpty()) {
        val now = System.currentTimeMillis()
        val currentFrames = LinkedList<DecodedAudioPacket>()

        audioQueue.forEach {
          var currFrame = it.value.poll()

          while (currFrame != null && now - currFrame.receivedTimestamp > 100) {
            currFrame = it.value.poll()
          }

          if (it.value.isEmpty()) audioQueue.remove(it.key)
          if (currFrame != null) currentFrames.push(currFrame)
        }

        for (i in 0 until BUFF_CAP / 2) {
          var sample = 0

          // Using conventional for loop instead of foreach prevents arraylist.iterator() calls,
          // that were allocating too much memory on the heap
          for (j in 0 until currentFrames.size) {
            sample += currentFrames[j].data.get(i)
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
      outputChannel.write(mp3Buf)
    }, 0, 20, TimeUnit.MILLISECONDS)
  }

  fun close() {
    finished = true
    mixerExecutor.shutdown()

    opusDecoders.values.forEach { it.close() }
    opusDecoders.clear()
    audioQueue.clear()

    // 7200 bytes recommended on lame.h#L868
    val finalMp3Frames = ByteBuffer.allocateDirect(7200)
      .order(ByteOrder.nativeOrder())

    mp3Encoder.flush(finalMp3Frames)
    mp3Encoder.close()

    outputChannel.write(finalMp3Frames)
    outputChannel.close()
  }

  override fun handleAudio(packet: AudioPacket) {
    if (finished) return
    val opusBuf = packet.opusAudio

    val pcmBuf = ByteBuffer.allocateDirect(BUFF_CAP)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()

    getDecoder(packet.ssrc).decode(opusBuf, pcmBuf)
    getAudioQueue(packet.ssrc).add(DecodedAudioPacket(pcmBuf, packet.receivedTimestamp))
  }

  private fun getDecoder(ssrc: Long) =
    opusDecoders.computeIfAbsent(ssrc) {
      OpusDecoder(48000, 2)
    }

  private fun getAudioQueue(ssrc: Long) =
    audioQueue.computeIfAbsent(ssrc) {
      ConcurrentLinkedQueue()
    }

  data class DecodedAudioPacket(
    val data: ShortBuffer,
    val receivedTimestamp: Long
  )
}