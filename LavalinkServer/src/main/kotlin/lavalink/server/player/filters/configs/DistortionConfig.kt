/*
 *  Copyright (c) 2021 Dimensional Fun
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

package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.distortion.DistortionPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class DistortionConfig(
  private val sinOffset: Float = 0f,
  private val sinScale: Float = 1f,
  private val tanOffset: Float = 0f,
  private val tanScale: Float = 1f,
  private val cosOffset: Float = 0f,
  private val cosScale: Float = 1f,
  private val offset: Float = 0f,
  private val scale: Float = 1f
) : FilterConfig()  {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return DistortionPcmAudioFilter(output, format.channelCount)
      .setSinOffset(sinOffset)
      .setSinScale(sinScale)
      .setTanOffset(tanOffset)
      .setTanScale(tanScale)
      .setCosOffset(cosOffset)
      .setCosScale(cosScale)
      .setOffset(offset)
      .setScale(scale)
  }

  override fun isEnabled(): Boolean {
    return isSet(sinOffset, 0f) || isSet(sinScale, 1f)
            || isSet(tanOffset, 0f) || isSet(tanScale, 1f)
            || isSet(cosOffset, 0f) || isSet(cosScale, 1f)
            || isSet(offset, 0f) || isSet(scale, 1f)
  }
}
