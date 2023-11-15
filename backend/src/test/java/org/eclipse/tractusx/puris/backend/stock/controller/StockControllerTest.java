package org.eclipse.tractusx.puris.backend.stock.controller;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WebMvcTest(StockViewController.class)
class StockControllerTest {

//    @LocalServerPort
//    private int port;

//    @Autowired
//    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private ProductStockService productStockService;

    @MockBean
    private MaterialStockService materialStockService;

    @MockBean
    private PartnerProductStockService partnerProductStockService;

    @MockBean
    private ProductStockRequestApiService productStockRequestApiService;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private MaterialPartnerRelationService mprService;

    @MockBean
    private ModelMapper modelMapper;

    //@InjectMocks
    //@MockBean
    //StockController underTest;

//    @BeforeEach
//    void setUp() {
//    }
//
//    @AfterEach
//    void tearDown() {
//    }

    @Test
    void getMaterials() throws Exception{

        // given
        Material material1 = Material.builder()
            .ownMaterialNumber("MNR-4711")
            .materialFlag(true)
            .name("Test Material 1")
            .materialNumberCx("urn:uuid:ccfffbba-cfa0-49c4-bc9c-4e13d7a4ac7a")
            .build();
        Material material2 = Material.builder()
            .ownMaterialNumber("MNR-4712")
            .materialFlag(true)
            .name("Test Material 2")
            .build();
        List<Material> allMaterials = new ArrayList<>();
        allMaterials.add(material1);
        allMaterials.add(material2);
        when(materialService.findAllMaterials()).thenReturn(allMaterials);

        // 401 returned due to https://stackoverflow.com/questions/39554285/spring-test-returning-401-for-unsecured-urls
        // problem: we need security
        this.mockMvc.perform(
                get("/catena/stockView/materials")
            ).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // then
//        assertAll(
//            () -> assertNotNull(returnedMaterials),
//            () -> assertEquals(2, returnedMaterials.size())
//        );
//
//        FrontendMaterialDto returnedMaterial = returnedMaterials.stream().filter(
//            frontendMaterialDto -> frontendMaterialDto.getOwnMaterialNumber().equals("MNR-4711")
//        ).findFirst().get();
//        assertAll(
//            () -> assertEquals("MNR-4711", returnedMaterial.getOwnMaterialNumber()),
//            () -> assertEquals("Test Material 1", returnedMaterial.getDescription())
//        );


        //this.mockMvc.perform(get("/materials")).andDo(print()).andExpect(status().isOk())
        //    .andExpect(content().contentType().);
    }
//
//    @Test
//    void getMaterialNumbers() {
//    }
//
//    @Test
//    void getProducts() {
//    }
//
//    @Test
//    void getProductStocks() {
//    }
//
//    @Test
//    void createProductStocks() {
//    }
//
//    @Test
//    void updateProductStocks() {
//    }
//
//    @Test
//    void getMaterialStocks() {
//    }
//
//    @Test
//    void createMaterialStocks() {
//    }
//
//    @Test
//    void updateMaterialStocks() {
//    }
//
//    @Test
//    void getPartnerProductStocks() {
//    }
//
//    @Test
//    void getCustomerPartnersOrderingMaterial() {
//    }
//
//    @Test
//    void triggerPartnerProductStockUpdateForMaterial() {
//    }
}
