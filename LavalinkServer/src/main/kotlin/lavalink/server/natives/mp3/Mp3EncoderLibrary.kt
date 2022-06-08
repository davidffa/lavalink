package lavalink.server.natives.mp3

import com.sedmelluq.lava.common.natives.NativeLibraryLoader
import java.nio.ByteBuffer
import java.nio.ShortBuffer

object Mp3EncoderLibrary {
  init {
    NativeLibraryLoader.create(this::class.java, "mp3encoder").load()
  }

  external fun create(sampleRate: Int, channels: Int, bitrate: Int): Long

  external fun destroy(instance: Long)

  external fun flush(instance: Long, directOutput: ByteBuffer, outputCapacity: Int): Int

  external fun encode(instance: Long, directInput: ShortBuffer, frameSize: Int, directOutput: ByteBuffer, outputCapacity: Int): Int
}