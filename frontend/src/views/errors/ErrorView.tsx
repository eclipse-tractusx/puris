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

export const ErrorView = () => {
    return (
        <div className="flex flex-col items-center pt-10 w-full h-full">
            <h1 className="text-4xl font-bold text-gray-700 mb-5">Error</h1>
            <p className="text-gray-500">Something went wrong.</p>
            <p className="text-gray-500">Try reloading the page. If the error persists please contact our support team.</p>
        </div>
    );
}
