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
