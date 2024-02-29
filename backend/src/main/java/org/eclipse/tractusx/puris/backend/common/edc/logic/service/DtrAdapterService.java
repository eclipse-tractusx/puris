package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.DtrRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DtrAdapterService {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DtrRequestBodyBuilder dtrRequestBodyBuilder;

    private Response sendDtrPostRequest(JsonNode requestBody, List<String> pathSegments) throws Exception {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
            .post(body)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .build();
        return CLIENT.newCall(request).execute();
    }

    public void registerMaterialAtDtr(Material material) {
        var body = dtrRequestBodyBuilder.createMaterialRegistrationRequestBody(material);
        try (var response = sendDtrPostRequest(body, List.of("api", "v3.0", "shell-descriptors"))) {
            var bodyString = response.body().string();
            log.info("RESPONSE FROM DTR\n" + bodyString);
        } catch (Exception e) {

            log.error("Failed to register material " + material, e);
        }
    }
}
