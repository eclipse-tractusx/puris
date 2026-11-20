/*
Copyright (c) 2026 Volkswagen AG

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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/

// All dates and times are displayed in en-GB format, independent of the browser's locale.
const DISPLAY_LOCALE = 'en-GB';

// date-fns format used by the date pickers, matching the en-GB date format
export const DATE_INPUT_FORMAT = 'dd/MM/yyyy';

type DateInput = Date | string | number;

// e.g. 27/10/2026
export const formatDate = (value: DateInput, options?: Intl.DateTimeFormatOptions) =>
    new Date(value).toLocaleDateString(DISPLAY_LOCALE, options);

// e.g. 14:35:00
export const formatTime = (value: DateInput, options?: Intl.DateTimeFormatOptions) =>
    new Date(value).toLocaleTimeString(DISPLAY_LOCALE, options);

// e.g. 27/10/2026, 14:35:00
export const formatDateTime = (value: DateInput, options?: Intl.DateTimeFormatOptions) =>
    new Date(value).toLocaleString(DISPLAY_LOCALE, options);
