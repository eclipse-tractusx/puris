/*
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DspProtocolVersionEnum {
    
    // order assumes the publishing date, thus last entry is latest
    V_0_8("dataspace-protocol-http:0_8"),
    V_2025_01("dataspace-protocol-http:2025-1");

    private String version;

    DspProtocolVersionEnum(String version){
        this.version = version;
    }

    public static DspProtocolVersionEnum fromVersion(String version) {
        for (DspProtocolVersionEnum dspProtocolVersion : DspProtocolVersionEnum.values()) {
            if (dspProtocolVersion.getVersion().equals(version)) {
                return dspProtocolVersion;
            }
        }
        throw new IllegalArgumentException("Unknown version: " + version);
    }

    // TODO: do I need the annotation?
    @JsonValue
    public String getVersion() {
        return version;
    }
        
}