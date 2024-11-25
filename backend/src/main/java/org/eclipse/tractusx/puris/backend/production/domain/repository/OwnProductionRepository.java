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

import org.eclipse.tractusx.puris.backend.production.domain.model.OwnProduction;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnProductionRepository extends ProductionRepository<OwnProduction> {

    List<OwnProduction> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    @Override
    default List<OwnProduction> getForOwnMaterialNumber(String ownMatNbr) {
        return findAllByMaterial_OwnMaterialNumber(ownMatNbr);
    }

    List<OwnProduction> findAllByMaterial_OwnMaterialNumberAndPartner_Bpnl(String ownMaterialNumber, String partnerBpnl);

    @Override
    default List<OwnProduction> getForOwnMaterialNumberAndPartnerBPNL(String ownMatNbr, String bpnl) {
        return findAllByMaterial_OwnMaterialNumberAndPartner_Bpnl(ownMatNbr, bpnl);
    }

    List<OwnProduction> findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndProductionSiteBpns(String ownMaterialNumber, String partnerBpnl, String bpns);

    @Override
    default List<OwnProduction> getForOwnMaterialNumberAndPartnerBPNLAndBPNS(String ownMatNbr, String bpnl, String bpns) {
        return findAllByMaterial_OwnMaterialNumberAndPartner_BpnlAndProductionSiteBpns(ownMatNbr, bpnl, bpns);
    }

    List<OwnProduction> findAllByMaterial_OwnMaterialNumberAndProductionSiteBpns(String ownMatNbr, String bpns);

    @Override
    default List<OwnProduction> getForOwnMaterialNumberAndBPNS(String ownMatNbr, String bpns) {
        return findAllByMaterial_OwnMaterialNumberAndProductionSiteBpns(ownMatNbr, bpns);
    }

}
