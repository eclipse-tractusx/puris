package org.eclipse.tractusx.puris.backend.masterdata.controller;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.PartTypeInformationSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("parttype")
@Slf4j
public class PartTypeInformationController {
    static Pattern bpnlPattern = PatternStore.BPNL_PATTERN;
    static Pattern materialNumberPattern = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN;

    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private PartTypeInformationSammMapper sammMapper;

    @GetMapping("/{bpnl}/{materialnumber}")
    public ResponseEntity<?> getMapping(@PathVariable String bpnl, @PathVariable String materialnumber) {
        if (!bpnlPattern.matcher(bpnl).matches() || !materialNumberPattern.matcher(materialnumber).matches()) {
            return ResponseEntity.badRequest().build();
        }
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            return ResponseEntity.status(401).build();
        }
        log.info(bpnl + " requests part type information on " + materialnumber);
        Material material = materialService.findByOwnMaterialNumber(materialnumber);
        if (material == null || !material.isProductFlag()) {
            return ResponseEntity.status(404).build();
        }
        var mpr = mprService.find(material, partner);
        if (mpr == null || !mpr.isPartnerBuysMaterial()) {
            return ResponseEntity.status(404).build();
        }
        var samm = sammMapper.productToSamm(material);
        return ResponseEntity.ok(samm);
    }
}
