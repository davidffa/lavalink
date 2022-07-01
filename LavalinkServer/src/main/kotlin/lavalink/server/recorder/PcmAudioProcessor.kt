package lavalink.server.recorder

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class PcmAudioProcessor(
  channels: Int,
  fileName: String
) : AudioProcessor {
  override val outputChannel: FileChannel = FileChannel.open(
    Path("$fileName.pcm"),
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE
  )

  private val silenceBuf = ByteBuffer.allocate(AudioReceiver.FRAME_SIZE * channels * 2)

  override fun process(input: ByteBuffer?) {
    if (input == null) {
      outputChannel.write(silenceBuf)
      silenceBuf.rewind()
    } else {
      outputChannel.write(input)
    }
  }

  override fun close() {
    outputChannel.close()
  }
}