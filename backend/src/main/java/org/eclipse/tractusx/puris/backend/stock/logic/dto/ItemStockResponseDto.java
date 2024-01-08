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

package org.eclipse.tractusx.puris.backend.stock.logic.dto;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
/**
 * This class represents a response message as it is sent
 * to the counterparty.
 */
public class ItemStockResponseDto {

    private HeaderDto header = new HeaderDto();
    private ContentDto content = new ContentDto();

    @Getter
    @Setter
    public static class HeaderDto {
        private UUID messageId;
        private UUID relatedMessageId;
        private String context;
        private String version;
        private String senderBpn;
        private String receiverBpn;
        private Date sentDateTime;
    }

    @Getter
    @Setter
    public static class ContentDto {
        List<ItemStockSamm> itemStock = new ArrayList<>();
    }
}
