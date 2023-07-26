package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A class that marshalls and unmarshalls product stock
 * requests.
 */
@Component
@Slf4j
public class RequestMarshallingService {

    @Autowired
    ObjectMapper objectMapper;

    /**
     * This method transforms a RequestDto into a String object that
     * contains a JSON in accordance with the api definition that can be sent
     * to the other party.
     * @param requestDto a request you want to send
     * @return a String carrying the information from the input object
     */
    public String transformRequest(RequestDto requestDto) {
        var objectNode = objectMapper.createObjectNode();
        objectNode.set("header", objectMapper.convertValue(requestDto.getHeader(), JsonNode.class));
        var productStockArray = objectMapper.createArrayNode();
        for (var item : requestDto.getPayload()) {
            productStockArray.addPOJO(item);
        }
        var contentObject = objectMapper.createObjectNode();
        contentObject.put("productStock", productStockArray);
        objectNode.put("content", contentObject);
        return objectNode.toString();
    }

    /**
     * This method transforms a JsonNode received from a customer Partner
     * and transforms it into a ProductStockRequestDto
     * @param jsonData body from a customer message to the request api
     * @return a ProductStockRequestDto carrying the same information
     */
    public ProductStockRequestDto transformToProductStockRequestDto(String jsonData) {
        try {
            JsonNode jsonNode = objectMapper.readValue(jsonData, JsonNode.class);
            ProductStockRequestDto productStockRequestDto = new ProductStockRequestDto();
            productStockRequestDto.setHeader(objectMapper.convertValue(jsonNode.get("header"), MessageHeaderDto.class));
            log.info("Header created");
            for (var item : jsonNode.get("content").get("productStock") ) {
                log.info("transforming item: \n" + item.toPrettyString());
                ProductStockRequestForMaterialDto itemDto = objectMapper.readValue(item.toString(), ProductStockRequestForMaterialDto.class);
                productStockRequestDto.getPayload().add(itemDto);
            }
            return productStockRequestDto;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Unmarshalling of object failed: \n" +jsonData);
        }
    }
}
