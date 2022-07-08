package lavalink.server.recorder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

interface AudioProcessor {
  /**
   * The output file channel
   */
  val outputChannel: FileChannel

  /**
   * Processes the PCM audio frame, encoding it if needed, and writing it into the output file
   */
  fun process(input: ByteBuffer?)

  /**
   * Closes the encoder
   */
  fun close()
}