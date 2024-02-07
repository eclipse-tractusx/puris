/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

import { useTransfers } from '@hooks/edc/useTransfers';

export const TransferView = () => {
    const { transfers } = useTransfers();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Transfers</h1>
            <ul>
                {transfers && transfers.length > 0 ? (
                    transfers.map((transfer) => (
                        <li>
                            <h2> Transfer Id: {transfer['@id']}</h2>
                            <span> Correlation Id: {transfer['edc:correlationId']}</span>
                            <span> State: {transfer['edc:state']}</span>
                            <span> State Timestamp: {transfer['edc:stateTimestamp']}</span>
                            <span> Type: {transfer['edc:type']}</span>
                            <span> Asset Id: {transfer['edc:assetId']}</span>
                            <span> Contract Id: {transfer['edc:contractId']}</span>
                            <span> Connector Id: {transfer['edc:connectorId']}</span>
                        </li>
                    ))
                ) : (
                    <p>No transfers found</p>
                )}
            </ul>
        </div>
    );
}
