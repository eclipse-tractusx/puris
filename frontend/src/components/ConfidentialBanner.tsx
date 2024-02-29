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

export const ConfidentialBanner = () => {
    return (
        <header className="bg-orange-100 text-red-500 font-semibold w-full flex items-center justify-center p-3 mb-5 text-center">
            <span>
                IMPORTANT: Please note that the data shown may be <b> competitively sensitive </b> and, according to appliable antitrust
                laws,
                <b> must not </b> be shared with competitors. Please consult your legal department, if necessary.
            </span>
        </header>
    );
};
