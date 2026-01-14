/*
Copyright (c) 2026 Volkswagen AG
Copyright (c) 2026 Contributors to the Eclipse Foundation

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

import { config } from '@models/constants/config';

export function withDefaultProductionTime(date: Date | null): Date | undefined {
    if (!date || isNaN(date.getTime())) {
        return undefined;
    }
    const dateCopy = new Date(date);
    const hasTime = dateCopy.getHours() !== 0 || dateCopy.getMinutes() !== 0;
    if (!hasTime) {
        const [hours, minutes] = getDefaultProductionTimeNumbers();
        dateCopy.setHours(hours, minutes, 0, 0);
    }
    return dateCopy;
}

export function getDefaultProductionTimeNumbers(): [number, number] {
    return config.app.DEFAULT_PRODUCTION_TIME.split(":").map(Number) as [number, number];
}
