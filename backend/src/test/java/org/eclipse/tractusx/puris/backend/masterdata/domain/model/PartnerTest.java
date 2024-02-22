/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartnerTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
            validator = factory.getValidator();
        }
    }

    @Test
    public void test_invalidPartnerName() {
        Partner partner = new Partner("Invalid\nName", "https://www.example.com", "BPNL1234567890LE",
            "BPNS1234567890LE", "Site A", "BPNA1234567890LE", "123 Main St", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(1, violations.size());
        ConstraintViolation<Partner> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNL1234567890E",
        "BPNLE",
        "BPNS12345E6666A",
        "BPNA5555555555AA"
    })
    public void test_invalidBpnlRegex(String bpnl) {
        Partner partner = new Partner("ABC Company", "https://www.example.com", bpnl,
            "BPNS1234567890EE", "Site A", "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(1, violations.size());
        ConstraintViolation<Partner> violation = violations.iterator().next();
        assertEquals("bpnl", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNL1234567890LE",
        "BPNL5555555555AA"
    })
    public void test_validBpnlRegex(String bpnl) {
        Partner partner = new Partner("ABC Company", "https://www.example.com", bpnl,
            "BPNS1234567890EE", "Site A", "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://isst-edc-supplier.int.demo.catena-x.net",
        "http://customer-control-plane:8184/api/v1/dsp",
        "https://localhost:8181/api/v1/dsp",
        "http://127.0.0.1:8081/api/v1/dsp"
    })
    public void test_validEdcRegex(String edcUrl) {
        Partner partner = new Partner("ABC Company", edcUrl, "BPNL1234567890LE",
            "BPNS1234567890ZZ", "Site A", "BPNA1234567890ZZ", "123 Main Str.", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "www.hostname.de",
        "invalid@email.de"
    })
    public void test_invalidEdcRegex(String edcUrl) {
        Partner partner = new Partner("ABC Company", edcUrl, "BPNL1234567890LE",
            "BPNS1234567890ZZ", "Site A", "BPNA1234567890ZZ", "123 Main Str.", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(1, violations.size());
        ConstraintViolation<Partner> violation = violations.iterator().next();
        assertEquals("edcUrl", violation.getPropertyPath().toString());
    }

}
