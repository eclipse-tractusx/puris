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
    public void testPartnerNamePattern() {
        Partner partner = new Partner("Invalid!Name", "https://www.example.com", "BPNL1234567890LE",
            "BPNS123456780LE", "Site A", "BPNA1234567890LE", "123 Main St", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(1, violations.size());
        ConstraintViolation<Partner> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString());
    }

    @Test
    public void testInvalidBpnRegexes() {
        Partner partner = new Partner("ABC Company", "https://www.example.com", "BPN1234567890LE",
            "BPNS1234567890E", "Site A", "BPNA123", "123 Main St 12", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(1, violations.size());
        ConstraintViolation<Partner> violation = violations.iterator().next();
        assertEquals("bpnl", violation.getPropertyPath().toString());

        Set<ConstraintViolation<Site>> siteViolations = validator.validate(partner.getSites().first());

        assertEquals(1, siteViolations.size());
        ConstraintViolation<Site> siteViolation = siteViolations.iterator().next();
        assertEquals("bpns", siteViolation.getPropertyPath().toString());

        Set<ConstraintViolation<Address>> addressViolations = validator.validate(
            partner.getSites().first().getAddresses().first()
        );

        System.out.println(addressViolations);

        assertEquals(1, addressViolations.size());
        ConstraintViolation<Address> addressViolation = addressViolations.iterator().next();
        assertEquals("bpna", addressViolation.getPropertyPath().toString());

    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://isst-edc-supplier.int.demo.catena-x.net",
        "http://customer-control-plane:8184/api/v1/dsp",
        "https://localhost:8181/api/v1/dsp"
    })
    public void testEdcRegexPattern(String edcUrl) {
        Partner partner = new Partner("ABC Company", edcUrl, "BPNL1234567890LE",
            "BPNS1234567890ZZ", "Site A", "BPNA1234567890ZZ", "123 Main Str.", "12345 New York", "USA");

        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);

        assertEquals(0, violations.size());
    }

}
