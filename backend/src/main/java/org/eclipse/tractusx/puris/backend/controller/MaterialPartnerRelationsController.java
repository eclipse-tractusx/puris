/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("materialpartnerrelations")
public class MaterialPartnerRelationsController {


    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @PostMapping
    @CrossOrigin
    @Operation(description = "Creates a new MaterialPartnerRelation with the given parameter data. " +
        "Please note that this is only possible, if the designated Material " +
        "and Partner entities have already been created before this request. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created a new MaterialPartnerRelationEntity."),
        @ApiResponse(responseCode = "400", description = "Material and/or Partner do not exist."),
        @ApiResponse(responseCode = "409", description = "Relation for given Material and Partner does already exist."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<?> createMaterialPartnerRelation(
        @Parameter(name = "ownMaterialNumber", description = "The Material Number that is used in your own company to identify the Material.",
            example = "MNR-7307-AU340474.002", required = true) @RequestParam String ownMaterialNumber,
        @Parameter(name = "partnerBpnl", description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR", required = true) @RequestParam() String partnerBpnl,
        @Parameter(name = "partnerMaterialNumber", description = "The Material Number that this Partner is using in his own company to identify the Material.",
            example = "MNR-8101-ID146955.001") @RequestParam String partnerMaterialNumber,
        @Parameter(name = "partnerSupplies", description = "This boolean flag indicates whether this Partner is a potential supplier of the given Material.",
            example = "true") @RequestParam boolean partnerSupplies,
        @Parameter(name = "partnerBuys", description = "This boolean flag indicates whether this Partner is a potential customer of this Material.",
            example = "true") @RequestParam boolean partnerBuys) {
        Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (material == null || partnerBpnl == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Partner partner = null;
        if (partnerBpnl != null) {
            partner = partnerService.findByBpnl(partnerBpnl);
        }
        if (partner == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }

        if (mprService.find(material, partner) != null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(409));
        }
        MaterialPartnerRelation newMpr = new MaterialPartnerRelation(material, partner, partnerMaterialNumber, partnerSupplies, partnerBuys);

        newMpr = mprService.create(newMpr);
        if (newMpr == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping
    @CrossOrigin
    @Operation(description = "Updates an existing MaterialPartnerRelation. You have to specify the ownMaterialNumber and " +
        "the partnerBpnl. The other three parameters are genuinely optional. Provide them only if you want to change their values. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Update was accepted."),
        @ApiResponse(responseCode = "404", description = "No existing entity was found. "),
        @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<?> updateMaterialPartnerRelation(
        @Parameter(name = "ownMaterialNumber", description = "The Material Number that is used in your own company to identify the Material.",
            example = "MNR-7307-AU340474.002", required = true) @RequestParam String ownMaterialNumber,
        @Parameter(name = "partnerBpnl", description = "The unique BPNL that was assigned to that Partner.",
            example = "BPNL2222222222RR", required = true) @RequestParam() String partnerBpnl,
        @Parameter(name = "partnerMaterialNumber", description = "The Material Number that this Partner is using in his own company to identify the Material.",
            example = "MNR-8101-ID146955.001") @RequestParam(required = false) String partnerMaterialNumber,
        @Parameter(name = "partnerSupplies", description = "This boolean flag indicates whether this Partner is a potential supplier of the given Material.",
            example = "true") @RequestParam(required = false) Boolean partnerSupplies,
        @Parameter(name = "partnerBuys", description = "This boolean flag indicates whether this Partner is a potential customer of this Material.",
            example = "true") @RequestParam(required = false) Boolean partnerBuys) {
        MaterialPartnerRelation existingRelation = null;
        Partner partner = partnerService.findByBpnl(partnerBpnl);
        Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (partner != null && material != null) {
            existingRelation = mprService.find(material, partner);
        }
        if (existingRelation == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        if (partnerSupplies != null) {
            existingRelation.setPartnerSuppliesMaterial(partnerSupplies);
        }
        if (partnerBuys != null) {
            existingRelation.setPartnerBuysMaterial(partnerBuys);
        }
        if (partnerMaterialNumber != null) {
            existingRelation.setPartnerMaterialNumber(partnerMaterialNumber);
        }
        existingRelation = mprService.update(existingRelation);
        if (existingRelation == null) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(500));
        }

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

}
