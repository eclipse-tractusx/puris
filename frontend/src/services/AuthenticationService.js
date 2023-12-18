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

// 5 min validity
const MIN_TOKEN_VALIDITY = 600;

const keycloak = new Keycloak({
    onLoad: "login-required",
    url: getIdpUrl,
    realm: getIdpRealm,
    clientId: getIdpClientId,
    redirectUri: getIdpRedirectUrlFrontend,
    enableLogging: true,
});

const isEnabled = AuthenticationConfig.isDisabled !== true;

const init = () => {
    return new Promise((resolve, reject) => {
        if (!isEnabled) {
            console.info("Authentication via Identity Provider is disabled.");
            return resolve();
        }
        keycloak
            .init({ onLoad: "login-required" })
            .then((authenticated) => {
                if (!authenticated) {
                    // User is not authenticated
                    console.error(
                        "User '%s' is NOT authenticated.",
                        getUsername()
                    );
                    reject();
                } else {
                    // authenticated
                    console.info("User '%s' authenticated.", getUsername());
                    resolve();
                }
            })
            .catch((error) => {
                console.error("Authentication failed:", error);
                reject(error);
            });
    });
};

keycloak.onTokenExpired = () => {
    keycloak
        .updateToken(MIN_TOKEN_VALIDITY)
        .then((updated) => {
            if (updated) {
                console.info("Renewed auth token for user '%s'.", getUsername());
            } else {
                console.error(
                    "Auth token could not be renewed for user '%s'.",
                    getUsername()
                );
                keycloak.clearToken();
            }
        })
        .catch((error) => {
            console.error("Error during auth token renewal:", error);
            keycloak.clearToken();
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

    return requiredRoles.some((role) => userRoles.includes(role));
};

const logout = () => {
    if (!isEnabled) {
        return;
    }
    keycloak
        .logout()
        .then((success) => {
            console.info(
                "User '%s' logged out successfully: ",
                getUsername(),
                success
            );
            keycloak.clearToken();
        })
        .catch((error) => {
            console.error("Logout for user '%s' failed: ", getUsername(), error);
        });
};

const getUsername = () => keycloak.idTokenParsed.preferred_username;

const AuthenticationService = {
    isEnabled,
    getUsername,
    init,
    logout,
    isAuthenticated,
    userHasRole,
};

export default AuthenticationService;
