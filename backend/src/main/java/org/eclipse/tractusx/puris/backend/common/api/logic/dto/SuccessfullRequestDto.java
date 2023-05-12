package org.eclipse.tractusx.puris.backend.common.api.logic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class SuccessfullRequestDto {

    @JsonProperty("request-id")
    private UUID requestId;
}
