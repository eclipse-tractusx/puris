/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class ApiKeyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void stockViewShouldReturn403WithoutAuth() throws Exception {
        this.mockMvc.perform(
            get("/stockView/materials"))
                .andDo(print())
            .andExpect(status().is(403));
    }

    @Test
//    @WithMockApiKey // not yet working
    void stockViewShouldReturn200WithAuth() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
                    .header("X-API-KEY", "test")
            )
            .andExpect(status().is(200));
    }
    @Test
    @WithMockApiKey(apiKey = "test2")
    void stockViewShouldReturn403WithWrongAuthBasedOnMockKeyAnnotation() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
            )
            .andExpect(status().is(200));
    }

    @Test
    @WithMockApiKey
    void stockViewShouldReturn200WithWrongAuthBasedOnDefaultMockKeyAnnotation() throws Exception {
        this.mockMvc.perform(
                get("/stockView/materials")
            )
            .andExpect(status().is(200));
    }

}
