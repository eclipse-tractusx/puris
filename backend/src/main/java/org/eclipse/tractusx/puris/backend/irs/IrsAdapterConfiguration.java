/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.irs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class IrsAdapterConfiguration {

    /**
     * The IRS use case identifier used for all PURIS IRS requests (job creation and
     * chain opening grants).
     */
    public static final String PURIS_USE_CASE = "PURIS_ITEM_STOCK_ANONYMIZED_RECURSIVE";

    /**
     * The IRS API path for policy management (creation, deletion, etc.).
     */
    public static final String POLICIES_PATH = "irs/policies";

    /**
     * Toggles usage of IRS adapter.
     */
    @Value("${puris.irsadapter.enabled}")
    private boolean irsAdapterEnabled;

    /**
     * Base URL of the external IRS service.
     */
    @Value("${puris.irsadapter.url}")
    private String irsAdapterUrl;

    /**
     * Header key used for IRS API authentication.
     */
    @Value("${puris.irsadapter.authkey}")
    private String irsAdapterAuthKey;

    /**
     * Header value used for IRS API authentication.
     */
    @Value("${puris.irsadapter.adminauthsecret}")
    private String irsAdapterAuthSecret;

    /**
     * How often (in seconds) the IRS request queue worker checks for due requests.
     */
    @Value("${puris.irsadapter.queue.poll-interval-seconds}")
    private long queuePollIntervalSeconds;

    /**
     * Maximum number of attempts (including the first) made for a queued IRS request before it
     * is marked permanently failed.
     */
    @Value("${puris.irsadapter.queue.max-attempts}")
    private int queueMaxAttempts;

    /**
     * Delay (in seconds) before the first retry of a failed queued IRS request.
     */
    @Value("${puris.irsadapter.queue.initial-retry-delay-seconds}")
    private long queueInitialRetryDelaySeconds;

    /**
     * Multiplier applied to the retry delay after each failed attempt.
     */
    @Value("${puris.irsadapter.queue.backoff-multiplier}")
    private double queueBackoffMultiplier;

    /**
     * Upper bound (in seconds) on the delay between two attempts of a queued IRS request.
     */
    @Value("${puris.irsadapter.queue.max-retry-delay-seconds}")
    private long queueMaxRetryDelaySeconds;
}