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

package org.eclipse.tractusx.puris.backend.delivery.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IncotermEnumeration {
    EXW("EXW"),
    FCA("FCA"),
    FAS("FAS"),
    FOB("FOB"),
    CFR("CFR"),
    CIF("CIF"),
    DAP("DAP"),
    DPU("DPU"),
    CPT("CPT"),
    CIP("CIP"),
    DDP("DDP");

    private String value;

    IncotermEnumeration(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public DeliveryResponsibilityEnumeration getResponsibility() {
        switch (this) {
            case EXW:
                return DeliveryResponsibilityEnumeration.CUSTOMER;
            case FCA:
                return DeliveryResponsibilityEnumeration.PARTIAL;
            case FAS:
                return DeliveryResponsibilityEnumeration.PARTIAL;
            case FOB:
                return DeliveryResponsibilityEnumeration.PARTIAL;
            case CFR:
                return DeliveryResponsibilityEnumeration.PARTIAL;
            case CIF:
                return DeliveryResponsibilityEnumeration.PARTIAL;
            case DAP:
                return DeliveryResponsibilityEnumeration.SUPPLIER;
            case DPU:
                return DeliveryResponsibilityEnumeration.SUPPLIER;
            case CPT:
                return DeliveryResponsibilityEnumeration.SUPPLIER;
            case CIP:
                return DeliveryResponsibilityEnumeration.SUPPLIER;
            case DDP:
                return DeliveryResponsibilityEnumeration.SUPPLIER;
            default:
                throw new IllegalArgumentException("Unknown Incoterm");
        }
    }
}
