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

import { useNegotiations } from '@hooks/edc/useNegotiations';

export const NegotiationView = () => {
    const { negotiations  } = useNegotiations();
    return (
        <div className="flex flex-col items-center w-full h-full">
            <h1 className="text-3xl font-semibold text-gray-700 mb-10">Negotiation</h1>
            <ul>
                {negotiations && negotiations.length > 0 ? (
                    negotiations.map((negotiation) => (
                        <li>
                            <h2> Transfer Id: {negotiation['@id']}</h2>
                            <span> Agreement Id: {negotiation['edc:contractAgreementId']}</span>
                            <span> Type: {negotiation['edc:type']}</span>
                            <span> State: {negotiation['edc:state']}</span>
                            <span> Counterparty: {negotiation['edc:counterPartyId']}</span>
                            <span> Counterparty EDC URL: {negotiation['edc:counterPartyAddress']}</span>
                            <span> Timestamp: {negotiation['edc:createdAt']}</span>
                        </li>
                    ))
                ) : (
                    <p>No negotiations found</p>
                )}
            </ul>
        </div>
    );
}
