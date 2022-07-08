/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
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

package lavalink.server.player

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.slf4j.LoggerFactory

import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean

class AudioLoader(
  private val audioPlayerManager: AudioPlayerManager
) : AudioLoadResultHandler {
  companion object {
    private val log = LoggerFactory.getLogger(AudioLoader::class.java)
  }

  private val NO_MATCHES = LoadResult(ResultStatus.NO_MATCHES, Collections.emptyList(),
    null, null)

  private val loadResult = CompletableFuture<LoadResult>()
  private val used = AtomicBoolean(false)
  

  fun load(identifier: String): CompletionStage<LoadResult> {
    val isUsed = this.used.getAndSet(true)
    if (isUsed) {
      throw IllegalStateException("This loader can only be used once per instance")
    }

    log.trace("Loading item with identifier {}", identifier)
    this.audioPlayerManager.loadItem(identifier, this)

    return loadResult
  }

  override fun trackLoaded(audioTrack: AudioTrack) {
    log.info("Loaded track ${audioTrack.info.title}")
    val result = mutableListOf<AudioTrack>()
    result.add(audioTrack)
    this.loadResult.complete(LoadResult(ResultStatus.TRACK_LOADED, result, null, null))
  }

  override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
    log.info("Loaded playlist ${audioPlaylist.name}")

    var playlistName: String? = null
    var selectedTrack: Int? = null
    if (!audioPlaylist.isSearchResult) {
      playlistName = audioPlaylist.name
      selectedTrack = audioPlaylist.tracks.indexOf(audioPlaylist.selectedTrack)
    }

    val status = if (audioPlaylist.isSearchResult) ResultStatus.SEARCH_RESULT else ResultStatus.PLAYLIST_LOADED
    val loadedItems = audioPlaylist.tracks

    this.loadResult.complete(LoadResult(status, loadedItems, playlistName, selectedTrack))
  }

  override fun noMatches() {
    log.info("No matches found")
    this.loadResult.complete(NO_MATCHES)
  }

  override fun loadFailed(e: FriendlyException) {
    log.error("Track loading failed", e)
    this.loadResult.complete(LoadResult(e))
  }

}
