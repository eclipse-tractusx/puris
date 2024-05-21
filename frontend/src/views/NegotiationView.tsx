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

import {useNegotiations} from '@hooks/edc/useNegotiations';
import {Negotiation} from '@models/types/edc/negotiation';

type NegotiationCardProps = {
    negotiation: Negotiation;
};

const NegotiationCard = ({negotiation}: NegotiationCardProps) => {
    return (
        <Card className="p-5">
            <h2 className="text-xl font-semibold mb-3">Negotiation</h2>
            <div className="flex w-full flex-col gap-4">
                <div className="flex flex-col gap-2">
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Negotiation Id: </span>
                        {negotiation['@id']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Aggreement  Id: </span>
                        <span className="break-all w-[60ch]">{negotiation['contractAgreementId']}</span>
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Type: </span>
                        {negotiation['type']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> State: </span>
                        {negotiation['state']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> CounterParty: </span>
                        {negotiation['counterPartyId']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Counterparty EDC URL: </span>
                        {negotiation['counterPartyAddress']}
                    </div>
                    <div className="flex gap-3">
                        <span className="w-[30ch] font-semibold"> Timestamp: </span>
                        {new Date(negotiation['createdAt']).toLocaleString()}
                    </div>
                </div>
            </div>
        </Card>
    );
};

export const NegotiationView = () => {
    const {negotiations} = useNegotiations();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Negotiation</h1>
            <ul className="flex flex-col gap-3 w-[100ch]">
                {negotiations && negotiations.length > 0 ? (
                    negotiations.map((negotiation) => (
                        <li key={negotiation["@id"]}>
                            <NegotiationCard negotiation={negotiation}/>
                        </li>
                    ))
                ) : (
                    <p className='text-center'>No negotiations found. This list will be updated when Negotiations
                        happen.</p>
                )}
            </ul>
        </div>
    );
}
