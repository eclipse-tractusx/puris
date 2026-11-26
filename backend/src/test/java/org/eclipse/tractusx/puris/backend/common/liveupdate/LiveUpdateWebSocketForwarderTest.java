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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LiveUpdateWebSocketForwarderTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private LiveUpdateWebSocketForwarder forwarder;

    @Test
    void forward_sendsEventToTopicOfItsType() {
        var notificationEvent = new LiveUpdateEvent(LiveUpdateType.NOTIFICATION, LiveUpdateAction.CREATED);
        var requestEvent = new LiveUpdateEvent(LiveUpdateType.DATA_EXCHANGE_REQUEST, LiveUpdateAction.UPDATED);
        var approvalEvent = new LiveUpdateEvent(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.CREATED);

        forwarder.forward(notificationEvent);
        forwarder.forward(requestEvent);
        forwarder.forward(approvalEvent);

        verify(messagingTemplate).convertAndSend("/topic/notifications", notificationEvent);
        verify(messagingTemplate).convertAndSend("/topic/data-exchange-requests", requestEvent);
        verify(messagingTemplate).convertAndSend("/topic/data-exchange-approvals", approvalEvent);
    }

    @Test
    void forward_whenBrokerFails_doesNotThrow() {
        doThrow(new MessagingException("broker unavailable")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertDoesNotThrow(() -> forwarder.forward(new LiveUpdateEvent(LiveUpdateType.NOTIFICATION, LiveUpdateAction.DELETED)));
    }

    @Test
    void event_serializesToTypeAndActionOnly() throws Exception {
        var json = new ObjectMapper().writeValueAsString(new LiveUpdateEvent(LiveUpdateType.DATA_EXCHANGE_APPROVAL, LiveUpdateAction.UPDATED));

        assertEquals(Map.of("type", "DATA_EXCHANGE_APPROVAL", "action", "UPDATED"), new ObjectMapper().readValue(json, Map.class));
    }
}
