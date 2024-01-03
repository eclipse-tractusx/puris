package org.eclipse.tractusx.puris.backend.stock.controller;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("item-stock")
@Slf4j
public class ItemStockRequestApiController {

    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;
    @Autowired
    private ExecutorService executorService;

    @PostMapping("request")
    public ResponseEntity<?> postMapping(@RequestBody ItemStockRequestMessageDto request) {
        int status = 200;
        switch (request.getContent().getDirection()) {
            case INBOUND -> executorService.submit(() -> itemStockRequestApiService.handleRequestFromCustomer(request));
            case OUTBOUND ->
                executorService.submit(() -> itemStockRequestApiService.handleRequestFromSupplier(request));
            default -> {
                log.warn("Missing direction in request \n" + request);
                status = 400;
            }
        }
        return ResponseEntity.status(status).body(new ReactionMessageDto(request.getHeader().getMessageId()));
    }

    private static record ReactionMessageDto(UUID messageId) {
    }


}
