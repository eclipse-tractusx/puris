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

import { usePartnerStocks } from '@features/stock-view/hooks/usePartnerStocks';
import { useStocks } from '@features/stock-view/hooks/useStocks';
import { MaterialDescriptor } from '@models/types/data/material-descriptor';
import { Site } from '@models/types/edc/site';
import { useState } from 'react';
import { DashboardFilters } from './DashboardFilters';
import { DemandTable } from './DemandTable';
import { ProductionTable } from './ProductionTable';
import { Stack, Typography, capitalize } from '@mui/material';
import { Delivery } from '@models/types/data/delivery';
import { DeliveryInformationModal } from './DeliveryInformationModal';
import { getPartnerType } from '../util/helpers';

const NUMBER_OF_DAYS = 42;

export const Dashboard = ({ type }: { type: 'customer' | 'supplier' }) => {
    const [selectedMaterial, setSelectedMaterial] = useState<MaterialDescriptor | null>(null);
    const [selectedSite, setSelectedSite] = useState<Site | null>(null);
    const [selectedPartnerSites, setSelectedPartnerSites] = useState<Site[] | null>(null);
    const { stocks } = useStocks(type === 'customer' ? 'material' : 'product');
    const { partnerStocks } = usePartnerStocks(type === 'customer' ? 'material' : 'product', selectedMaterial?.ownMaterialNumber ?? null);
    const [open, setOpen] = useState(false);
    const [delivery, setDelivery] = useState<Delivery | null>(null);
    const openDeliveryDialog = (d: Delivery) => {
        setDelivery(d);
        setOpen(true);
    };
    const handleMaterialSelect = (material: MaterialDescriptor | null) => {
        setSelectedMaterial(material);
        setSelectedSite(null);
        setSelectedPartnerSites(null);
    };
    return (
        <>
            <Stack spacing={3} alignItems={'center'}>
                <DashboardFilters
                    type={type}
                    material={selectedMaterial}
                    site={selectedSite}
                    partnerSites={selectedPartnerSites}
                    onMaterialChange={handleMaterialSelect}
                    onSiteChange={setSelectedSite}
                    onPartnerSitesChange={setSelectedPartnerSites}
                />
                <Typography variant="h5" component="h2" marginBottom={0}>
                    Our Stock Information {selectedMaterial && selectedSite && <>for {selectedMaterial.description}</>}
                </Typography>
                {selectedSite ? (
                    type === 'supplier' ? (
                        <ProductionTable
                            numberOfDays={NUMBER_OF_DAYS}
                            stocks={stocks}
                            site={selectedSite}
                            onDeliveryClick={openDeliveryDialog}
                        />
                    ) : (
                        <DemandTable
                            numberOfDays={NUMBER_OF_DAYS}
                            stocks={stocks}
                            site={selectedSite}
                            onDeliveryClick={openDeliveryDialog}
                        />
                    )
                ) : (
                    <Typography variant="body1">Select a Site to show production data</Typography>
                )}
                {selectedSite && (
                    <>
                        <Typography variant="h5" component="h2">
                            {`${capitalize(getPartnerType(type))} Stocks ${selectedMaterial ? `for ${selectedMaterial?.description}` : ''}`}
                        </Typography>
                        {selectedPartnerSites ? (
                            selectedPartnerSites.map((ps) =>
                                type === 'supplier' ? (
                                    <DemandTable
                                        numberOfDays={NUMBER_OF_DAYS}
                                        stocks={partnerStocks}
                                        site={ps}
                                        onDeliveryClick={openDeliveryDialog}
                                    />
                                ) : (
                                    <ProductionTable
                                        numberOfDays={NUMBER_OF_DAYS}
                                        stocks={partnerStocks}
                                        site={ps}
                                        onDeliveryClick={openDeliveryDialog}
                                    />
                                )
                            )
                        ) : (
                            <Typography variant="body1">{`Select a ${getPartnerType(type)} site to show their stock information`}</Typography>
                        )}
                    </>
                )}
            </Stack>
            <DeliveryInformationModal open={open} onClose={() => setOpen(false)} delivery={delivery} />
        </>
    );
};
