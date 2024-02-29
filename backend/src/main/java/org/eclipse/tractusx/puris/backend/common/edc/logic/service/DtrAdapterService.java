package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.DtrRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DtrAdapterService {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DtrRequestBodyBuilder dtrRequestBodyBuilder;

    private Response sendDtrPostRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
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

    private Response sendDtrPutRequest(JsonNode requestBody, List<String> pathSegments) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));
        var request = new Request.Builder()
            .put(body)
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .build();
        return CLIENT.newCall(request).execute();
    }

    private Response sendDtrGetRequest(List<String> pathSegments, Map<String, String> headerData, Map<String, String> queryParams) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(variablesService.getDtrUrl()).newBuilder();
        for (var pathSegment : pathSegments) {
            urlBuilder.addPathSegment(pathSegment);
        }
        if (queryParams != null) {
            for (var queryParam : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(queryParam.getKey(), queryParam.getValue());
            }
        }
        var requestBuilder = new Request.Builder().url(urlBuilder.build());
        if (headerData != null) {
            for (var header : headerData.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }
        return CLIENT.newCall(requestBuilder.build()).execute();
    }

    private String getAasForMaterial(String idAsBase64) {
        try (var response = sendDtrGetRequest(List.of("api", "v3.0", "shell-descriptors", idAsBase64), Map.of("Edc-Bpn", variablesService.getOwnBpnl()), Map.of())){
                var bodyString = response.body().string();
                log.info("Response Code " + response.code());
                if(response.isSuccessful()) {
                    return bodyString;
                }

        } catch (Exception e) {
            log.error("Failed to retrieve DTR from ");
        }
        return null;
    }

    public boolean updateMaterialForMaterialPartnerRelation(MaterialPartnerRelation materialPartnerRelation) {
        Material material = materialPartnerRelation.getMaterial();
        String idAsBase64 = Base64.getEncoder().encodeToString(material.getMaterialNumberCx().getBytes(StandardCharsets.UTF_8));
        var result = getAasForMaterial(idAsBase64);
        if (result == null) {
            return false;
        }
        try {
            var updatedBody = dtrRequestBodyBuilder.injectMaterialPartnerRelation(materialPartnerRelation, result);
            try (var response = sendDtrPutRequest(updatedBody, List.of("api", "v3.0", "shell-descriptors", idAsBase64))) {
                var bodyString = response.body().string();
                log.info("Response Code " + response.code());
                if(response.isSuccessful()) {
                    return true;
                }
                log.error("Failure in update for " + materialPartnerRelation + "\n" + bodyString);
            }
        } catch (Exception e) {
            log.error("Failure in update for " + materialPartnerRelation, e);
        }
        return false;
    }

    public boolean registerMaterialAtDtr(Material material) {
        var body = dtrRequestBodyBuilder.createMaterialRegistrationRequestBody(material);
        try (var response = sendDtrPostRequest(body, List.of("api", "v3.0", "shell-descriptors"))) {
            var bodyString = response.body().string();
            if (response.isSuccessful()) {
                return true;
            }
            log.error("Failed to register material at DTR " + material.getOwnMaterialNumber() + "\n" + bodyString);
            return false;
        } catch (Exception e) {
            log.error("Failed to register material at DTR " + material.getOwnMaterialNumber(), e);
            return false;
        }
    }
}
