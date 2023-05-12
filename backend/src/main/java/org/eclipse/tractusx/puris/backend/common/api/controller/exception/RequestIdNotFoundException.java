package org.eclipse.tractusx.puris.backend.common.api.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;


@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "No Request for Request ID has" +
        " been found.")
public class RequestIdNotFoundException extends RuntimeException {

    public RequestIdNotFoundException(UUID requestUuid) {
        super(String.format("Request with ID %s not found.", requestUuid));
    }
}
