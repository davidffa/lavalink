package lavalink.server.recorder

import lavalink.server.natives.mp3.Mp3Encoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class Mp3AudioProcessor(
  sampleRate: Int,
  private val channels: Int,
  bitrate: Int,
  fileName: String
) : AudioProcessor {
  companion object {
    fun calcUnsafeFrameSize(bitrate: Int, sampleRate: Int) = 144 * bitrate / sampleRate
    // lame.h#L701
    fun calcSafeFrameSize(bitrate: Int, sampleRate: Int)
            = AudioReceiver.FRAME_SIZE * (bitrate / 8) / sampleRate + 4 * 1152 * (bitrate / 8) / sampleRate + 512
  }

  override val outputChannel: FileChannel = FileChannel.open(
    Path("$fileName.mp3"),
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE
  )

  private val encoder = Mp3Encoder(sampleRate, channels, bitrate)

  private val tempBuf = ByteBuffer.allocateDirect(calcSafeFrameSize(bitrate, sampleRate))
    .order(ByteOrder.nativeOrder())
  private val silenceBuf = ByteBuffer.allocateDirect(calcUnsafeFrameSize(bitrate, sampleRate))
    .order(ByteOrder.nativeOrder())

  init {
    val pcmSilenceBuf = ByteBuffer.allocateDirect(AudioReceiver.FRAME_SIZE * channels * 2)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()

    if (channels == 1) {
      encoder.encodeMono(pcmSilenceBuf, AudioReceiver.FRAME_SIZE, silenceBuf)
    } else {
      encoder.encodeStereo(pcmSilenceBuf, AudioReceiver.FRAME_SIZE, silenceBuf)
    }
  }

  override fun process(input: ByteBuffer?) {
    if (input == null) {
      outputChannel.write(silenceBuf)
      silenceBuf.rewind()
    } else {
      if (channels == 1) {
        encoder.encodeMono(input.asShortBuffer(), AudioReceiver.FRAME_SIZE, tempBuf)
      } else {
        encoder.encodeStereo(input.asShortBuffer(), AudioReceiver.FRAME_SIZE, tempBuf)
      }

      outputChannel.write(tempBuf)
    }
  }

  override fun close() {
    // 7200 bytes recommended on lame.h#L868
    val finalMp3Frames = ByteBuffer.allocateDirect(7200)
      .order(ByteOrder.nativeOrder())

    encoder.flush(finalMp3Frames)
    encoder.close()

    outputChannel.write(finalMp3Frames)
    outputChannel.close()
  }
}