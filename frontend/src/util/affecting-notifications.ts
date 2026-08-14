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

import { DemandCapacityNotification } from '@models/types/data/demand-capacity-notification';
import { MaterialRelation } from '@models/types/data/material-relation';
import { getDescendantMaterialNumbers } from './supply-chain-relations';

export type DemandCapacityNotificationImpact = {
    notification: DemandCapacityNotification;
    viaChildMaterialNumbers: string[];
};

export function getAffectingNotifications(
    ownMaterialNumber: string,
    isOutbound: boolean,
    openNotifications: DemandCapacityNotification[],
    materialRelations: MaterialRelation[]
): DemandCapacityNotificationImpact[] {
    if (isOutbound) {
        const descendantMaterialNumbers = getDescendantMaterialNumbers(ownMaterialNumber, materialRelations);
        return openNotifications
            .map((notification) => ({
                notification,
                viaChildMaterialNumbers: notification.affectedMaterialNumbers?.filter((m) => descendantMaterialNumbers.has(m)) ?? [],
            }))
            .filter(({ viaChildMaterialNumbers }) => viaChildMaterialNumbers.length > 0);
    }
    return openNotifications
        .filter((n) => n.affectedMaterialNumbers?.includes(ownMaterialNumber))
        .map((notification) => ({ notification, viaChildMaterialNumbers: [] as string[] }));
}
