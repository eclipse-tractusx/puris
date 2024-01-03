/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;

import java.util.*;

@Getter
@Setter
@Entity
@ToString
@NoArgsConstructor
public class ItemStockRequestMessage {

    public final static String CONTEXT = "RES-PURIS-ItemStockRequest:1.0";
    public final static String VERSION = "urn:samm:io.catenax.message_header:2.0";

    @NotNull
    @Id
    @GeneratedValue
    private UUID messageId;
    @NotNull
    private String context = CONTEXT;
    @NotNull
    private String version = VERSION;
    @NotNull
    @Pattern(regexp = Partner.BPNL_REGEX)
    private String senderBpn;
    @NotNull
    @Pattern(regexp = Partner.BPNL_REGEX)
    private String receiverBpn;
    @Nullable
    private Date sentDateTime;
    @NotNull
    private DirectionCharacteristic direction;
    @ElementCollection
    private List<Request> itemStock = new ArrayList<>();
    @NotNull
    private DT_RequestStateEnum state = DT_RequestStateEnum.Working;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStockRequestMessage that)) return false;
        return Objects.equals(messageId, that.messageId) && Objects.equals(context, that.context) && Objects.equals(version, that.version) && Objects.equals(senderBpn, that.senderBpn) && Objects.equals(receiverBpn, that.receiverBpn) && Objects.equals(sentDateTime, that.sentDateTime) && direction == that.direction && Objects.equals(itemStock, that.itemStock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, context, version, senderBpn, receiverBpn, sentDateTime, direction, itemStock);
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
