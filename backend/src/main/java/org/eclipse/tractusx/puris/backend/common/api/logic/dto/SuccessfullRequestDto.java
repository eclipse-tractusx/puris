package org.eclipse.tractusx.puris.backend.common.api.logic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SuccessfullRequestDto {

    @JsonProperty("request-id")
    private UUID requestId;
}
