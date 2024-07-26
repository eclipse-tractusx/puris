/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.erpadapter;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ErpAdapterConfiguration {

    /**
     * Toggles usage of the ERP adapter
     */
    @Value("${puris.erpadapter.enabled}")
    private boolean erpAdapterEnabled;

    /**
     * The URL of the ERP adapter
     */
    @Value("${puris.erpadapter.url}")
    private String erpAdapterUrl;

    /**
     * The URL under which we expect responses from
     * the ERP adapter
     */
    @Value("${puris.baseurl}" + "${server.servlet.context-path}" + "/erp-adapter")
    private String erpResponseUrl;

    /**
     * The auth-key used when accessing the ERP adapter's
     * request interface
     */
    @Value("${puris.erpadapter.authkey}")
    private String erpAdapterAuthKey;

    /**
     * The auth-secret used when accessing the ERP adapter's
     * request interface
     */
    @Value("${puris.erpadapter.authsecret}")
    private String erpAdapterAuthSecret;

    @Value("${puris.erpadapter.refreshinterval}")
    @Getter(AccessLevel.NONE)
    private long refreshInterval;

    @Value("${puris.erpadapter.timelimit}")
    @Getter(AccessLevel.NONE)
    private long refreshTimeLimit;

    /**
     * Period since last received partner request after which no more new update requests to the
     * erp adapter will be sent (milliseconds).
     * That means: Adding this period to the date and time of the last received request results in that
     * point in time, when the ErpAdapterTriggerService assumes, that this specific kind of request
     * is no longer relevant and will stop issuing scheduled update requests to the ErpAdapter.
     *
     * <p>
     * Example: Let's assume we have set this variable to the equivalent of seven days (in milliseconds).
     * Let's also assume that we have received a request from a specific partner for a specific material
     * and a specific submodel (and possibly also a specific direction characteristic) on May 15 10:39:21 GMT 2024.
     *
     * <p>
     * Then the ErpAdapterTriggerService will issue scheduled requests for new updates from the ErpAdapter, for at least seven days.
     *
     * After seven days (i.e. at or a few seconds after May 22 10:39:21 GMT 2024), and, of course
     * assuming that we didn't receive any requests with the exact same specifics from the same partner in the meantime,
     * then no more scheduled requests with these specifics will be sent out to the ErpAdapter.
     *
     *
     * @return the time period
     */
    public long getRefreshTimeLimit() {
        // translate days to milliseconds
        return refreshTimeLimit * 24 * 60 * 60 * 1000;
    }

    /**
     * Interval between two scheduled requests to the erp adapter for the same issue (milliseconds)
     * <p>
     * Example: Let's assume, that this variable is set to the equivalent of 3 hours (in milliseconds)
     * Let's also assume that we have received a request from a specific partner for a specific material
     * and a specific submodel (and possibly also a specific direction characteristic) on May 15 10:39:21 GMT 2024.
     * <p>
     * Then ErpAdapterTriggerService will schedule the next request to the ErpAdapter with the specifics of that aforementioned
     * request at or a few seconds after May 15 13:39:21 GMT 2024.
     *
     * These update requests will perpetuate with the given interval, for as long as the refreshTimeLimit has not expired.
     *
     * @return the interval
     */
    public long getRefreshInterval() {
        // translate minutes to milliseconds
        return refreshInterval * 60 * 1000;
    }
}
