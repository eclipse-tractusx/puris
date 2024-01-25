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

package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRequestMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
/**
 * This class is a service to store and retrieve item stock request messages and their status.
 */
public class ItemStockRequestMessageService {
    @Autowired
    private ItemStockRequestMessageRepository repository;

    public ItemStockRequestMessage create(ItemStockRequestMessage itemStockRequestMessage) {
        if (!validate(itemStockRequestMessage)) {
            return null;
        }
        if (repository.findById(itemStockRequestMessage.getKey()).isPresent()) {
            log.warn("Message already exists, cannot create message \n" + itemStockRequestMessage);
            return null;
        }
        return repository.save(itemStockRequestMessage);
    }

    public ItemStockRequestMessage update(ItemStockRequestMessage itemStockRequestMessage) {
        if (!validate(itemStockRequestMessage)) {
            return null;
        }
        if (repository.findById(itemStockRequestMessage.getKey()).isEmpty()) {
            log.warn("Cannot update message, message didn't exist before \n" + itemStockRequestMessage);
            return null;
        }
        return repository.save(itemStockRequestMessage);
    }

    public ItemStockRequestMessage find(ItemStockRequestMessage itemStockRequestMessage) {
        if(itemStockRequestMessage.getKey() == null) {
            return null;
        }
        return repository.findById(itemStockRequestMessage.getKey()).orElse(null);
    }

    public ItemStockRequestMessage find(ItemStockRequestMessage.Key key) {
        if(key == null) {
            return null;
        }
        return repository.findById(key).orElse(null);
    }

    private boolean validate(ItemStockRequestMessage itemStockRequestMessage) {
        try {
            Objects.requireNonNull(itemStockRequestMessage.getKey(), "Missing key");
            Objects.requireNonNull(itemStockRequestMessage.getKey().getMessageId(), "Missing MessageId");
            Objects.requireNonNull(itemStockRequestMessage.getKey().getReceiverBpn(), "Missing receiverBpnl");
            Objects.requireNonNull(itemStockRequestMessage.getKey().getSenderBpn(), "Missing senderBpnl");
            Objects.requireNonNull(itemStockRequestMessage.getDirection(), "Missing direction");
            Objects.requireNonNull(itemStockRequestMessage.getContext(), "Missing context");
            Objects.requireNonNull(itemStockRequestMessage.getVersion(), "Missing version");
            Objects.requireNonNull(itemStockRequestMessage.getState(), "Missing state");
            if (itemStockRequestMessage.getState() == DT_RequestStateEnum.Requested || itemStockRequestMessage.getState() == DT_RequestStateEnum.Completed
                || itemStockRequestMessage.getState() == DT_RequestStateEnum.Received) {
                Objects.requireNonNull(itemStockRequestMessage.getSentDateTime(), "Missing sendDateTime in state " + itemStockRequestMessage.getState());
            }
        } catch (Exception e) {
            log.error("Validation failed ", e);
            return false;
        }
        return true;
    }
}
