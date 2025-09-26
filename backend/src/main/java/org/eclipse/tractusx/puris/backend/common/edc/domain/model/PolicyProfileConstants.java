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

package org.eclipse.tractusx.puris.backend.common.edc.domain.model;

public class PolicyProfileConstants {
    public String EDC_NAMESPACE;
    public String VOCAB_KEY;
    public String ODRL_NAMESPACE;
    public String ODRL_REMOTE_CONTEXT;
    public String CX_TAXO_NAMESPACE;
    public String CX_COMMON_NAMESPACE;
    public String CX_POLICY_NAMESPACE;
    public String DCT_NAMESPACE;
    public String AAS_SEMANTICS_NAMESPACE;
    public String CONTRACT_POLICY_ID;
    public String TX_NAMESPACE;
    public String TX_AUTH_NAMESPACE;
    public String DCAT_NAMESPACE;
    public String DSPACE_NAMESPACE;
    public String CX_POLICY_CONTEXT;

    public PolicyProfileConstants(String CX_POLICY_NAMESPACE, String CX_POLICY_CONTEXT, String Contract_Policy) {
        this.CX_POLICY_NAMESPACE = CX_POLICY_NAMESPACE;
        this.CONTRACT_POLICY_ID = Contract_Policy;
        this.CX_POLICY_CONTEXT = CX_POLICY_CONTEXT;
        this.EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
        this.VOCAB_KEY = "@vocab";
        this.ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
        this.ODRL_REMOTE_CONTEXT = "http://www.w3.org/ns/odrl.jsonld";
        this.CX_TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
        this.CX_COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
        this.DCT_NAMESPACE = "http://purl.org/dc/terms/";
        this.AAS_SEMANTICS_NAMESPACE = "https://admin-shell.io/aas/3/0/HasSemantics/";
        this.TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
        this.TX_AUTH_NAMESPACE = "https://w3id.org/tractusx/auth/";
        this.DCAT_NAMESPACE = "http://www.w3.org/ns/dcat#";
        this.DSPACE_NAMESPACE = "https://w3id.org/dspace/v0.8/";
    }

    public PolicyProfileConstants(
        String EDC_NAMESPACE, String VOCAB_KEY, String ODRL_NAMESPACE, String ODRL_REMOTE_CONTEXT,
        String CX_TAXO_NAMESPACE, String CX_COMMON_NAMESPACE, String CX_POLICY_NAMESPACE,
        String DCT_NAMESPACE, String AAS_SEMANTICS_NAMESPACE, String CONTRACT_POLICY_ID, String TX,
        String TX_AUTH_NAMESPACE, String DCAT_NAMESPACE, String DSPACE_NAMESPACE, String CX_POLICY_CONTEXT
    ) {
        this.EDC_NAMESPACE = EDC_NAMESPACE;
        this.VOCAB_KEY = VOCAB_KEY;
        this.ODRL_NAMESPACE = ODRL_NAMESPACE;
        this.ODRL_REMOTE_CONTEXT = ODRL_REMOTE_CONTEXT;
        this.CX_TAXO_NAMESPACE = CX_TAXO_NAMESPACE;
        this.CX_COMMON_NAMESPACE = CX_COMMON_NAMESPACE;
        this.CX_POLICY_NAMESPACE = CX_POLICY_NAMESPACE;
        this.DCT_NAMESPACE = DCT_NAMESPACE;
        this.AAS_SEMANTICS_NAMESPACE = AAS_SEMANTICS_NAMESPACE;
        this.CONTRACT_POLICY_ID = CONTRACT_POLICY_ID;
        this.TX_NAMESPACE = TX;
        this.TX_AUTH_NAMESPACE = TX_AUTH_NAMESPACE;
        this.DCAT_NAMESPACE = DCAT_NAMESPACE;
        this.DSPACE_NAMESPACE = DSPACE_NAMESPACE;
        this.CX_POLICY_CONTEXT = CX_POLICY_CONTEXT;
    }
}
