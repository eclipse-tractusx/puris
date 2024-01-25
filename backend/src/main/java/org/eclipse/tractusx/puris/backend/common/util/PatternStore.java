/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.common.util;

import java.util.regex.Pattern;

public class PatternStore {

    /**
     * Contains a Java-Regex-String that can be used to validate
     * BPNL numbers.
     */
    public final static String BPNL_STRING = "^BPNL[0-9a-zA-Z]{12}$";

    /**
     * Contains a Pattern that matches valid BPNL numbers.
     * It is constructed from the BPNL_STRING constant.
     */
    public final static Pattern BPNL_PATTERN = Pattern.compile(BPNL_STRING);

    /**
     * Contains a Java-Regex-String that can be used to validate
     * BPNA numbers.
     */
    public static final String BPNA_STRING = "^BPNA[0-9a-zA-Z]{12}$";

    /**
     * Contains a Pattern that matches valid BPNA numbers.
     * It is constructed from the BPNA_STRING constant.
     */
    public static final Pattern BPNA_PATTERN = Pattern.compile(BPNA_STRING);


    /**
     * Contains a Java-Regex-String that can be used to validate
     * BPNS numbers.
     */
    public static final String BPNS_STRING = "^BPNS[0-9a-zA-Z]{12}$";

    /**
     * Contains a Pattern that matches valid BPNS numbers.
     * It is constructed from the BPNS_STRING constant.
     */
    public static final Pattern BPNS_PATTERN = Pattern.compile(BPNS_STRING);

    /**
     * Contains a Java-Regex-String that allows any combination of alphabetic characters,
     * digits and special characters, excluding only empty strings and vertical whitespaces
     * like "\n" or "\r".
     *
     * It can be used to validate common names in human language, a well as material-numbers.
     *
     * It also can be used for sanitization of foreign input data before printing it to the logs.
     *
     */
    public final static String NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING = "^[^\\n\\x0B\\f\\r\\x85\\u2028\\u2029]+$";

    /**
     * Contains a Pattern that allows any combination of alphabetic characters,
     * digits and special characters, excluding only empty strings and vertical whitespaces
     * like "\n" or "\r".
     *
     * It can be used to validate common names in human language, a well as material-numbers.
     * It also can be used for sanitization of foreign input data before printing it to the logs.
     * It is constructed from the NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING constant.
     *
     */
    public final static Pattern NON_EMPTY_NON_VERTICAL_WHITESPACE_PATTERN = Pattern.compile(NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING);

    /**
     * Contains a Java-Regex-String that matches valid url data like
     *  <li>"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp" - common ingress with path</li>
     * <li>"https://isst-edc-supplier.int.demo.catena-x.net" - ingress stating directly to protocol path</li>
     * <li>"http://customer-control-plane:8184/api/v1/dsp" - e.g. local development</li>
     * <li>"http://127.0.0.1:8081/api/v1/dsp" - e.g. local development/li>
     * <li>"http://127.0.0.1:8081/api/v1/dsp/" - trailing slash is allowed/li>
     */
    public final static String URL_STRING = "^http[s]?://([a-z0-9][a-z0-9\\-]+[a-z0-9])(\\.[a-z0-9\\-]+)*(:[0-9]{1,4})?(/[a-z0-9\\-]+)*[/]?$";

    /**
     * Contains a Pattern that matches valid url data.
     * It is constructed from the URL_STRING constant.
     */
    public final static Pattern URL_PATTERN = Pattern.compile(URL_STRING);

    /**
     * Contains a Java-Regex-String that matches valid urn data like
     * <li>urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df</li>
     * <li>48878d48-6f1d-47f5-8ded-a441d0d879df</li>
     */
    public final static String URN_STRING = "(^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)|(^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)";

    /**
     * Contains a Pattern that matches valid urn data.
     * It is constructed from the URN_STRING constant.
     */
    public final static Pattern URN_PATTERN = Pattern.compile(URN_STRING);


}
