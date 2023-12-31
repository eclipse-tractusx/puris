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
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
/**
 * This class represents a request message as it is sent
 * to the counterparty.
 */
public class ItemStockRequestMessageDto {

    private HeaderDto header = new HeaderDto();
    private ContentDto content = new ContentDto();

    @Getter
    @Setter
    public static class HeaderDto {
        private UUID messageId;
        private String context;
        private String version;
        private String senderBpn;
        private String receiverBpn;
        private Date sentDateTime;
    }

    @Getter
    @Setter
    @ToString
    public static class ContentDto {
        private DirectionCharacteristic direction;
        private List<RequestDto> itemStock = new ArrayList<>();
    }
    @Getter
    @Setter
    @ToString
    public static class RequestDto {
        private String materialGlobalAssetId;
        private String materialNumberCustomer;
        private String materialNumberSupplier;
    }

    /**
     * Create an ItemStockRequestMessageDto from the
     * given ItemStockRequestMessage
     *
     * @param itemStockRequestMessage the ItemStockRequestMessage
     * @return the corresponding Dto
     */
    public static ItemStockRequestMessageDto convertToDto(ItemStockRequestMessage itemStockRequestMessage) {
        ItemStockRequestMessageDto dto = new ItemStockRequestMessageDto();
        var header = dto.getHeader();
        header.messageId = itemStockRequestMessage.getMessageId();
        header.context = itemStockRequestMessage.getContext();
        header.version = itemStockRequestMessage.getVersion();
        header.senderBpn = itemStockRequestMessage.getSenderBpn();
        header.receiverBpn = itemStockRequestMessage.getReceiverBpn();
        header.sentDateTime = itemStockRequestMessage.getSentDateTime();
        var content = dto.getContent();
        content.direction = itemStockRequestMessage.getDirection();
        for(var request : itemStockRequestMessage.getItemStock()){
            var requestDto = new RequestDto();
            requestDto.materialGlobalAssetId = request.getMaterialGlobalAssetId();
            requestDto.materialNumberCustomer = request.getMaterialNumberCustomer();
            requestDto.materialNumberSupplier = request.getMaterialNumberSupplier();
            dto.content.itemStock.add(requestDto);
        }
        return dto;
    }

    /**
     * Create an ItemStockRequestMessage from the given
     * ItemStockRequestMessageDto
     *
     * @param dto the Dto
     * @return the corresponding entity
     */
    public static ItemStockRequestMessage convertToEntity(ItemStockRequestMessageDto dto) {
        ItemStockRequestMessage entity = new ItemStockRequestMessage();
        entity.setMessageId(dto.getHeader().messageId);
        entity.setContext(dto.getHeader().context);
        entity.setVersion(dto.getHeader().version);
        entity.setSenderBpn(dto.getHeader().senderBpn);
        entity.setReceiverBpn(dto.getHeader().receiverBpn);
        entity.setSentDateTime(dto.getHeader().sentDateTime);
        entity.setDirection(dto.getContent().direction);
        for(var requestDto : dto.content.itemStock){
            ItemStockRequestMessage.Request request = new ItemStockRequestMessage.Request();
            request.setMaterialGlobalAssetId(requestDto.materialGlobalAssetId);
            request.setMaterialNumberCustomer(requestDto.materialNumberCustomer);
            request.setMaterialNumberSupplier(requestDto.materialNumberSupplier);
            entity.getItemStock().add(request);
        }
        return entity;
    }
}
