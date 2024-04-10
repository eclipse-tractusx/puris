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

package org.eclipse.tractusx.puris.backend.common.edc.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
/**
 * This entity stores EDC contract information for a given partner.
 * Persisting this data helps to improve the efficiency, since in
 * general contract negotiations take a considerable amount of time
 * and EDC contracts in general are not time-limited and allow for
 * multiple transfer requests from the consuming party.
 */
public class EdcContractMapping {
    @Id
    private String partnerBpnl;

    private String dtrContractId;

    private String dtrAssetId;

    private String itemStockContractId;

    private String itemStockAssetId;

    private String itemStockEdcProtocolUrl;

    private String partTypeContractId;

    private String partTypeAssetId;

    @ElementCollection
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    @Getter(AccessLevel.NONE)
    private Map<String, String> materialToHrefMapping = new HashMap<>();

    public EdcContractMapping(String partnerBpnl){
        this.partnerBpnl = partnerBpnl;
    }

    /**
     * Assign the DTR HREF value to the respective MaterialNumber.
     *
     * @param key       the MaterialNumber
     * @param value     the HREF
     */
    public void putMaterialToHrefMapping(String key, String value){
        materialToHrefMapping.put(key + "@" + partnerBpnl, value);
    }

    /**
     * Retrieve the DTR HREF for a given MaterialNumber.
     *
     * @param key       the MaterialNumber
     * @return          the HREF
     */
    public String getMaterialToHrefMapping(String key){
        return materialToHrefMapping.get(key + "@" + partnerBpnl);
    }

}
