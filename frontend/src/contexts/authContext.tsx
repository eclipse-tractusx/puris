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

import { ReactNode, createContext, useEffect, useState } from 'react';
import AuthenticationService from '@services/authentication-service';
import { Auth } from '@models/types/auth/auth';

export const AuthContext = createContext<Auth>({
    isInitialized: false,
    isAuthenticated: false,
    userName: null,
    hasRole: () => false,
    logout: () => {},
});

type AuthProviderProps = {
    children: ReactNode;
};

export const AuthContextProvider = ({ children }: AuthProviderProps) => {
    const [auth, setAuth] = useState<Auth>({
        isInitialized: false,
        isAuthenticated: false,
        userName: '',
        hasRole: () => false,
        logout: () => {},
    });
    useEffect(() => {
        const initializeAuth = async () => {
            await AuthenticationService.init();
            setAuth({
                isInitialized: true,
                isAuthenticated: AuthenticationService.isAuthenticated() || false,
                userName: AuthenticationService.getUsername(),
                hasRole: AuthenticationService.userHasRole,
                logout: AuthenticationService.logout,
            });
        };
        initializeAuth();
    }, []);
    return <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>;
}
