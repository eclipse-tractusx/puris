package org.eclipse.tractusx.puris.backend.common.api.logic.service;

import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageContent;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.ReferenceIdentification;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public abstract class RequestApiServiceBaseImpl implements RequestApiService {

    @Autowired
    private Request requestService;

    @Override
    public void handleRequest(Request request) {
        Map<ReferenceIdentification, MessageContent> responseMap = new HashMap<>();
        for (MessageContent messageContent : request.getMessage().getPayload()) {
            // TODO: Response needs Map<ReferenceIdentification, MessageContent> as payload.
            MessageContent resultContent = determineRequestedData(messageContent);

            // TODO: Where is the referenceIdentification per Content? Rethink concept.
            //  ForRequest and Response

        }
    }

    private void sendResponseToRequestApiCall(Request request) {

    }


    abstract MessageContent determineRequestedData(MessageContent messageContent);


    private void sendResponseToResponseApi(Response response) {

    }
}
