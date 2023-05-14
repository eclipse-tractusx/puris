package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * Generated class {@link LocationIdTypeEnum}.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LocationIdTypeEnum {
    B_P_N_S("BPNS"), B_P_N_A("BPNA");

    private String value;

    LocationIdTypeEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    static LocationIdTypeEnum enumDeserializationConstructor(String value) {
        return fromValue(value).orElseThrow(() -> new RuntimeException(
                "Tried to parse value \"" + value + "\", but there is no enum field like that in LocationIdTypeEnum"));
    }

    public static Optional<LocationIdTypeEnum> fromValue(String value) {
        return Arrays.stream(LocationIdTypeEnum.values()).filter(enumValue -> compareEnumValues(enumValue, value))
                .findAny();
    }

    private static boolean compareEnumValues(LocationIdTypeEnum enumValue, String value) {
        return enumValue.getValue().equals(value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}
