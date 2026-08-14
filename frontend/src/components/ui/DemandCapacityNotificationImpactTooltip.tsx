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

import { ReactElement } from 'react';
import { Tooltip } from '@mui/material';
import { EFFECTS } from '@models/constants/effects';
import { DemandCapacityNotificationImpact } from '@util/affecting-notifications';

type DemandCapacityNotificationImpactTooltipProps = {
    impacts: DemandCapacityNotificationImpact[];
    children: ReactElement;
};

export function DemandCapacityNotificationImpactTooltip({ impacts, children }: DemandCapacityNotificationImpactTooltipProps) {
    return (
        <Tooltip
            arrow
            title={
                <>
                    {impacts.map(({ notification, viaChildMaterialNumbers }, index) => (
                        <div key={notification.uuid ?? index}>
                            {EFFECTS.find((e) => e.key === notification.effect)?.value ?? notification.effect}
                            {viaChildMaterialNumbers.length > 0 && ` (via component ${viaChildMaterialNumbers.join(', ')})`}
                        </div>
                    ))}
                </>
            }
        >
            {children}
        </Tooltip>
    );
}
