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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.server.util.Util
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest
import java.io.IOException
import java.util.concurrent.CompletionStage

@RestController
class AudioLoaderRestHandler(
  private val audioPlayerManager: AudioPlayerManager
) {
  companion object {
    private val log = LoggerFactory.getLogger(AudioLoaderRestHandler::class.java)
  }
  
  private fun log(request: HttpServletRequest) {
    log.info("${request.method} ${request.servletPath}")
  }

  private fun trackToJSON(audioTrack: AudioTrack): JSONObject {
    val trackInfo = audioTrack.info

    return JSONObject()
      .put("title", trackInfo.title)
      .put("author", trackInfo.author)
      .put("length", trackInfo.length)
      .put("identifier", trackInfo.identifier)
      .put("uri", trackInfo.uri)
      .put("isStream", trackInfo.isStream)
      .put("isSeekable", audioTrack.isSeekable)
      .put("sourceName", audioTrack.sourceManager?.sourceName)
      .put("position", audioTrack.position)
      .put("thumbnail", trackInfo.artworkUrl)
  }

  private fun encodeLoadResult(result: LoadResult): JSONObject {
    val json = JSONObject()
    val playlist = JSONObject()
    val tracks = JSONArray()

    result.tracks.forEach {
      val obj = JSONObject()
      obj.put("info", trackToJSON(it))

      try {
        val encoded = Util.encodeAudioTrack(audioPlayerManager, it)
        obj.put("track", encoded)
        tracks.put(obj)
      } catch (e: IOException) {
        log.warn("Failed to encode a track ${it.identifier}, skipping", e)
      }
    }

    playlist.put("name", result.playlistName)
    playlist.put("selectedTrack", result.selectedTrack)

    json.put("playlistInfo", playlist)
    json.put("loadType", result.loadResultType)
    json.put("tracks", tracks)

    if (result.loadResultType == ResultStatus.LOAD_FAILED && result.exception != null) {
      val exception = JSONObject()
      exception.put("message", result.exception.localizedMessage)
      exception.put("severity", result.exception.severity.toString())

      json.put("exception", exception)
    }

    return json
  }

  @GetMapping(value = ["/loadtracks"], produces = ["application/json"])
  @ResponseBody
  fun getLoadTracks(
    @RequestParam identifier: String): CompletionStage<ResponseEntity<String>> {
    log.info("Got request to load for identifier \"${identifier}\"")

    return AudioLoader(audioPlayerManager).load(identifier)
      .thenApply(this::encodeLoadResult)
      .thenApply {
        ResponseEntity(it.toString(), HttpStatus.OK)
      }
  }

  @GetMapping(value = ["/decodetrack"], produces = ["application/json"])
  @ResponseBody
  @Throws(IOException::class)
  fun getDecodeTrack(request: HttpServletRequest, @RequestParam track: String): ResponseEntity<String> {
    log(request)

    val audioTrack = Util.decodeAudioTrack(audioPlayerManager, track)

    return ResponseEntity(trackToJSON(audioTrack).toString(), HttpStatus.OK)
  }

  @PostMapping(value = ["/decodetracks"], consumes = ["application/json"], produces = ["application/json"])
  @ResponseBody
  @Throws(IOException::class)
  fun postDecodeTracks(request: HttpServletRequest, @RequestBody body: String): ResponseEntity<String> {
    log(request)

    val requestJSON = JSONArray(body)
    val responseJSON = JSONArray()

    for (i in 0 until requestJSON.length()) {
      val track = requestJSON.getString(i)
      val audioTrack = Util.decodeAudioTrack(audioPlayerManager, track)

      val infoJSON = trackToJSON(audioTrack)
      val trackJSON = JSONObject()
        .put("track", track)
        .put("info", infoJSON)

      responseJSON.put(trackJSON)
    }

    return ResponseEntity(responseJSON.toString(), HttpStatus.OK)
  }
}
