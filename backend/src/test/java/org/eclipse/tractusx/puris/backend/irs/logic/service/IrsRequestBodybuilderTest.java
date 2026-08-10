/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.JsonLdConstants;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IrsRequestBodybuilderTest {

    @Mock
    private VariablesService variablesService;

    @InjectMocks
    private IrsRequestBodybuilder irsRequestBodybuilder;

    private static final String FRAMEWORK_AGREEMENT_WITH_VERSION = "DataExchangeGovernance:1.0";
    private static final String PURPOSE_WITH_VERSION = "cx.puris.base:1";

    @BeforeEach
    void setUp() {
        // IrsRequestBodybuilder uses constructor injection for VariablesService;
        // Mockito @InjectMocks handles this, but the ObjectMapper field is initialised
        // inline, so we do not need ReflectionTestUtils for it.
        lenient().when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn(FRAMEWORK_AGREEMENT_WITH_VERSION);
        lenient().when(variablesService.getPurisPurposeWithVersion()).thenReturn(PURPOSE_WITH_VERSION);
    }

    // --- buildFrameworkPolicyCreationRequestBody ---

    @Test
    void buildFrameworkPolicyCreationRequestBody_ValidUntilIsInTheFuture() {
        JsonNode body = irsRequestBodybuilder.buildFrameworkPolicyCreationRequestBody("some-policy-id", "some-purpose");

        assertThat(Instant.parse(body.get("validUntil").asText())).isAfter(Instant.now());
    }

    @Test
    void buildFrameworkPolicyCreationRequestBody_ContainsExpectedContextAndId() {
        JsonNode payload = irsRequestBodybuilder.buildFrameworkPolicyCreationRequestBody("some-policy-id", "some-purpose").get("payload");

        assertThat(payload.get("@context").get("odrl").asText()).isEqualTo(JsonLdConstants.ODRL_NAMESPACE);
        assertThat(payload.get("@context").get("edc").asText()).isEqualTo(JsonLdConstants.EDC_NAMESPACE);
        assertThat(payload.get("@context").get("cx-policy").asText()).isEqualTo(PolicyProfileVersionEnumeration.POLICY_PROFILE_2405.CX_POLICY_CONTEXT);
        assertThat(payload.get("@id").asText()).isEqualTo("some-policy-id");
    }

    @Test
    void buildFrameworkPolicyCreationRequestBody_PermissionAllowsUseAction() {
        JsonNode payload = irsRequestBodybuilder.buildFrameworkPolicyCreationRequestBody("some-policy-id", "some-purpose").get("payload");
        JsonNode permission = payload.get("policy").get("odrl:permission").get(0);

        assertThat(permission.get("odrl:action").asText()).isEqualTo("use");
    }

    @Test
    void buildFrameworkPolicyCreationRequestBody_ConstrainsOnFrameworkAgreementAndUsagePurpose() {
        JsonNode payload = irsRequestBodybuilder.buildFrameworkPolicyCreationRequestBody("some-policy-id", "some-purpose").get("payload");
        JsonNode andArray = payload.get("policy").get("odrl:permission").get(0).get("odrl:constraint").get("odrl:and");

        assertThat(andArray).hasSize(2);

        JsonNode frameworkAgreementConstraint = andArray.get(0);
        assertThat(frameworkAgreementConstraint.get("odrl:leftOperand").asText()).isEqualTo("cx-policy:FrameworkAgreement");
        assertThat(frameworkAgreementConstraint.get("odrl:operator").get("@id").asText()).isEqualTo("odrl:eq");
        assertThat(frameworkAgreementConstraint.get("odrl:rightOperand").asText()).isEqualTo(FRAMEWORK_AGREEMENT_WITH_VERSION);

        JsonNode usagePurposeConstraint = andArray.get(1);
        assertThat(usagePurposeConstraint.get("odrl:leftOperand").asText()).isEqualTo("cx-policy:UsagePurpose");
        assertThat(usagePurposeConstraint.get("odrl:operator").get("@id").asText()).isEqualTo("odrl:eq");
        assertThat(usagePurposeConstraint.get("odrl:rightOperand").asText()).isEqualTo("some-purpose");
    }

    // --- buildPurisFrameworkPolicyCreationRequestBody ---

    @Test
    void buildPurisFrameworkPolicyCreationRequestBody_UsesPurisPolicyIdAndPurpose() {
        JsonNode payload = irsRequestBodybuilder.buildPurisFrameworkPolicyCreationRequestBody().get("payload");
        JsonNode andArray = payload.get("policy").get("odrl:permission").get(0).get("odrl:constraint").get("odrl:and");

        assertThat(payload.get("@id").asText()).isEqualTo("puris-framework-policy");
        assertThat(andArray.get(1).get("odrl:rightOperand").asText()).isEqualTo(PURPOSE_WITH_VERSION);
    }

    // --- buildDtrFrameworkPolicyCreationRequestBody ---

    @Test
    void buildDtrFrameworkPolicyCreationRequestBody_UsesDtrPolicyIdAndPurpose() {
        JsonNode payload = irsRequestBodybuilder.buildDtrFrameworkPolicyCreationRequestBody().get("payload");
        JsonNode andArray = payload.get("policy").get("odrl:permission").get(0).get("odrl:constraint").get("odrl:and");

        assertThat(payload.get("@id").asText()).isEqualTo("dtr-framework-policy");
        assertThat(andArray.get(1).get("odrl:rightOperand").asText()).isEqualTo("cx.core.digitalTwinRegistry:1");
    }
}
