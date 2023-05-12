package org.eclipse.tractusx.puris.backend.common.api.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;


@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Request ID has already been used.")
public class RequestIdNotFoundException extends RuntimeException {

    public RequestIdNotFoundException(UUID requestUuid) {
        super(String.format("Request ID %s has already been used.", requestUuid));
    }
}
