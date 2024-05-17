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

const app = {
    APP_NAME: import.meta.env.VITE_APP_NAME.trim() as string,
    BACKEND_BASE_URL: import.meta.env.VITE_BACKEND_BASE_URL.trim() as string,
    BACKEND_API_KEY: import.meta.env.VITE_BACKEND_API_KEY.trim() as string,
    ENDPOINT_MATERIALS: import.meta.env.VITE_ENDPOINT_MATERIALS.trim() as string,
    ENDPOINT_PRODUCTS: import.meta.env.VITE_ENDPOINT_PRODUCTS.trim() as string,
    ENDPOINT_MATERIAL_STOCKS: import.meta.env.VITE_ENDPOINT_MATERIAL_STOCKS.trim() as string,
    ENDPOINT_PRODUCT_STOCKS: import.meta.env.VITE_ENDPOINT_PRODUCT_STOCKS.trim() as string,
    ENDPOINT_CUSTOMER: import.meta.env.VITE_ENDPOINT_CUSTOMER.trim() as string,
    ENDPOINT_SUPPLIER: import.meta.env.VITE_ENDPOINT_SUPPLIER.trim() as string,
    ENDPOINT_REPORTED_MATERIAL_STOCKS: import.meta.env.VITE_ENDPOINT_REPORTED_MATERIAL_STOCKS.trim() as string,
    ENDPOINT_REPORTED_PRODUCT_STOCKS: import.meta.env.VITE_ENDPOINT_REPORTED_PRODUCT_STOCKS.trim() as string,
    ENDPOINT_UPDATE_REPORTED_MATERIAL_STOCKS: import.meta.env.VITE_ENDPOINT_UPDATE_REPORTED_MATERIAL_STOCKS.trim() as string,
    ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS: import.meta.env.VITE_ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS.trim() as string,
    ENDPOINT_PARTNER: import.meta.env.VITE_ENDPOINT_PARTNER.trim() as string,
    ENDPOINT_DEMAND: import.meta.env.VITE_ENDPOINT_DEMAND.trim() as string,
    ENDPOINT_PRODUCTION: import.meta.env.VITE_ENDPOINT_PRODUCTION.trim() as string,
    ENDPOINT_PRODUCTION_RANGE: import.meta.env.VITE_ENDPOINT_PRODUCTION_RANGE.trim() as string,
    ENDPOINT_DELIVERY: import.meta.env.VITE_ENDPOINT_DELIVERY.trim() as string,
};

const auth = {
    IDP_URL: import.meta.env.VITE_IDP_URL.trim() as string,
    IDP_REALM: import.meta.env.VITE_IDP_REALM.trim() as string,
    IDP_CLIENT_ID: import.meta.env.VITE_IDP_CLIENT_ID.trim() as string,
    IDP_REDIRECT_URL_FRONTEND: import.meta.env.VITE_IDP_REDIRECT_URL_FRONTEND.trim() as string,
    IDP_DISABLE: import.meta.env.VITE_IDP_DISABLE === true || import.meta.env.VITE_IDP_DISABLE.trim().toLowerCase() === 'true',
};

export const config = {
    app,
    auth,
};
