/*
 * Copyright (c) 2026 Volkswagen AG
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class IrsRequestService {

	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

	private final OkHttpClient client = new OkHttpClient();

	private final IrsAdapterConfiguration irsAdapterConfiguration;

	public boolean isEnabled() {
		return irsAdapterConfiguration.isIrsAdapterEnabled();
	}

	public IrsResponse get(String path, Map<String, String> queryParams) {
		return execute("GET", path, queryParams, null);
	}

	public IrsResponse post(String path, String jsonBody) {
		return execute("POST", path, null, jsonBody);
	}

	public IrsResponse put(String path, String jsonBody) {
		return execute("PUT", path, null, jsonBody);
	}

	public IrsResponse delete(String path, Map<String, String> queryParams) {
		return execute("DELETE", path, queryParams, null);
	}

	protected IrsResponse execute(
		String method,
		String path,
		Map<String, String> queryParams,
		String jsonBody
	) {
		ensureIrsAdapterEnabled(method, path);

		HttpUrl url = buildUrl(path, queryParams);
		Request.Builder requestBuilder = new Request.Builder().url(url);

		requestBuilder.header(irsAdapterConfiguration.getIrsAdapterAuthKey(), irsAdapterConfiguration.getIrsAdapterAuthSecret());

		switch (method) {
			case "GET" -> requestBuilder.get();
			case "DELETE" -> requestBuilder.delete();
			case "POST", "PUT" -> {
				String payload = jsonBody == null ? "{}" : jsonBody;
				RequestBody body = RequestBody.create(payload, JSON_MEDIA_TYPE);
				if ("POST".equals(method)) {
					requestBuilder.post(body);
				} else {
					requestBuilder.put(body);
				}
				requestBuilder.header("Content-Type", "application/json");
			}
			default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
		}

		try (var response = client.newCall(requestBuilder.build()).execute()) {
			String body = response.body() != null ? response.body().string() : "";
			return IrsResponse.builder()
				.statusCode(response.code())
				.responseBody(body)
				.successful(response.code() >= 200 && response.code() < 300)
				.build();
		} catch (IOException e) {
			return IrsResponse.builder()
				.statusCode(500)
				.responseBody("Error while calling IRS endpoint " + path + ": " + e.getMessage())
				.successful(false)
				.build();
		}
	}

	private void ensureIrsAdapterEnabled(String method, String path) {
		if (!isEnabled()) {
			String message = "IRS integration is disabled. Skipping HTTP " + method + " call to " + path;
			log.warn(message);
			throw new IllegalStateException(message);
		}
	}

	private HttpUrl buildUrl(String path, Map<String, String> queryParams) {
		String baseUrl = irsAdapterConfiguration.getIrsAdapterUrl();
		String normalizedPath = path == null ? "" : path;

		HttpUrl base = HttpUrl.parse(baseUrl);
		if (base == null) {
			throw new IllegalStateException("Invalid IRS base URL configuration: " + baseUrl);
		}

		HttpUrl.Builder urlBuilder = base.newBuilder();
		String cleanedPath = normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
		if (!cleanedPath.isEmpty()) {
			for (String segment : cleanedPath.split("/")) {
				if (!segment.isEmpty()) {
					urlBuilder.addPathSegment(segment);
				}
			}
		}

		if (queryParams != null) {
			queryParams.forEach(urlBuilder::addQueryParameter);
		}

		return urlBuilder.build();
	}

	@Getter
	@Builder
	public static class IrsResponse {
		private final int statusCode;
		private final String responseBody;
		private final boolean successful;
	}
}
