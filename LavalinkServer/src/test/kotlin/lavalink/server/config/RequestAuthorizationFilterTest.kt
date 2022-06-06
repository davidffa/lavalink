/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 */

package lavalink.server.config

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import lavalink.server.info.AppInfo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * Created by napster on 08.03.19.
 * - Edited by davidffa on 07.23.21.
 */
@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class RequestAuthorizationFilterTest(
  @Autowired val mvc: MockMvc,
  @Autowired val serverConfig: ServerConfig,
  @Autowired val appInfo: AppInfo
) {
  @Test
  @Throws(Exception::class)
  fun unauthenticatedRequest_Fail() {
    this.mvc.perform(get("/loadtracks")).andExpect(status().isUnauthorized)
  }

  @Test
  @Throws(Exception::class)
  fun wrongAuthenticatedRequest_Fail() {
    this.mvc
        .perform(
            get("/loadtracks")
                .header("Authorization", serverConfig.password + "foo"))
        .andExpect(status().isForbidden)
  }

  @Test
  @Throws(Exception::class)
  fun authenticatedRequest_Success() {
    this.mvc
        .perform(
            get("/version").header("Authorization", serverConfig.password))
        .andExpect(status().isOk)
        .andExpect(content().string(appInfo.getVersionBuild()))
  }
}
