/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.stock.domain.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@Entity
@ToString
@NoArgsConstructor
/**
 * An instance of this class represents the data received in a
 * call to the itemstockrequest api.
 */
public class ItemStockRequestMessage {

    public final static String CONTEXT = "RES-PURIS-ItemStockRequest:1.0";
    public final static String VERSION = "urn:samm:io.catenax.message_header:2.0";

    @EmbeddedId
    private Key key;
    @NotNull
    private String context = CONTEXT;
    @NotNull
    private String version = VERSION;
    @Nullable
    private Date sentDateTime;
    @NotNull
    private DirectionCharacteristic direction;
    @ElementCollection
    private List<Request> itemStock = new ArrayList<>();
    @NotNull
    private DT_RequestStateEnum state = DT_RequestStateEnum.Working;

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class Key implements Serializable {
        @NotNull
        private UUID messageId;
        @NotNull
        @Pattern(regexp = PatternStore.BPNL_STRING)
        private String senderBpn;
        @NotNull
        @Pattern(regexp = PatternStore.BPNL_STRING)
        private String receiverBpn;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(messageId, key.messageId) && Objects.equals(senderBpn, key.senderBpn) && Objects.equals(receiverBpn, key.receiverBpn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, senderBpn, receiverBpn);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStockRequestMessage that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Getter
    @Setter
    @Embeddable
    @ToString
    public static class Request {
        private String materialGlobalAssetId;
        private String materialNumberCustomer;
        private String materialNumberSupplier;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Request request)) return false;
            return Objects.equals(materialGlobalAssetId, request.materialGlobalAssetId) && Objects.equals(materialNumberCustomer, request.materialNumberCustomer) && Objects.equals(materialNumberSupplier, request.materialNumberSupplier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialGlobalAssetId, materialNumberCustomer, materialNumberSupplier);
        }
    }

}
