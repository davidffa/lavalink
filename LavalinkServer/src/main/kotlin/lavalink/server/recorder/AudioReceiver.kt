package lavalink.server.recorder

import com.sedmelluq.discord.lavaplayer.natives.opus.OpusDecoder
import moe.kyokobot.koe.handler.AudioReceiveHandler
import moe.kyokobot.koe.internal.util.AudioPacket
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.nio.file.Files
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

class AudioReceiver(
  val guildId: String,
  val id: String,
  private val selfAudio: Boolean,
  private val users: Set<String>?,
  format: AudioFormat,
  private val channels: Int,
  bitrate: Int,
) : AudioReceiveHandler {
  companion object {
    const val FRAME_SIZE = 960 // (sample_rate / 1000) * frame_duration -> (48000 / 1000) * 20
    const val BUFF_CAP = FRAME_SIZE * 2 * 2 // 2 channels with 960 samples each, in bytes

    private val log: Logger = LoggerFactory.getLogger(AudioReceiver::class.java)
  }

  // ssrc <-> list of 20ms pcm buffers
  private val audioQueue = ConcurrentHashMap<Long, Queue<DecodedAudioPacket>>()
  private val opusDecoders = ConcurrentHashMap<Long, OpusDecoder>()

  private val mixerExecutor = Executors.newSingleThreadScheduledExecutor {
    val t = Thread(it, "$guildId - Mixer Thread")
    t.isDaemon = true
    t
  }

  // Lock for processing bot's audio in order (UdpQueueFramePoller polls 2 frames at once)
  private val botLock = Object()
  private val audioProcessorExecutor = Executors.newFixedThreadPool(2) {
    val t = Thread(it, "$guildId - Audio Processor Thread")
    t.isDaemon = true
    t
  }

  private val mixedAudioFrame = ByteBuffer.allocateDirect(BUFF_CAP)
    .order(ByteOrder.nativeOrder())

  private val processor: AudioProcessor

  private var started = false
  @Volatile
  private var finished = false

  init {
    log.debug("Setting up AudioReceiver for guild $guildId, with id: $id")

    Files.createDirectories(Path("./records/$guildId"))

    val fileName = "./records/$guildId/record-$id"

    processor = when (format) {
      AudioFormat.MP3 ->  Mp3AudioProcessor(48000, channels, bitrate, fileName)
      AudioFormat.PCM -> PcmAudioProcessor(channels, fileName)
    }
  }

  override fun users() = users

  fun start() {
    if (started) return
    started = true

    log.info("Starting AudioReceiver for guild $guildId, with id: $id")

    mixerExecutor.scheduleAtFixedRate({
      if (audioQueue.isNotEmpty()) {
        mixedAudioFrame.clear()

        val now = System.currentTimeMillis()
        val currentFrames = LinkedList<DecodedAudioPacket>()

        audioQueue.forEach {
          var currFrame: DecodedAudioPacket?

          if (it.key == -1L) {
            currFrame = it.value.peek()
            // Don't mix the bot's audio if it hasn't been sent yet (NAS queue)
            if (currFrame != null && currFrame.receivedTimestamp > now) return@forEach
          }

          currFrame = it.value.poll()

          while (it.key != -1L && currFrame != null && now - currFrame.receivedTimestamp > 100) {
            currFrame = it.value.poll()
          }

          if (it.value.isEmpty()) audioQueue.remove(it.key)
          if (currFrame != null) currentFrames.push(currFrame)
        }

        if (currentFrames.isEmpty()) {
          processor.process(null)
          return@scheduleAtFixedRate
        }

        for (i in 0 until currentFrames[0].data.capacity()) {
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

        mixedAudioFrame.flip()
        processor.process(mixedAudioFrame)
      } else {
        processor.process(null)
      }
    }, 0, 20, TimeUnit.MILLISECONDS)
  }

  fun close() {
    finished = true
    log.info("Shutting down AudioReceiver for guild $guildId, with id: $id")
    audioProcessorExecutor.shutdownNow()
    mixerExecutor.shutdownNow()

    opusDecoders.values.forEach { it.close() }
    opusDecoders.clear()
    audioQueue.clear()

    processor.close()
  }

  fun handleSelfAudio(opus: ByteArray, expectedSendTimestamp: Long) {
    if (finished || !selfAudio) return

    audioProcessorExecutor.execute {
      synchronized(botLock) {
        val opusBuf = ByteBuffer.allocateDirect(opus.size)
          .order(ByteOrder.nativeOrder())
          .put(opus)
          .flip()

        val pcmBuf = ByteBuffer.allocateDirect(BUFF_CAP)
          .order(ByteOrder.nativeOrder())
          .asShortBuffer()

        val decoder = getDecoder(-1L)

        synchronized(decoder) {
          try {
            decoder.decode(opusBuf, pcmBuf)
          } catch (ex: IllegalStateException) {
            log.error(ex.message)
          }
        }

        if (channels == 1) {
          val pcmMonoBuf = ByteBuffer.allocate(BUFF_CAP / 2)
            .asShortBuffer()

          for (i in 0 until BUFF_CAP / 2 step 2) {
            pcmMonoBuf.put(i / 2, ((pcmBuf.get(i) + pcmBuf.get(i + 1)) / 2).toShort())
          }

          getAudioQueue(-1L).add(DecodedAudioPacket(pcmMonoBuf, expectedSendTimestamp))
        } else {
          getAudioQueue(-1L).add(DecodedAudioPacket(pcmBuf, expectedSendTimestamp))
        }
      }
    }
  }

  override fun handleAudio(packet: AudioPacket) {
    if (finished) return
    val opusBuf = packet.opusAudio

    audioProcessorExecutor.execute {
      val pcmBuf = ByteBuffer.allocateDirect(BUFF_CAP)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()

      val decoder = getDecoder(packet.ssrc)

      synchronized(decoder) {
        try {
          decoder.decode(opusBuf, pcmBuf)
        } catch (ex: IllegalStateException) {
          log.error(ex.message)
        }
      }

      if (channels == 1) {
        val pcmMonoBuf = ByteBuffer.allocate(BUFF_CAP / 2)
          .asShortBuffer()

        for (i in 0 until BUFF_CAP / 2 step 2) {
          pcmMonoBuf.put(i / 2, ((pcmBuf.get(i) + pcmBuf.get(i + 1)) / 2).toShort())
        }

        getAudioQueue(packet.ssrc).add(DecodedAudioPacket(pcmMonoBuf, packet.receivedTimestamp))
      } else {
        getAudioQueue(packet.ssrc).add(DecodedAudioPacket(pcmBuf, packet.receivedTimestamp))
      }
    }
  }

  private fun getDecoder(ssrc: Long) =
    opusDecoders.computeIfAbsent(ssrc) {
      OpusDecoder(48000, 2)
    }

  private fun getAudioQueue(ssrc: Long) =
    audioQueue.computeIfAbsent(ssrc) {
      PriorityBlockingQueue()
    }

  data class DecodedAudioPacket(
    val data: ShortBuffer,
    val receivedTimestamp: Long
  ) : Comparable<DecodedAudioPacket> {
    override fun compareTo(other: DecodedAudioPacket) = (receivedTimestamp - other.receivedTimestamp).toInt()
  }
}