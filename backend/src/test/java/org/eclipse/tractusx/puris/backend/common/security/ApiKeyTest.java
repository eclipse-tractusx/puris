package org.eclipse.tractusx.puris.backend.common.security;

import org.eclipse.tractusx.puris.backend.AssetCreatorCommandLineRunner;
import org.eclipse.tractusx.puris.backend.DataInjectionCommandLineRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class ApiKeyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetCreatorCommandLineRunner assetCreatorCommandLineRunner;

    @MockBean
    private DataInjectionCommandLineRunner dataInjectionCommandLineRunner;

    @Test
    void stockViewShouldReturn403WithoutAuth() throws Exception {
        this.mockMvc.perform(
            get("/catena/stockView/materials"))
                .andDo(print())
            .andExpect(status().is(403));
    }

    @Test
    @WithMockApiKey
    void stockViewShouldReturn200WithAuth() throws Exception {
        this.mockMvc.perform(
                get("/catena/stockView/materials")
                    .header("X-API-KEY", "test"))
            //.andDo(print())
            .andExpect(status().is(200));
        // WTF??
    }

}
