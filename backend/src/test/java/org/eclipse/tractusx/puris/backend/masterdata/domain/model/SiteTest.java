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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SiteTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ValueSource(strings ={
        "New Company Inc.",
        "Help-Desk Company",
        "Volkswagen AG"
    })
    public void test_validNameRegex(String name) {
        Site site = new Site("BPNS1234567890SS", name, "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Site>> violations = validator.validate(site);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid name\r",
        "@email\n"
    })
    public void test_invalidNameRegex(String name) {
        Site site = new Site("BPNS1234567890SS", name, "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Site>> violations = validator.validate(site);

        assertEquals(1, violations.size());
        ConstraintViolation<Site> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNS1234567890E",
        "BPNSE",
        "BPNS12345E6666A",
        "BPNA5555555555AA"
    })
    public void test_invalidBpnsRegex(String bpns) {
        Site site = new Site(bpns, "Site A", "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Site>> violations = validator.validate(site);

        assertEquals(1, violations.size());
        ConstraintViolation<Site> violation = violations.iterator().next();
        assertEquals("bpns", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNS1234567890EE",
        "BPNS4444444444FA",
    })
    public void test_validBpnsRegex(String bpns) {
        Site site = new Site(bpns, "Site A", "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Site>> violations = validator.validate(site);

        assertEquals(0, violations.size());
    }
}
