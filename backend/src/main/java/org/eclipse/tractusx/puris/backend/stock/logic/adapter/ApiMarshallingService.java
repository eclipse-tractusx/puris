package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentErrorDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageHeaderDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockResponseDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.Position;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * A class that marshalls and unmarshalls product stock
 * requests.
 */
@Component
@Slf4j
public class ApiMarshallingService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ModelMapper modelMapper;

    /**
     * This method transforms a ProductStockRequestDto into a String object that
     * contains a JSON in accordance with the api definition that can be sent
     * to the other party.
     * @param productStockRequestDto a request you want to send
     * @return a String carrying the information from the input object
     */
    public String transformProductStockRequest(ProductStockRequestDto productStockRequestDto) {
        var objectNode = objectMapper.createObjectNode();
        objectNode.set("header", objectMapper.convertValue(productStockRequestDto.getHeader(), JsonNode.class));
        var productStockArray = objectMapper.createArrayNode();
        for (var item : productStockRequestDto.getPayload()) {
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
            for (var item : jsonNode.get("content").get("productStock") ) {
                try {
                    ProductStockRequestForMaterialDto itemDto = objectMapper.readValue(item.toString(), ProductStockRequestForMaterialDto.class);
                    productStockRequestDto.getPayload().add(itemDto);
                } catch (Exception e) {
                    log.error("Failed to unmarshall list item: \n" + item.toPrettyString());
                }
            }
            return productStockRequestDto;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Unmarshalling of object failed: \n" +jsonData);
        }
    }

    /**
     * This method transforms a ProductStockResponseDto into a String object that
     * contains a JSON in accordance with the api definition that can be sent to the
     * other party.
     * @param responseDto a response you received
     * @return a String carrying the information from the input object
     */
    public String transformProductStockResponse(ProductStockResponseDto responseDto) {
        var objectNode = objectMapper.createObjectNode();
        objectNode.set("header", objectMapper.convertValue(responseDto.getHeader(), JsonNode.class));
        var productStockArray = objectMapper.createArrayNode();
        for (var item : responseDto.getPayload()) {
            productStockArray.addPOJO(item);
        }
        var contentObject = objectMapper.createObjectNode();
        contentObject.set("productStock", productStockArray);
        objectNode.set("content", contentObject);
        return objectNode.toString();
    }

    /**
     * This method transforms a JsonNode received from a customer Partner
     * and transforms it into a ProductStockResponseDto
     * @param jsonData body from a customer message to the response api
     * @return a ProductStockResponseDto carrying the same information
     */
    public ProductStockResponseDto transformToProductStockResponseDto(String jsonData) {
        try {
            JsonNode jsonNode = objectMapper.readValue(jsonData, JsonNode.class);
            ProductStockResponseDto responseDto = new ProductStockResponseDto();
            responseDto.setHeader(objectMapper.convertValue(jsonNode.get("header"), MessageHeaderDto.class));
            boolean malformedMessage = responseDto.getHeader().getRequestId() == null || responseDto.getHeader().getSender() == null;
            if (malformedMessage) {
                throw new RuntimeException("MALFORMED MESSAGE");
            }
            for (var item : jsonNode.get("content").get("productStock")) {
                try {
                    MessageContentErrorDto errorDto = objectMapper.readValue(item.toString(), MessageContentErrorDto.class);
                    responseDto.getPayload().add(errorDto);
                } catch (Exception e) {
                    try {
                        ProductStockSammDto sammDto = objectMapper.readValue(item.toString(), ProductStockSammDto.class);
                        responseDto.getPayload().add(sammDto);
                    } catch (Exception e1) {
                        // Handle empty optionals manually
                        try {
                            Collection<Position> positionCollection = new ArrayList<>();
                            var positionArray = item.get("positions");
                            for (var position : positionArray) {
                                Position positionDto = objectMapper.readValue(position.toString(), Position.class);
                                positionCollection.add(positionDto);
                            }
                            String materialNumberCustomer = item.get("materialNumberCustomer").asText();
                            String materialNumberSupplier = item.get("materialNumberSupplier").asText().length() == 0
                                ? null : item.get("materialNumberSupplier").asText();
                            String materialNumberCatenaX = item.get("materialNumberCatenaX").asText().length() == 0
                                ? null : item.get("materialNumberCatenaX").asText();
                            Optional<String> supplier = Optional.ofNullable(materialNumberSupplier);
                            Optional<String> catenaX = Optional.ofNullable(materialNumberCatenaX);
                            responseDto.getPayload().add(new ProductStockSammDto(positionCollection, materialNumberCustomer, catenaX, supplier));
                        } catch (Exception e2) {
                            log.error("Failed to unmarshall list item: \n" + item.toPrettyString());
                        }
                    }
                }
            }
            return responseDto;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Unmarshalling of object failed: \n" +jsonData);
        }
    }
}
