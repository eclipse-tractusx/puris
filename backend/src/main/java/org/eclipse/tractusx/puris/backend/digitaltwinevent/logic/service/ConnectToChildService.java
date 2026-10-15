/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated 
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Anthropic Claude (claude-opus-5-5)
 */
package org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageContext;
import org.eclipse.tractusx.puris.backend.common.industrycore.IndustryCoreMessageService;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.ParentData;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UniqueIdPushConnectTochildPayload;
import org.eclipse.tractusx.puris.backend.digitaltwinevent.logic.dto.UsageAsPlannedItem;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.ExternalMaterialRelation.ValidityPeriod;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.ExternalMaterialRelationRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConnectToChildService {
    private static final String INFORMATION = "Usage as Planned information for parts supplied by the receiver.";

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ExternalMaterialRelationRepository externalMaterialRelationRepository;

    @Autowired
    private IndustryCoreMessageService messageService;

    @Autowired
    private EdcAdapterService edcAdapterService;

    /**
     * Stores the parent items of a received ConnectToChild event as {@link ExternalMaterialRelation}s. A relation
     * that already exists for the same child material, partner and parent globalAssetId is updated.
     *
     * @param senderBpnl the BPNL of the partner who sent the event
     * @param payload    the content of the event, already validated against the OpenAPI specification
     * @throws NoSuchElementException   if a child material is unknown or not supplied to the sender
     * @throws IllegalArgumentException if a parent item does not belong to the sender or its validity period ends before it starts
     */
    @Transactional
    public void handleConnectToChild(String senderBpnl, UniqueIdPushConnectTochildPayload payload) {
        Partner sender = partnerService.findByBpnl(senderBpnl);
        List<ExternalMaterialRelation> relations = new ArrayList<>();
        for (UsageAsPlannedItem item : payload.getListOfItems()) {
            Material child = findMaterialSuppliedTo(item.getCatenaXId(), sender, senderBpnl);
            for (ParentData parent : item.getParentItems()) {
                relations.add(toRelation(child, sender, parent));
            }
        }
        relations.forEach(this::createOrUpdate);
    }

    /**
     * Informs the suppliers of the child material of a new material relation with a ConnectToChild event. The parent
     * is identified by its Catena-X ID (materialNumberCx), which is the globalAssetId of its Digital Twin.
     *
     * @param materialRelation the new material relation
     */
    public void sendConnectToChild(MaterialRelation materialRelation) {
        Material parent = materialService.findByOwnMaterialNumber(materialRelation.getParentOwnMaterialNumber());
        for (MaterialPartnerRelation supplierRelation : mprService.findAllSupplierRelations(materialRelation.getChildOwnMaterialNumber())) {
            Partner supplier = supplierRelation.getPartner();
            if (supplierRelation.getPartnerCXNumber() == null) {
                log.warn("Not sending ConnectToChild to {}, its Catena-X ID for material {} is unknown",
                    supplier.getBpnl(), materialRelation.getChildOwnMaterialNumber());
                continue;
            }
            try {
                JsonNode message = messageService.createMessage(supplier, IndustryCoreMessageContext.DIGITAL_TWIN_EVENT_CONNECT_TO_CHILD_CONTEXT, createPayload(materialRelation, parent, supplierRelation.getPartnerCXNumber()));
                if (edcAdapterService.doDigitalTwinEventPostRequest(supplier, message) == null) {
                    log.error("Failed to send ConnectToChild for material {} to {}", materialRelation.getChildOwnMaterialNumber(), supplier.getBpnl());
                } else {
                    log.info("Sent ConnectToChild for material {} to {}", materialRelation.getChildOwnMaterialNumber(), supplier.getBpnl());
                }
            } catch (Exception e) {
                log.error("Error sending ConnectToChild for material {} to {}: {}", materialRelation.getChildOwnMaterialNumber(), supplier.getBpnl(), e);
            }
        }
    }

    private Material findMaterialSuppliedTo(String catenaXId, Partner partner, String bpnl) {
        Material material = materialService.findByMaterialNumberCx(catenaXId);
        if (material == null) {
            throw new NoSuchElementException("Unknown child material " + catenaXId);
        }
        if (partner == null || !mprService.partnerOrdersProduct(material, partner)) {
            throw new NoSuchElementException("Child material " + catenaXId + " is not supplied to " + bpnl);
        }
        return material;
    }

    private ExternalMaterialRelation toRelation(Material child, Partner partner, ParentData parent) {
        if (!partner.getBpnl().equals(parent.getBusinessPartner())) {
            throw new IllegalArgumentException("Partner of parent " + parent.getCatenaXId() + " does not match the sender " + partner.getBpnl());
        }
        ValidityPeriod validityPeriod = null;
        if (parent.getValidityPeriod() != null) {
            Date validFrom = parent.getValidityPeriod().getValidFrom();
            Date validTo = parent.getValidityPeriod().getValidTo();
            if (validFrom != null && validTo != null && validFrom.after(validTo)) {
                throw new IllegalArgumentException("validFrom of parent " + parent.getCatenaXId() + " is after validTo");
            }
            validityPeriod = new ValidityPeriod(validFrom, validTo);
        }
        ExternalMaterialRelation relation = new ExternalMaterialRelation();
        relation.setMaterial(child);
        relation.setPartner(partner);
        relation.setParentGlobalAssetId(parent.getCatenaXId());
        relation.setValidityPeriod(validityPeriod);
        return relation;
    }

    private void createOrUpdate(ExternalMaterialRelation relation) {
        Date now = new Date();
        ExternalMaterialRelation existingRelation = externalMaterialRelationRepository
            .findByMaterialAndPartnerAndParentGlobalAssetId(relation.getMaterial(), relation.getPartner(), relation.getParentGlobalAssetId())
            .orElse(null);
        if (existingRelation != null) {
            existingRelation.setValidityPeriod(relation.getValidityPeriod());
            existingRelation.setLastModifiedOn(now);
            externalMaterialRelationRepository.save(existingRelation);
            log.info("Updated relation of material {} to parent {}", relation.getMaterial().getOwnMaterialNumber(), relation.getParentGlobalAssetId());
            return;
        }
        relation.setCreatedOn(now);
        relation.setLastModifiedOn(now);
        externalMaterialRelationRepository.save(relation);
    }

    private UniqueIdPushConnectTochildPayload createPayload(MaterialRelation materialRelation, Material parent, String childCatenaXId) {
        ValidityPeriod validityPeriod = null;
        if (materialRelation.getValidFrom() != null || materialRelation.getValidTo() != null) {
            validityPeriod = new ValidityPeriod(materialRelation.getValidFrom(), materialRelation.getValidTo());
        }
        ParentData parentData = new ParentData(
            materialRelation.getCreatedOn(),
            new ItemQuantityEntity(materialRelation.getQuantity(), materialRelation.getMeasurementUnit()),
            materialRelation.getLastModifiedOn(), validityPeriod, partnerService.getOwnPartnerEntity().getBpnl(), parent.getMaterialNumberCx());
        UsageAsPlannedItem item = new UsageAsPlannedItem(childCatenaXId, List.of(parentData));
        return new UniqueIdPushConnectTochildPayload(UniqueIdPushConnectTochildPayload.PART_TYPE, INFORMATION, List.of(item));
    }
}
