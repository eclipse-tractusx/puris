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

SPDX-License-Identifier: Apache-2.0
*/

import { useEffect, useState } from 'react';
import { Material } from '@models/types/data/stock';
import { getMaterialNumbersMapping } from '@services/material-numbers-mapping-service';

export const useMaterialNumbersMapping = (materials: Material[] | null) => {
    const [mappingByOwnMaterialNumber, setMappingByOwnMaterialNumber] = useState<Map<string, Record<string, string>>>(new Map());

    useEffect(() => {
        const ownMaterialNumbers = (materials ?? [])
            .map((m) => m.ownMaterialNumber)
            .filter((n): n is string => Boolean(n));
        if (ownMaterialNumbers.length === 0) {
            return;
        }
        let cancelled = false;
        Promise.all(
            ownMaterialNumbers.map((ownMaterialNumber) =>
                getMaterialNumbersMapping(ownMaterialNumber)
                    .then((mapping) => [ownMaterialNumber, mapping] as const)
                    .catch(() => [ownMaterialNumber, {}] as const)
            )
        ).then((results) => {
            if (!cancelled) {
                setMappingByOwnMaterialNumber(new Map(results));
            }
        });
        return () => {
            cancelled = true;
        };
    }, [materials]);

    return mappingByOwnMaterialNumber;
};
