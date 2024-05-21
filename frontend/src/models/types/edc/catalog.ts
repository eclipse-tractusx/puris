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

export type CatalogOperation = {
    'odrl:constraint': {
        'odrl:leftOperand': string;
        'odrl:operator': {
            '@id': string;
        };
        'odrl:rightOperand': string;
    };
};

export type CatalogPermission = {
    'odrl:target': string;
    'odrl:action': {
        'odrl:type': string;
    }
    'odrl:constraint': {
        'odrl:leftOperand': string;
        'odrl:operator': {
            '@id': string;
        };
        'odrl:rightOperand': string;
    } | {
        '@type': string,
        'odrl:and': {
            '@type': string,
            'odrl:leftOperand': string;
            'odrl:operator': {
                '@id': string;
            };
            'odrl:rightOperand': string;
        }[]
    };
};

export type RawCatalogData = {
    'dcat:dataset': {
        '@id': string;
        'https://purl.org/dc/terms/type': {
            '@id': string;
        }
        'https://w3id.org/catenax/ontology/common#version': string;
        'odrl:hasPolicy': {
            'odrl:permission': CatalogPermission;
            'odrl:prohibition': CatalogOperation[];
            'odrl:obligation': CatalogOperation[];
        };
    }[];
};
