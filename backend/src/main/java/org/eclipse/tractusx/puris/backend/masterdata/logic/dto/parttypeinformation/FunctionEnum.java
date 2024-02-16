
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Generated class {@link FunctionEnum}.
 */
public enum FunctionEnum {
	PRODUCTION("production"), WAREHOUSE("warehouse"), SPARE_PART_WAREHOUSE("spare part warehouse");

	private String value;

	FunctionEnum(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}


}
