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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/
package org.eclipse.tractusx.puris.backend.common.liveupdate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards {@link LiveUpdateEvent}s to the websocket topic of their type.
 * <p>
 * Events published within a transaction are only forwarded after it has committed, so clients
 * never refetch data that is not yet (or never) visible. Events published outside of a
 * transaction (e.g. after a plain repository save, which commits on its own) are forwarded
 * immediately.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LiveUpdateWebSocketForwarder {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void forward(LiveUpdateEvent event) {
        try {
            messagingTemplate.convertAndSend(event.type().getTopic(), event);
        } catch (Exception e) {
            // a failed live update must never affect the change that triggered it
            log.warn("Failed to publish live update {} to {}", event, event.type().getTopic(), e);
        }
    }
}
