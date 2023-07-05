package org.eclipse.tractusx.puris.backend.stock.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class DataPullController {

    @Value("${edc.dataplane.public.port}")
    String dataPlanePort;

    @Value("${edc.controlplane.host}")
    String dataPlaneHost;

    @Value("${edc.idsUrl}")
    String edcIdsUrl;

    @Autowired
    EdcAdapterService edcAdapterService;

    @RequestMapping("/requestapipull")
    ResponseEntity<String> requestapipull(@RequestBody JsonNode body){
        log.info("requestapipull called");
        log.info("\n" + body.toPrettyString());
        ObjectMapper mapper = new ObjectMapper();
        String endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
        log.info("Endpoint: " + endpoint);
        var authCode = body.get("authCode").asText();

        ObjectMapper objectMapper = new ObjectMapper();
        MessageHeaderDto mhd = new MessageHeaderDto();
        mhd.setRequestId(UUID.randomUUID());
        mhd.setCreationDate(new Date());
        mhd.setSender("BPNL4444444444XX"); // SOKRATES' BPNL
        mhd.setSenderEdc(edcIdsUrl);
        mhd.setRespondAssetId("product-stock-response-api");
        mhd.setUseCase(DT_UseCaseEnum.PURIS);
        mhd.setContractAgreementId("some cid");
        ArrayList<ProductStockRequestForMaterialDto> payload = new ArrayList<>();
        payload.add(new ProductStockRequestForMaterialDto("MNR-7307-AU340474.002", "", "MNR-8101-ID146955.001"));
        ProductStockRequestDto dto = new ProductStockRequestDto(DT_RequestStateEnum.WORKING, UUID.randomUUID(), mhd, null);
        dto.setHeader(mhd);
        dto.setPayload(payload);
        dto.setUuid(UUID.randomUUID());
        String dtoString = null;
        try {
            dtoString = objectMapper.writeValueAsString(dto);
        } catch (Exception e){
            log.info("serialization failed");
            return ResponseEntity.status(500).build();
        }

        log.info("\n");
        log.info("trying to pull data ...");
        var response = edcAdapterService.sendDataPullRequest(endpoint, authCode, dtoString);
        log.info("response received");
        try{
            var string = response.body().string();
            var stringObject =  mapper.readTree(string);
            log.info(stringObject.toPrettyString());
        } catch (Exception e){
            log.error("response failed", e);
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }



    @RequestMapping("/responseapipull")
    ResponseEntity<String> responseapipull(@RequestBody JsonNode body){
        log.info("responseapipull called");
        log.info("\n" + body.toPrettyString());
        ObjectMapper mapper = new ObjectMapper();
        String endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
        log.info("Endpoint: " + endpoint);
        // var authCode = body.get("authCode").asText();

        // ObjectMapper objectMapper = new ObjectMapper();
        // MessageHeaderDto mhd = new MessageHeaderDto();
        // mhd.setRequestId(UUID.randomUUID());
        // mhd.setCreationDate(new Date());
        // mhd.setSender("BPNL4444444444XX"); // SOKRATES' BPNL
        // mhd.setSenderEdc(edcIdsUrl);
        // mhd.setRespondAssetId("product-stock-request-api");
        // mhd.setUseCase(DT_UseCaseEnum.PURIS);
        // mhd.setContractAgreementId("some cid");
        // ArrayList<ProductStockRequestForMaterialDto> payload = new ArrayList<>();
        // payload.add(new ProductStockRequestForMaterialDto("MNR-7307-AU340474.002", "", "MNR-8101-ID146955.001"));
        // ProductStockRequestDto dto = new ProductStockRequestDto(DT_RequestStateEnum.WORKING, UUID.randomUUID(), mhd, null);
        // dto.setHeader(mhd);
        // dto.setPayload(payload);
        // dto.setUuid(UUID.randomUUID());
        // String dtoString = null;
        // try {
        //     dtoString = objectMapper.writeValueAsString(dto);
        // } catch (Exception e){
        //     log.info("serialization failed");
        //     return ResponseEntity.status(500).build();
        // }

        // log.info("\n");
        // log.info("trying to pull data ...");
        // var response = edcAdapterService.sendDataPullRequest(endpoint, authCode, dtoString);
        // log.info("response received");
        // try{
        //     var string = response.body().string();
        //     var stringObject =  mapper.readTree(string);
        //     log.info(stringObject.toPrettyString());
        // } catch (Exception e){
        //     log.error("response failed", e);
        //     return ResponseEntity.status(500).build();
        // }
        return ResponseEntity.ok().build();
    }
}