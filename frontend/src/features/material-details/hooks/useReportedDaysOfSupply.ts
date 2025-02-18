/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

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

import { useFetch } from '@hooks/useFetch';
import { config } from '@models/constants/config';
import { Supply } from '@models/types/data/supply';
import { BPNL, BPNS } from '@models/types/edc/bpn';
import { DirectionType } from '@models/types/erp/directionType';
import { incrementDate } from '@util/date-helpers';

export function useReportedDaysOfSupply(materialNumber: string, direction: DirectionType, siteBpns?: BPNS, bpnl?: BPNL, numberOfDays = 28) {
    const params = new URLSearchParams();
    params.set('materialNumber', btoa(materialNumber));
    if (siteBpns) {
        params.set('siteBpns', siteBpns);
    }
    if (bpnl) {
        params.set('bpnl', bpnl);
    }
    params.set('numberOfDays', (numberOfDays ?? 28).toString());
    const url = `${config.app.BACKEND_BASE_URL}days-of-supply/${direction === DirectionType.Inbound ? 'customer' : 'supplier'}/reported?`;
    const { data, error, isLoading, refresh } = useFetch<Supply[]>(url + params.toString());
    // adjust dates to local timezone
    const today = new Date();
    const supplies = data?.map((supply, index) => ({ ...supply, date: incrementDate(today, index) }));
    return { supplies, error, isLoadingSupply: isLoading, refreshSupply: refresh };
}
