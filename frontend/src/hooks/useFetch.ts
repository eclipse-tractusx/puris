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

import { useCallback, useEffect, useState } from 'react';
import { config } from '@models/constants/config';

const defaultHeaders = {
    'Content-Type': 'application/json',
    'X-API-KEY': config.app.BACKEND_API_KEY,
};

export const useFetch = <T = unknown>(url?: string, options?: RequestInit) => {
    const [data, setData] = useState<T | null>(null);
    const [error, setError] = useState<unknown>(null);
    const [isLoading, setIsLoading] = useState(true);

    const fetchData = useCallback(async () => {
        if (!url) {
            setIsLoading(false);
            return;
        }
        let shouldCancel = false;
        setIsLoading(true);
        setError(null);
        fetch(url, {
            method: options?.method ?? 'GET',
            body: options?.body ?? undefined,
            headers: { ...defaultHeaders, ...options?.headers },
        })
            .then((res) => res.json())
            .then((data) => {
                if (shouldCancel) return;
                setData(data);
            })
            .catch((err) => {
                if (shouldCancel) return;
                setData(null);
                setError(err);
            })
            .finally(() => setIsLoading(false));
        return () => {
            shouldCancel = true;
        };
    }, [url, options]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const refresh = useCallback(() => {
        return fetchData();
    }, [fetchData]);
    return {
        data,
        error,
        isLoading,
        refresh,
    };
};
