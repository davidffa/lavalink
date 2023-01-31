package lavalink.server.recorder

import jakarta.servlet.http.HttpServletRequest
import org.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

@RestController
class RecorderRestHandler {
  companion object {
    private val log = LoggerFactory.getLogger(RecorderRestHandler::class.java)

    private val fileRegex = Regex("record-(.+)\\.(?:mp3|pcm)$")
  }

  private fun log(request: HttpServletRequest) {
    log.info("${request.method} ${request.servletPath}")
  }

  @GetMapping(value = ["/records/{guildId}/{id}"])
  fun downloadRecord(request: HttpServletRequest, @PathVariable guildId: String, @PathVariable id: String): ResponseEntity<ByteArrayResource?> {
    log(request)

    val fileBytes = try {
      Files.readAllBytes(Path("./records/$guildId/record-$id.mp3"))
    } catch (e: Exception) {
      try {
        Files.readAllBytes(Path("./records/$guildId/record-$id.pcm"))
      } catch (e: Exception) {
        null
      }
    } ?: return ResponseEntity(HttpStatus.NOT_FOUND)

    val file = ByteArrayResource(fileBytes)

    return ResponseEntity.ok()
      .contentLength(fileBytes.size.toLong())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(file)
  }

  @GetMapping(value = ["/records/{guildId}"], produces = ["application/json"])
  fun getAllRecords(request: HttpServletRequest, @PathVariable guildId: String): ResponseEntity<String> {
    log(request)

    val responseJSON = JSONArray()

    try {
      Files.walk(Path("./records/$guildId")).filter(Files::isRegularFile).forEach {
        val match = fileRegex.find(it.name) ?: return@forEach
        responseJSON.put(match.groupValues[1])
      }
    } catch (_: Exception) {}

    return ResponseEntity(responseJSON.toString(), HttpStatus.OK)
  }

  @DeleteMapping(value = ["/records/{guildId}"])
  fun deleteAllRecords(request: HttpServletRequest, @PathVariable guildId: String): ResponseEntity<Unit> {
    log(request)

    try {
      Files.walk(Path("./records/$guildId"))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete)
    } catch (_: Exception) {}

    return ResponseEntity(HttpStatus.OK)
  }

  @DeleteMapping(value = ["/records/{guildId}/{id}"])
  fun deleteRecord(request: HttpServletRequest, @PathVariable guildId: String, @PathVariable id: String): ResponseEntity<Unit> {
    log(request)

    try {
      Files.delete(Path("./records/$guildId/record-$id.mp3"))
    } catch (_: Exception) {
      try {
        Files.delete(Path("./records/$guildId/record-$id.pcm"))
      } catch (_: Exception) {}
    }

    return ResponseEntity(HttpStatus.OK)
  }
}