
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * Generated class {@link FunctionCharacteristic}.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FunctionCharacteristic {
	PRODUCTION("production"), WAREHOUSE("warehouse"), SPARE_PART_WAREHOUSE("spare part warehouse");

	private String value;

	FunctionCharacteristic(String value) {
		this.value = value;
	}

	@JsonCreator
	static FunctionCharacteristic enumDeserializationConstructor(String value) {
		return fromValue(value).orElseThrow(() -> new RuntimeException("Tried to parse value \"" + value
				+ "\", but there is no enum field like that in FunctionCharacteristic"));
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	public static Optional<FunctionCharacteristic> fromValue(String value) {
		return Arrays.stream(FunctionCharacteristic.values()).filter(enumValue -> compareEnumValues(enumValue, value))
				.findAny();
	}

	private static boolean compareEnumValues(FunctionCharacteristic enumValue, String value) {
		return enumValue.getValue().equals(value);
	}

}
