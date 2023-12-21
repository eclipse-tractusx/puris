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
        "123 Main St 12",
        "Main St 12",
        "Wall Street 101",
        "Musterstraße 35b",
        "Test Str. 5"
    })
    public void test_validNameRegex(String name) {
        Site site = new Site("BPNS1234567890SS", name, "BPNA1234567890AA", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Site>> violations = validator.validate(site);

        assertEquals(0, violations.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid name!",
        "@email"
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