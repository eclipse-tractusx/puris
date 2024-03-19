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

import { Stock } from '@models/types/data/stock';

export const compareStocks = (a: Stock, b: Stock) =>
    a.stockLocationBpna === b.stockLocationBpna &&
    a.stockLocationBpns === b.stockLocationBpns &&
    (a.material.materialNumberCustomer === b.material.materialNumberCustomer ||
        a.material.materialNumberSupplier === b.material.materialNumberSupplier) &&
    a.partner?.uuid === b.partner?.uuid &&
    a.isBlocked === b.isBlocked &&
    a.customerOrderNumber === b.customerOrderNumber &&
    a.customerOrderPositionNumber === b.customerOrderPositionNumber &&
    a.supplierOrderNumber === b.supplierOrderNumber;

export const validateStock = (stock: Partial<Stock>) => {
  return (
    stock?.material &&
    stock?.partner &&
    stock?.quantity &&
    stock?.measurementUnit &&
    stock?.stockLocationBpns &&
    stock?.stockLocationBpna &&
    ((stock?.customerOrderNumber && stock?.customerOrderPositionNumber && stock?.supplierOrderNumber) || !stock?.customerOrderNumber)
);
}
