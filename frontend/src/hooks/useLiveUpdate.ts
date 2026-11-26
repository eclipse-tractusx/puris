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

import { useCallback, useEffect, useRef } from 'react';
import { useStompClient, useSubscription } from 'react-stomp-hooks';

/**
 * Calls onUpdate whenever a change is announced on one of the given websocket topics.
 * Events arriving in quick succession are debounced into a single call.
 *
 * Events sent while the connection is down are lost, so onUpdate is also called whenever the
 * connection becomes available after the hook mounted (reconnect, or the initial connect when the
 * component mounted before it). This brings the data back in sync without a manual reload.
 */
export const useLiveUpdate = (topics: string | string[], onUpdate: () => void, delay: number = 500) => {
    const onUpdateRef = useRef(onUpdate);
    const timeoutRef = useRef<ReturnType<typeof setTimeout>>();
    // the client is only provided while the connection is established
    const isConnected = useStompClient() !== undefined;
    const wasConnectedRef = useRef(isConnected);

    useEffect(() => {
        onUpdateRef.current = onUpdate;
    }, [onUpdate]);

    useEffect(() => () => clearTimeout(timeoutRef.current), []);

    const scheduleUpdate = useCallback(() => {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => onUpdateRef.current(), delay);
    }, [delay]);

    useEffect(() => {
        if (isConnected && !wasConnectedRef.current) {
            scheduleUpdate();
        }
        wasConnectedRef.current = isConnected;
    }, [isConnected, scheduleUpdate]);

    useSubscription(topics, scheduleUpdate);
};
