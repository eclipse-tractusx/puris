package org.eclipse.tractusx.puris.backend.stock.controller;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockResponseDto;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ItemStockRequestMessageService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedMaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedProductItemStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("item-stock")
@Slf4j
public class ItemStockResponseApiController {
    @Autowired
    private ItemStockRequestApiService itemStockRequestApiService;
    @Autowired
    private ItemStockRequestMessageService itemStockRequestMessageService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;
    @Autowired
    private ReportedProductItemStockService reportedProductItemStockService;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private ItemStockSammMapper sammMapper;

    @PostMapping("response")
    public ResponseEntity<?> postMapping(@RequestBody ItemStockResponseDto responseDto){
        Partner partner = partnerService.findByBpnl(responseDto.getHeader().getSenderBpn());
        if(partner == null) {
            log.error("Unknown partner in response dto: \n" + responseDto);
            return ResponseEntity.badRequest().build();
        }
        var initialRequest = itemStockRequestMessageService.find(responseDto.getHeader().getRelatedMessageId());
        if(initialRequest == null) {
            log.error("Unknown requestMessage in response dto: \n" +responseDto);
            return ResponseEntity.badRequest().build();
        }
        HashSet<ReportedMaterialItemStock> oldReportedMaterialItemStocks = new HashSet<>();
        HashSet<ReportedMaterialItemStock> newReportedMaterialItemStocks = new HashSet<>();
        HashSet<ReportedProductItemStock> oldReportedProductItemStocks = new HashSet<>();
        HashSet<ReportedProductItemStock> newReportedProductItemStocks = new HashSet<>();
        for(var itemStockSamm : responseDto.getContent().getItemStock()) {
            switch (itemStockSamm.getDirection()) {
                case INBOUND -> {
                    // ReportedProductItemStock
                    var reportedProductItemStocks = sammMapper.itemStockSammToReportedProductItemStock(itemStockSamm, partner);
                    for(var reportedProductItemStock: reportedProductItemStocks) {
                        oldReportedProductItemStocks.addAll(reportedProductItemStockService.findByPartnerAndMaterial(reportedProductItemStock.getPartner(), reportedProductItemStock.getMaterial()));
                        newReportedProductItemStocks.add(reportedProductItemStock);
                    }
                }
                case OUTBOUND -> {
                    // ReportedMaterialItemStock
                    var reportedMaterialItemStocks = sammMapper.itemStockSammToReportedMaterialItemStock(itemStockSamm, partner);
                }
                default -> {
                    log.error("Missing direction in Samm object: \n" + itemStockSamm);
                }
            }
        }
        return ResponseEntity.ok(new ReactionMessage(responseDto.getHeader().getMessageId()));
    }

    private record ReactionMessage(UUID messageId){
    }
}
