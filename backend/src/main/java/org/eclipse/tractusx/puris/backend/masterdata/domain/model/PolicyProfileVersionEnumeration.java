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

import com.fasterxml.jackson.annotation.JsonValue;

public enum PolicyProfileVersionEnumeration {
    POLICY_PROFILE_2405("profile2405",
            "https://w3id.org/catenax/policy/",
            "https://w3id.org/tractusx/policy/v1.0.0",
            "http://www.w3.org/ns/odrl.jsonld",
            "https://w3id.org/dspace/v0.8/",
            "Contract_Policy_for_Profile_2405"
    ),
    POLICY_PROFILE_2509("profile2509",
            "https://w3id.org/catenax/2025/9/policy/",
            "https://w3id.org/catenax/2025/9/policy/context.jsonld",
            "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
            "https://w3id.org/edc/dspace/v0.0.1",
            "Contract_Policy_for_Profile_2509"
    );

    private String value;
    public String CX_POLICY_NAMESPACE;
    public String CX_POLICY_CONTEXT;
    public String CONTRACT_POLICY_ID;
    public String DTR_CONTRACT_POLICY_ID;
    public String ODRL_REMOTE_CONTEXT;
    public String DSPACE_NAMESPACE;

    PolicyProfileVersionEnumeration(String value, String CX_POLICY_NAMESPACE, String CX_POLICY_CONTEXT, String ODRL_REMOTE_CONTEXT, String DSPACE_NAMESPACE, String Contract_Policy) {
        this.value = value;
        this.CX_POLICY_NAMESPACE = CX_POLICY_NAMESPACE;
        this.CX_POLICY_CONTEXT = CX_POLICY_CONTEXT;
        this.ODRL_REMOTE_CONTEXT = ODRL_REMOTE_CONTEXT;
        this.DSPACE_NAMESPACE = DSPACE_NAMESPACE;
        this.CONTRACT_POLICY_ID = Contract_Policy;
        this.DTR_CONTRACT_POLICY_ID = Contract_Policy + "_DTR";
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
