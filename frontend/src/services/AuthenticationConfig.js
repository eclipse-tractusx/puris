/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

export const isDisabled =
    import.meta.env.VITE_IDP_DISABLE === true ||
    import.meta.env.VITE_IDP_DISABLE.trim().toLowerCase() === "true";

export const getIdpUrl = import.meta.env.VITE_IDP_URL.trim();

export const getIdpRealm = import.meta.env.VITE_IDP_REALM.trim();

export const getIdpClientId = import.meta.env.VITE_IDP_CLIENT_ID.trim();

export const getIdpRedirectUrlFrontend = import.meta.env
    .VITE_IDP_REDIRECT_URL_FRONTEND.trim();

const AuthenticationConfig = {
    isDisabled,
    getIdpUrl,
    getIdpRealm,
    getIdpClientId,
    getIdpRedirectUrlFrontend,
};

export default AuthenticationConfig;
