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

import { MaterialRelation } from '@models/types/data/material-relation';
import { UnitOfMeasurementKey } from '@models/types/data/uom';

const MAX_SUPPLY_CHAIN_DEPTH = 20;

export type SupplyChainTreeNodeData = {
    ownMaterialNumber: string;
    depth: number;
    quantity: number;
    measurementUnit: UnitOfMeasurementKey;
    children: SupplyChainTreeNodeData[];
};

export const getSupplyChainTierLabel = (depth: number) => Array.from({ length: depth }, (_, i) => `T${i + 1}`).join('→');

function walk(
    parentOwnMaterialNumber: string,
    depth: number,
    ancestorPath: Set<string>,
    relations: MaterialRelation[]
): SupplyChainTreeNodeData[] {
    if (depth > MAX_SUPPLY_CHAIN_DEPTH) return [];
    return relations
        .filter((mr) => mr.parentOwnMaterialNumber === parentOwnMaterialNumber)
        .filter((mr) => !ancestorPath.has(mr.childOwnMaterialNumber))
        .map((mr) => ({
            ownMaterialNumber: mr.childOwnMaterialNumber,
            depth,
            quantity: mr.quantity,
            measurementUnit: mr.measurementUnit,
            children: walk(mr.childOwnMaterialNumber, depth + 1, new Set(ancestorPath).add(mr.childOwnMaterialNumber), relations),
        }));
}

export function buildSupplyChainTree(rootOwnMaterialNumber: string, relations: MaterialRelation[]): SupplyChainTreeNodeData[] {
    return walk(rootOwnMaterialNumber, 1, new Set([rootOwnMaterialNumber]), relations);
}

function flattenMaterialNumbers(nodes: SupplyChainTreeNodeData[], into: Set<string>): Set<string> {
    for (const node of nodes) {
        into.add(node.ownMaterialNumber);
        flattenMaterialNumbers(node.children, into);
    }
    return into;
}

export function getDescendantMaterialNumbers(rootOwnMaterialNumber: string, relations: MaterialRelation[]): Set<string> {
    return flattenMaterialNumbers(buildSupplyChainTree(rootOwnMaterialNumber, relations), new Set<string>());
}
