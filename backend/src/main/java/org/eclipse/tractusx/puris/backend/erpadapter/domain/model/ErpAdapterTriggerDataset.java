/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.erpadapter.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.*;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@IdClass(ErpAdapterTriggerDataset.Key.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErpAdapterTriggerDataset {

    @Id
    private String partnerBpnl;

    @Id
    private String ownMaterialNumber;

    @Id
    private AssetType assetType;

    @Id
    private String directionCharacteristic;

    private long lastPartnerRequest;

    private long nextErpRequestScheduled;

    @Override
    public String toString() {
        return "ErpAdapterTriggerDataset{" +
            "partnerBpnl='" + partnerBpnl + '\'' +
            ", ownMaterialNumber='" + ownMaterialNumber + '\'' +
            ", assetType=" + assetType +
            ", directionCharacteristic='" + directionCharacteristic + '\'' +
            ", lastPartnerRequest=" + new Date(lastPartnerRequest) +
            ", nextErpRequestScheduled=" + new Date(nextErpRequestScheduled) +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErpAdapterTriggerDataset dataset)) return false;
        return Objects.equals(partnerBpnl, dataset.partnerBpnl) && Objects.equals(ownMaterialNumber,
            dataset.ownMaterialNumber) && assetType == dataset.assetType &&
            Objects.equals(directionCharacteristic, dataset.directionCharacteristic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partnerBpnl, ownMaterialNumber, assetType, directionCharacteristic);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Key implements Serializable {
        private String partnerBpnl;
        private String ownMaterialNumber;
        private AssetType assetType;
        private String directionCharacteristic;

    }
}
