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

public class AddressTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()){
            validator = factory.getValidator();
        }
    }

    @ParameterizedTest
    @ValueSource(strings ={
        "123 Main St 12",
        "Main St 12",
        "Wall Street 101",
        "Musterstraße 35b",
        "Test Str. 5"
    })
    public void test_validStreetAndNumberRegex(String streetAndNumber) {
        Address address = new Address("BPNA1234567890AA", streetAndNumber, "12345 New York", "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "Missing \n Nubmer Str.",
        "4711\r"
    })
    public void test_invalidStreetAndNumberRegex(String streetAndNumber) {
        Address address = new Address("BPNA1234567890AA", streetAndNumber, "12345 New York", "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(1, violations.size());
        ConstraintViolation<Address> violation = violations.iterator().next();
        assertEquals("streetAndNumber", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNA1234567890E",
        "BPNAE",
        "BPNA12345E6666A",
        "BPNS5555555555AA"
    })
    public void test_invalidBpnaRegex(String bpna) {
        Address address = new Address(bpna, "Test Street 5", "12345 New York", "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(1, violations.size());
        ConstraintViolation<Address> violation = violations.iterator().next();
        assertEquals("bpna", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "BPNA1234567890EE",
        "BPNA4444444444FA",
    })
    public void test_validBpnaRegex(String bpna) {
        Address address = new Address(bpna, "Test Street 5", "12345 New York", "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings ={
        "51766 Engelskirchen",
        "12345 New York",
        "1000 Bruxelles"
    })
    public void test_validZipCodeAndCityRegex(String zipCodeAndCity) {
        Address address = new Address("BPNA1234567890AA", "Test Str. 1", zipCodeAndCity, "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "47111\r",
        "471112 Wrong zip code\n"
    })
    public void test_invalidZipCodeAndCityRegex(String zipCodeAndCity) {
        Address address = new Address("BPNA1234567890AA", "Test Str. 1", zipCodeAndCity, "USA");

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(1, violations.size());
        ConstraintViolation<Address> violation = violations.iterator().next();
        assertEquals("zipCodeAndCity", violation.getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings ={
        "United States of America",
        "Germany"
    })
    public void test_validCountryRegex(String country) {
        Address address = new Address("BPNA1234567890AA", "Test Str. 1", "1000 Bruxelles", country);

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "47\n111"
    })
    public void test_invalidCountryRegex(String country) {
        Address address = new Address("BPNA1234567890AA", "Test Str. 1", "1000 Bruxelles", country);

        Set<ConstraintViolation<Address>> violations = validator.validate(address);

        assertEquals(1, violations.size());
        ConstraintViolation<Address> violation = violations.iterator().next();
        assertEquals("country", violation.getPropertyPath().toString());
    }
}
