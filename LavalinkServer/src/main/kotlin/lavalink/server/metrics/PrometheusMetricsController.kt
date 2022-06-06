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

package lavalink.server.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import java.io.IOException
import java.io.StringWriter

/**
 * Created by napster on 18.10.17. - Copied from Quarterdeck on 20.05.2018
 * - Edited by davidffa on 07.05.21.
 * <p>
 * Used to expose the prometheus metrics. Some code copied from prometheus' own MetricsServlet
 */
@RestController
@RequestMapping("\${metrics.prometheus.endpoint:/metrics}")
@ConditionalOnBean(PrometheusMetrics::class)
class PrometheusMetricsController {
  private val registry = CollectorRegistry.defaultRegistry

  @GetMapping(produces = [TextFormat.CONTENT_TYPE_004])
  @Throws(IOException::class)
  fun getMetrics(@RequestParam(name = "name[]", required = false) includedParam: Array<String>?):
  ResponseEntity<String> {
    return buildAnswer(includedParam)
  }

  @Throws(IOException::class)
  private fun  buildAnswer(includedParam: Array<String>?): ResponseEntity<String> {
    val params: Set<String> = if (includedParam == null) {
      setOf()
    } else {
      hashSetOf(*includedParam)
    }

    val writer = StringWriter()
    writer.use {
      TextFormat.write004(it, this.registry.filteredMetricFamilySamples(params))
      it.flush()
    }

    return ResponseEntity<String>(writer.toString(), HttpStatus.OK)
  }
}
