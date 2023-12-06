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
import Keycloak from "keycloak-js";
import AuthenticationConfig, {
    getIdpClientId,
    getIdpRealm,
    getIdpRedirectUrlFrontend,
    getIdpUrl,
} from "@/services/AuthenticationConfig";

const keycloak = new Keycloak({
    onLoad: "login-required",
    url: getIdpUrl,
    realm: getIdpRealm,
    clientId: getIdpClientId,
    redirectUri: getIdpRedirectUrlFrontend,
});

const isEnabled = AuthenticationConfig.isDisabled !== true;

const init = () => {
    return new Promise((resolve, reject) => {
        console.log("Auth is disabled: ", AuthenticationConfig.isDisabled);
        if (!isEnabled) {
            return resolve();
        }
        keycloak
            .init({ onLoad: "login-required" })
            .then((authenticated) => {
                if (!authenticated) {
                    // User is not authenticated
                    reject();
                } else {
                    // authenticated
                    resolve();
                }
            })
            .catch((error) => {
                console.error("Not authenticated:", error);
                reject(error);
            });
    });
};

const isAuthenticated = () => {
    if (!isEnabled) {
        return true;
    }
    return keycloak.authenticated;
};

const userHasRole = (requiredRoles) => {
    if (!isEnabled) {
        return true;
    }
    // client roles
    const rolesPerClient = keycloak.tokenParsed.resource_access;
    const userRoles = rolesPerClient[getIdpClientId].roles;
    // require every role, not some
    return requiredRoles.every((role) => userRoles.includes(role));
};

const logout = () => {
    if (!isEnabled){
        return;
    }
    keycloak
        .logout()
        .then((success) => {
            console.info("Logged out: ", success);
        })
        .catch((error) => {
            console.error("Logout failed: ", error);
        });
};

const AuthenticationService = {
    isEnabled,
    init,
    logout,
    isAuthenticated,
    userHasRole,
};

export default AuthenticationService;
