package lavalink.server.natives.mp3

import com.sedmelluq.lava.common.natives.NativeResourceHolder
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class Mp3Encoder(
  sampleRate: Int,
  channels: Int,
  bitrate: Int
) : NativeResourceHolder() {
  private val instance = Mp3EncoderLibrary.create(sampleRate, channels, bitrate)

  init {
    if (instance == 0L) {
      throw IllegalStateException("Failed to create an encoder instance")
    }
  }

  fun encode(directInput: ShortBuffer, frameSize: Int, directOutput: ByteBuffer): Int {
    checkNotReleased()
    if (!directInput.isDirect || !directOutput.isDirect) {
      throw IllegalArgumentException("Arguments must be direct buffers.")
    }

    directOutput.clear()
    val result = Mp3EncoderLibrary.encode(instance, directInput, frameSize, directOutput, directOutput.capacity())

    directOutput.position(result)
    directOutput.flip()

    return result
  }

  fun flush(directOutput: ByteBuffer): Int {
    checkNotReleased()

    if (!directOutput.isDirect) {
      throw IllegalArgumentException("Arguments must be direct buffers.")
    }

    directOutput.clear()
    val result = Mp3EncoderLibrary.flush(instance, directOutput, directOutput.capacity())

    if (result < 0) {
      throw IllegalStateException("Flush failed with error $result")
    }

    directOutput.position(result)
    directOutput.flip()

    return result
  }

  override fun freeResources() {
    Mp3EncoderLibrary.destroy(instance)
  }
}