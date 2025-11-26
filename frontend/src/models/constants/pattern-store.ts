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

export const RE_BPNL = /^BPNL[A-Z0-9]{12}$/;
export const RE_BPNA = /^BPNA[A-Z0-9]{12}$/;
export const RE_BPNS = /^BPNS[A-Z0-9]{12}$/;

export const NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING = /^[^\\n\\x0B\\f\\r\\x85\\u2028\\u2029]+$/;
export const URL_REGEX = /^http[s]?:\/\/([a-z0-9][a-z0-9-]+[a-z0-9])(\.[a-z0-9-]+)*(:[0-9]{1,4})?(\/[a-z0-9-]+)*\/?$/;