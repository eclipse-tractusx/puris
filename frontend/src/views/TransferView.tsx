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

import Card from '@mui/material/Card';

import {useTransfers} from '@hooks/edc/useTransfers';
import {Transfer} from '@models/types/edc/transfer';

type TransferCardProps = {
    transfer: Transfer;
};

const TransferCard = ({transfer}: TransferCardProps) => {
    return (
        <Card className="p-5">
            <h2 className="text-xl font-semibold mb-3">Transfer</h2>
            <div className="flex w-full flex-col gap-4">
                <div className="flex flex-col gap-2">
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Transfer Id: </span>
                        {transfer['@id']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Correlation Id: </span>
                        {transfer['correlationId']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> State: </span>
                        {transfer['state']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> State Timestamp: </span>
                        {new Date(transfer['stateTimestamp']).toLocaleString()}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Type: </span>
                        {transfer['type']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Asset Id: </span>
                        {transfer['assetId']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Contract Id: </span>
                        <span className="break-all w-[60ch]">{transfer['contractId']}</span>
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Connector Id: </span>
                        {transfer['connectorId']}
                    </div>
                </div>
            </div>
        </Card>
    );
};

export const TransferView = () => {
    const {transfers} = useTransfers();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Transfers</h1>
            <ul className="flex flex-col gap-3 w-[100ch]">
                {transfers && transfers.length > 0 ? (
                    transfers.map((transfer) => (
                        <li key={transfer["@id"]}>
                            <TransferCard transfer={transfer}/>
                        </li>
                    ))
                ) : (
                    <p className='text-center'>No transfers found. This Page will be updated as soon as there are
                        transfers.</p>
                )}
            </ul>
        </div>
    );
};
