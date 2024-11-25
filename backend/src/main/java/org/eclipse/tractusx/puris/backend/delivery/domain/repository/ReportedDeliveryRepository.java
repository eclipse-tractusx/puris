/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.delivery.domain.repository;

import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedDelivery;

import java.util.ArrayList;
import java.util.List;

public interface ReportedDeliveryRepository extends DeliveryRepository<ReportedDelivery> {

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    @Override
    default List<ReportedDelivery> getForOwnMaterialNumber(String ownMatNbr) {
        return findAllByMaterial_OwnMaterialNumber(ownMatNbr);
    }

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumberAndPartner_Bpnl(String ownMatNbr, String partnerBpnl);

    @Override
    default List<ReportedDelivery> getForOwnMaterialNumberAndPartnerBPNL(String ownMatNbr, String bpnl) {
        return findAllByMaterial_OwnMaterialNumberAndPartner_Bpnl(ownMatNbr, bpnl);
    }

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumberAndOriginBpns(String ownMatNbr, String originBpns);

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumberAndDestinationBpns(String ownMatNbr, String destinationBpns);

    @Override
    default List<ReportedDelivery> getForOwnMaterialNumberAndBPNS(String ownMatNbr, String bpns) {
        var resultList = new ArrayList<>(findAllByMaterial_OwnMaterialNumberAndOriginBpns(ownMatNbr, bpns));
        resultList.addAll(findAllByMaterial_OwnMaterialNumberAndDestinationBpns(ownMatNbr, bpns));
        return resultList;
    }

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndDestinationBpns(String ownMatNbr, String partnerBpnl, String destinationBpnl);

    List<ReportedDelivery> findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndOriginBpns(String ownMatNbr, String partnerBpnl, String originBpnl);

    @Override
    default List<ReportedDelivery> getForOwnMaterialNumberAndPartnerBPNLAndBPNS(String ownMatNbr, String bpnl, String bpns) {
        var resultList = new ArrayList<>(findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndDestinationBpns(ownMatNbr, bpnl, bpns));
        resultList.addAll(findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndOriginBpns(ownMatNbr, bpnl, bpns));
        return resultList;
    }

}
