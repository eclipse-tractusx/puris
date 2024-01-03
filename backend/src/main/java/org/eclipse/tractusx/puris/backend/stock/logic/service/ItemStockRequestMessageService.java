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

package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRequestMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ItemStockRequestMessageService {
    @Autowired
    private ItemStockRequestMessageRepository repository;

    public ItemStockRequestMessage create(ItemStockRequestMessage itemStockRequestMessage) {
        if (itemStockRequestMessage.getMessageId() != null) {
            log.error("MessageId already exists, could not create entity \n" + itemStockRequestMessage);
            return null;
        }
        return repository.save(itemStockRequestMessage);
    }

    public ItemStockRequestMessage update(ItemStockRequestMessage itemStockRequestMessage) {
        if (itemStockRequestMessage.getMessageId() == null) {
            log.error("MessageId missing, could not update entity \n" + itemStockRequestMessage);
            return null;
        }
        if (find(itemStockRequestMessage.getMessageId()) == null) {
            log.error("Unknown MessageId, could not update entity \n" + itemStockRequestMessage);
            return null;
        }
        return repository.save(itemStockRequestMessage);
    }

    public ItemStockRequestMessage find(UUID messageId) {
        return repository.findById(messageId).orElse(null);
    }


}
