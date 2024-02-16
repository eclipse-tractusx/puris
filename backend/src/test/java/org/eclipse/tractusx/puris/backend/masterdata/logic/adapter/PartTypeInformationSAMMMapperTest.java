package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@Slf4j
public class PartTypeInformationSAMMMapperTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Validator validator;

    static {
        try (var validationFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validationFactory.getValidator();
        }
    }

    @Test
    void testValidationShouldSuccess() {
        PartTypeInformationSAMM samm = getPartTypeInformationSAMMExample();
        Assertions.assertEquals(validator.validate(samm).size(), 0);
    }

    @Test
    void testValidationShouldFail() {
        PartTypeInformationSAMM samm = getPartTypeInformationSAMMExample();
        var partSitesInformation = samm.getPartSitesInformationAsPlanned().stream().findFirst().get();
        partSitesInformation.setFunctionValidUntil("\n");
        Assertions.assertNotEquals(validator.validate(samm).size(), 0);
    }

    @Test
    void testMarshallingAndUnmarshalling() throws Exception {
        PartTypeInformationSAMM partTypeInformationSAMM = getPartTypeInformationSAMMExample();

        Assertions.assertEquals(validator.validate(partTypeInformationSAMM).size(), 0);

        JsonNode marshalledSamm = objectMapper.readTree(objectMapper.writeValueAsString(partTypeInformationSAMM));
        log.info("Marshalled object: \n" + marshalledSamm.toPrettyString());
        JsonNode unmarshalledSamm = objectMapper.readTree(marshalledSamm.toString());
        PartTypeInformationSAMM sammFromJson = objectMapper.readValue(unmarshalledSamm.toString(), PartTypeInformationSAMM.class);

        Assertions.assertEquals(sammFromJson, partTypeInformationSAMM);


    }

    @NotNull
    private static PartTypeInformationSAMM getPartTypeInformationSAMMExample() {
        PartTypeInformationSAMM partTypeInformationSAMM = new PartTypeInformationSAMM();
        partTypeInformationSAMM.setCatenaXId(UUID.randomUUID().toString());

        PartSitesInformationAsPlanned partSitesInformationAsPlanned = new PartSitesInformationAsPlanned();
        partSitesInformationAsPlanned.setFunction(FunctionEnum.PRODUCTION);
        partSitesInformationAsPlanned.setCatenaXsiteId("BPNS1234567890ZZ");
        partSitesInformationAsPlanned.setFunctionValidFrom("2024-01-29T12:00:00.123+02:00");
        partSitesInformationAsPlanned.setFunctionValidUntil("2024-01-30T12:00:00.123+02:00");
        partTypeInformationSAMM.getPartSitesInformationAsPlanned().add(partSitesInformationAsPlanned);

        PartTypeInformationBody ptb = partTypeInformationSAMM.getPartTypeInformation();
        ptb.setNameAtManufacturer("Foo");
        ptb.setManufacturerPartId("Manufacturer Example Id");
        Classification classification = new Classification();
        classification.setClassificationDescription("Some Description");
        classification.setClassificationID("Some Classification-Id");
        classification.setClassificationStandard("Some Classification-Standard");
        ptb.getPartClassification().add(classification);
        partTypeInformationSAMM.setPartTypeInformation(ptb);
        return partTypeInformationSAMM;
    }
}
