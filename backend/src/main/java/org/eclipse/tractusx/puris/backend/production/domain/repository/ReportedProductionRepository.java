/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.production.domain.repository;

import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedProduction;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportedProductionRepository extends ProductionRepository<ReportedProduction> {


    List<ReportedProduction> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    @Override
    default List<ReportedProduction> getForOwnMaterialNumber(String ownMatNbr) {
        return findAllByMaterial_OwnMaterialNumber(ownMatNbr);
    }

    List<ReportedProduction> findByMaterial_OwnMaterialNumberAndPartner_Bpnl(String ownMaterialNumber, String partnerBpnl);

    @Override
    default List<ReportedProduction> getForOwnMaterialNumberAndPartnerBPNL(String ownMatNbr, String bpnl) {
        return findByMaterial_OwnMaterialNumberAndPartner_Bpnl(ownMatNbr, bpnl);
    }

    List<ReportedProduction> findByMaterial_OwnMaterialNumberAndPartner_BpnlAndProductionSiteBpns(String ownMaterialNumber, String partnerBpnl, String bpns);

    @Override
    default List<ReportedProduction> getForOwnMaterialNumberAndPartnerBPNLAndBPNS(String ownMatNbr, String bpnl, String bpns) {
        return findByMaterial_OwnMaterialNumberAndPartner_BpnlAndProductionSiteBpns(ownMatNbr, bpnl, bpns);
    }

    List<ReportedProduction> findByMaterial_OwnMaterialNumberAndProductionSiteBpns(String ownMatNbr, String bpns);

    @Override
    default List<ReportedProduction> getForOwnMaterialNumberAndBPNS(String ownMatNbr, String bpns) {
        return findByMaterial_OwnMaterialNumberAndProductionSiteBpns(ownMatNbr, bpns);
    }

}
