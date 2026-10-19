/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataExchangeRequestForwardService {
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedNotificationService;
    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;
    @Autowired
    private MaterialRelationService materialRelationService;
    @Autowired
    private MaterialPartnerRelationService mprService;
 
    public record ForwardTarget(Partner partner, ReportedDemandAndCapacityNotification notification, List<Material> materials, Date start, Date end) {}
 
    /**
     * Resolves the partners a request can be forwarded to.
     * If the origin is linked to an OwnDemandAndCapacityNotification, its related notifications are followed. Notifications belonging to the
     * requesting partner, without affected materials.
     * Otherwise the request continues along the bill of material, in the direction its effect gives: a capacity effect
     * to the suppliers of the child materials, a demand effect to the customers of the parent materials. Each such
     * partner becomes a target with its materials.
     */
    public List<ForwardTarget> resolveForwardTargets(ReportedDataExchangeRequest origin) {
        if (origin.getNotification() != null) {
            return resolveTargetsFromRelatedNotifications(origin);
        }
        return resolveTargetsFromMaterialRelation(origin);
    }
 
    private List<ForwardTarget> resolveTargetsFromRelatedNotifications(ReportedDataExchangeRequest origin) {
        OwnDemandAndCapacityNotification originNotification = origin.getNotification();
        List<UUID> relatedIds = originNotification.getRelatedNotificationIds();
        if (relatedIds == null || relatedIds.isEmpty()) {
            return List.of();
        }
        String requesterBpnl = origin.getPartner().getBpnl();
        List<ForwardTarget> targets = new ArrayList<>();
 
        for (ReportedDemandAndCapacityNotification target : reportedNotificationService.findByNotificationIdIn(relatedIds)) {
            if (requesterBpnl.equals(target.getPartner().getBpnl())) {
                log.info("Skipping forward target {}: notification belongs to the requesting partner", target.getNotificationId());
                continue;
            }
            if (target.getMaterials() == null || target.getMaterials().isEmpty()) {
                log.info("Skipping forward target {}: notification has no affected materials", target.getNotificationId());
                continue;
            }
            Date start = maxDate(origin.getDesiredStartDateTime(), target.getStartDateOfEffect());
            Date end = target.getExpectedEndDateOfEffect() == null ? origin.getDesiredEndDateTime() : minDate(origin.getDesiredEndDateTime(), target.getExpectedEndDateOfEffect());
 
            if (!start.before(end)) {
                log.info("Skipping forward target {}: requested window does not overlap the notification window", target.getNotificationId());
                continue;
            }
            targets.add(new ForwardTarget(target.getPartner(), target, new ArrayList<>(target.getMaterials()), start, end));
        }
        return targets;
    }
 
    private List<ForwardTarget> resolveTargetsFromMaterialRelation(ReportedDataExchangeRequest origin) {
        if (origin.getMaterials() == null || origin.getMaterials().isEmpty()) {
            return List.of();
        }
        Date now = new Date();
        String requesterBpnl = origin.getPartner().getBpnl();
 
        // The effect gives the direction, which keeps requests from running in circles: capacity effects go on to the
        // suppliers of the child materials, demand effects to the customers of the parent materials.
        boolean demand = isDemandEffect(origin.getEffect());
 
        // 1. Collect the related materials, as own material numbers since that is all a relation holds: the parents
        // for a demand effect, the children for a capacity effect, as far as their relation is valid now.
        Set<String> relatedMaterialNumbers = new LinkedHashSet<>();
        for (Material material : origin.getMaterials()) {
            List<MaterialRelation> relations = demand
                ? materialRelationService.findAllParents(material.getOwnMaterialNumber())
                : materialRelationService.findAllChildren(material.getOwnMaterialNumber());
            for (MaterialRelation relation : relations) {
                if (MaterialRelationService.isRelationValidNow(relation, now)) {
                    relatedMaterialNumbers.add(demand ? relation.getParentOwnMaterialNumber() : relation.getChildOwnMaterialNumber());
                }
            }
        }
        if (relatedMaterialNumbers.isEmpty()) {
            log.info("The affected materials have no currently valid {} materials, nothing to forward", demand ? "parent" : "child");
            return List.of();
        }
 
        // 2. Look up the partner relations of the related materials, customers for a demand effect and suppliers for a
        // capacity effect, and group them by partner. The requester is never a target.
        List<MaterialPartnerRelation> partnerRelations = demand
            ? mprService.findAllCustomerRelationsForOwnMaterialNumbers(relatedMaterialNumbers)
            : mprService.findAllSupplierRelationsForOwnMaterialNumbers(relatedMaterialNumbers);
        Map<String, List<MaterialPartnerRelation>> relationsByPartner = partnerRelations.stream()
            .filter(relation -> !requesterBpnl.equals(relation.getPartner().getBpnl()))
            .collect(Collectors.groupingBy(relation -> relation.getPartner().getBpnl(), LinkedHashMap::new, Collectors.toList()));
 
        // 3. One target for each partner, with the related materials it is related to.
        List<ForwardTarget> targets = new ArrayList<>();
        for (List<MaterialPartnerRelation> relationsOfPartner : relationsByPartner.values()) {
            List<Material> materials = relationsOfPartner.stream().map(MaterialPartnerRelation::getMaterial).toList();
            targets.add(new ForwardTarget(relationsOfPartner.getFirst().getPartner(), null, materials,
                origin.getDesiredStartDateTime(), origin.getDesiredEndDateTime()));
        }
        return targets;
    }
 
    public List<OwnDataExchangeRequest> createForwardedRequests(ReportedDataExchangeRequest origin, List<ForwardTarget> targets) throws KeyAlreadyExistsException {
        List<OwnDataExchangeRequest> created = new ArrayList<>();
        for (ForwardTarget target : targets) {
            ReportedDemandAndCapacityNotification notification = target.notification();
            OwnDataExchangeRequest forwarded = OwnDataExchangeRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .partner(target.partner())
                .notification(notification)
                .relatedDataExchangeRequest(origin)
                .sourceDisruptionId(origin.getSourceDisruptionId())
                .effect(notification != null ? notification.getEffect() : origin.getEffect())
                .materials(new ArrayList<>(target.materials()))
                .affectedSitesSender(notification != null ? copyOf(notification.getAffectedSitesRecipient()) : new ArrayList<>())
                .affectedSitesRecipient(notification != null ? copyOf(notification.getAffectedSitesSender()) : new ArrayList<>())
                .criticality(origin.getCriticality())
                .desiredStartDateTime(target.start())
                .desiredEndDateTime(target.end())
                .requestedTypes(new ArrayList<>(origin.getRequestedTypes()))
                .text("Forwarded data exchange request from another partner in the supply chain")
                .build();
            try {
                created.add(ownDataExchangeRequestService.create(forwarded));
            } catch (IllegalArgumentException e) {
                log.warn("Could not forward to partner {}: {}", target.partner().getBpnl(), e.getMessage());
            }
        }
        return created;
    }

    private static boolean isDemandEffect(EffectEnumeration effect) {
        return effect == EffectEnumeration.DEMAND_INCREASE || effect == EffectEnumeration.DEMAND_REDUCTION;
    }
 
    private static <T> List<T> copyOf(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
 
    private static Date maxDate(Date a, Date b) { return a.before(b) ? b : a; }
    private static Date minDate(Date a, Date b) { return a.before(b) ? a : b; }
}
