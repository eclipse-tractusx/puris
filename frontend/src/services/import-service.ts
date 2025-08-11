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

import { config } from '@models/constants/config';
import AuthenticationService from './authentication-service';

export interface DataImportResult {
  message: string;
  errors: Array<{
    row: number;
    errors: string[];
  }>;
}

export const uploadDocuments = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(config.app.BACKEND_BASE_URL + `${config.app.ENDPOINT_IMPORT_FILES}/upload`, {
        method: 'POST',
        body: formData,
        headers: {
        'Authorization': `Bearer ${AuthenticationService.getToken()}`
        },
    });

    if (res.status === 422 || res.status < 400) {
        return res.json() as Promise<DataImportResult>;
    }

    if (res.status === 413) {
        return {
            message: 'The uploaded file has an invalid format.',
            errors: [],
        };
    }

    if (res.status >= 400) {
        const errorText = await res.text();
        throw new Error(errorText);
    }

    return res.json() as Promise<DataImportResult>;
};
