/*
 * Copyright (c) 2025 Volkswagen AG
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.masterdata.domain.model;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.PolicyProfileConstants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PolicyProfileVersionEnumeration {
    POLICY_PROFILE_2405("profile2405", new PolicyProfileConstants(
            "https://w3id.org/catenax/policy/",
            "https://w3id.org/tractusx/policy/v1.0.0",
            "Contract_Policy_for_Profile_2405"
    )),
    POLICY_PROFILE_2509("profile2509", new PolicyProfileConstants(
            "https://w3id.org/catenax/2025/9/policy/",
            "https://w3id.org/catenax/2025/9/policy/context.jsonld",
            "Contract_Policy_for_Profile_2509"
    ));

    private String value;
    private final PolicyProfileConstants constants;

    PolicyProfileVersionEnumeration(String value, PolicyProfileConstants constants) {
        this.value = value;
        this.constants = constants;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public PolicyProfileConstants getConstants() {
        return constants;
    }
}
