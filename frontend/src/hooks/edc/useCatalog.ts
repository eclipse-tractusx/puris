/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

import {config} from '@models/constants/config';
import {CatalogConstraint, CatalogItem, CatalogOperation, CatalogPermission, RawCatalogData} from '@models/types/edc/catalog';
import {useFetch} from '../useFetch';
import {isErrorResponse} from '@util/helpers';
import {Partner} from '@models/types/edc/partner';

export const useCatalog = (partner: Partner | null) => {
    const {
        data,
        error,
        isLoading: isLoadingCatalog,
    } = useFetch<RawCatalogData>(partner ? config.app.BACKEND_BASE_URL +
        'edc/catalog?dspUrl=' + encodeURIComponent(partner.edcUrl) +
        '&partnerBpnl=' + encodeURIComponent(partner.bpnl) : undefined);
    const catalog: CatalogItem[] | null = data && !isErrorResponse(data) ? normalizeCatalog(data) : null;
    const catalogError = error ?? (data && isErrorResponse(data) ? data : null);
    return {
        catalog,
        catalogError,
        isLoadingCatalog,
    };
};

const stripPrefixes = (value: unknown): any => {
    if (Array.isArray(value)) return value.map(stripPrefixes);
    if (value && typeof value === 'object') {
        return Object.fromEntries(
            Object.entries(value).map(([k, v]) => [k.replace(/^(odrl|dcat):/, ''), stripPrefixes(v)])
        );
    }
    return value;
};

const idOf = (ref: any): string => (typeof ref === 'string' ? ref : ref?.['@id'] ?? '');
const localName = (value: string): string => value.split(/[/#:]/).pop() ?? value;
const first = <T,>(v: T | T[] | undefined): T | undefined => (Array.isArray(v) ? v[0] : v);
const toArray = <T,>(v: T | T[] | undefined | null): T[] => (v == null ? [] : Array.isArray(v) ? v : [v]);

const parseExpr = (c: any): CatalogConstraint => ({
    leftOperand: localName(idOf(c.leftOperand)),
    operator: localName(idOf(c.operator)),
    rightOperand: idOf(c.rightOperand),
});
const parseConstraints = (constraint: any): CatalogConstraint[] =>
    toArray(constraint).flatMap((c) => {
        if (c?.and) return c.and.map(parseExpr);
        if (c?.or) return c.or.map(parseExpr);
        return c?.leftOperand != null ? [parseExpr(c)] : [];
    });
const parsePermission = (permission: any): CatalogPermission | null => {
    const p = first(permission);
    return p == null ? null : { action: localName(idOf(p.action)), constraints: parseConstraints(p.constraint) };
};
const parseOps = (ops: any): CatalogOperation[] =>
    toArray(ops).map((op) => ({ constraints: parseConstraints(op.constraint) }));

export const normalizeCatalog = (raw: RawCatalogData): CatalogItem[] => {
    const data = stripPrefixes(raw);
    const datasets: any[] = data.dataset ?? [];
    return datasets.map((item) => {
        const policy = first(item.hasPolicy);
        return {
            assetId: item['@id'],
            assetType: item['dct:type']?.['@id'] ?? '',
            assetVersion: item['https://w3id.org/catenax/ontology/common#version'] ?? '',
            permission: parsePermission(policy?.permission),
            prohibitions: parseOps(policy?.prohibition),
            obligations: parseOps(policy?.obligation),
        };
    });
};