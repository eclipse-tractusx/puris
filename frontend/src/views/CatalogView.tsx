/*
Copyright (c) 2022,2024 Volkswagen AG
Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022,2024 Contributors to the Eclipse Foundation

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

import { Input, LoadingButton } from '@catena-x/portal-shared-components';
import { useCatalog } from '@hooks/edc/useCatalog';
import { useRef, useState } from 'react';
import { CatalogOperation } from '@models/types/edc/catalog';
import { Card } from '@mui/material';
import { getCatalogOperator } from '@util/helpers';

type OperationListProps = {
    title: string;
    operations: CatalogOperation[];
};

const OperationList = ({ title, operations }: OperationListProps) => {
    return (
        <>
            <span className="font-semibold">{title}</span>
            {operations && operations.length > 0 ? (
                <ul>
                    {operations.map((operation, index) => (
                        <li key={index}>
                            {`${operation['odrl:constraint']['odrl:leftOperand']} ${getCatalogOperator(
                                operation['odrl:constraint']['odrl:operator']['@id']
                            )} ${operation['odrl:constraint']['odrl:rightOperand']}`}
                        </li>
                    ))}
                </ul>
            ) : (
                <p>None</p>
            )}
        </>
    );
};

export const CatalogView = () => {
    const [edcUrl, setEdcUrl] = useState<string | null>(null);
    const { catalog, catalogError, isLoadingCatalog } = useCatalog(edcUrl);
    const urlRef = useRef<string | null>(null);
    return (
        <div className="flex flex-col items-center gap-4 w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700">View EDC Catalog</h1>
            <div className="flex items-end gap-5">
                <Input
                    label="EDC URL"
                    type="text"
                    id="edc-url"
                    placeholder="Enter URL"
                    margin="none"
                    onChange={(event) => (urlRef.current = event.target.value)}
                />
                <div className="mb-3">
                    <LoadingButton
                        label="Get Catalog"
                        loadIndicator="Loading..."
                        loading={isLoadingCatalog}
                        onButtonClick={() => setEdcUrl(urlRef?.current)}
                    />
                </div>
            </div>
            {catalog && catalog.length > 0 ? (
                <ul className="flex flex-col gap-5 w-[64rem]">
                    {catalog.map((item, index) => (
                        <Card className="p-5">
                            <li key={index}>
                                <h2 className="text-xl font-semibold">Catalog Item</h2>
                                <div className="flex w-full justify-start gap-4">
                                    <div className="flex flex-col gap-1 w-[70ch]">
                                        <div className="flex">
                                            <h3 className="font-semibold w-[20ch]">Asset ID: </h3>"{item.assetId}"
                                        </div>
                                        <div className="flex">
                                            <h4 className="font-semibold w-[20ch]">Asset type: </h4>"{item.assetType}"
                                        </div>
                                        <div className="flex">
                                            <h4 className="font-semibold w-[20ch]">Asset action: </h4>
                                            {item.permission['odrl:action']['odrl:type']} {item.permission['odrl:target']}
                                        </div>
                                        <div className="flex">
                                            <h4 className="font-semibold w-[20ch]">Asset condition(s): </h4>
                                            <div className="flex flex-col gap-1 w-[30ch]">
                                                {'odrl:and' in item.permission['odrl:constraint'] ?
                                                    (item.permission['odrl:constraint']['odrl:and'].map(constraint => (
                                                        <div className="flex">
                                                            {constraint['odrl:leftOperand'] + ' '}
                                                            {getCatalogOperator(constraint['odrl:operator']['@id']) + ' '}
                                                            {constraint['odrl:rightOperand']}
                                                        </div>
                                                    ))) :
                                                    (
                                                        <div className="flex">
                                                            {item.permission['odrl:constraint']['odrl:leftOperand'] + ' '}
                                                            {getCatalogOperator(item.permission['odrl:constraint']['odrl:operator']['@id']) + ' '}
                                                            {item.permission['odrl:constraint']['odrl:rightOperand']}
                                                        </div>
                                                    )
                                                }
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex flex-col w-1/3 flex-shrink-0">
                                        <OperationList title="The following prohibitions are defined:" operations={item.prohibitions} />
                                        <OperationList title="The following obligations are defined:" operations={item.obligations} />
                                    </div>
                                </div>
                            </li>
                        </Card>
                    ))}
                </ul>
            ) : catalogError != null ? (
                <div className="text-red-500 py-5">There was an error retrieving the Catalog from {edcUrl}</div>
            ) : (
                <div className="py-5"> {`No Catalog available for ${edcUrl}`} </div>
            )}
        </div>
    );
};
