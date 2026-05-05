/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.common.industrycore;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageContext {
    DEMAND_AND_CAPACITY_NOTIFICATION_CONTEXT("CX-DemandAndCapacityNotificationAPI-Receive:2.0.0", "demandAndCapacityNotification"),
    DATA_EXCHANGE_REQUEST_CONTEXT("CX-DataExchangeRequestAPI-RequestReceive:1.0.0", null);

    private final String value;
    private final String contentKey;

    MessageContext(String value, String contentKey) {
        this.value = value;
        this.contentKey = contentKey;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getContentKey() {
        return contentKey;
    }
}
