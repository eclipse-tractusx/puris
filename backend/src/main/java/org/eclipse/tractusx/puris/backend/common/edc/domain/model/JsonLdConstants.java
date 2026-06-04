/*
 * Copyright (c) 2026 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.common.edc.domain.model;

/**
 * Constants for JSON-LD contexts and namespaces used in the Catena-X ecosystem.
*/
public final class JsonLdConstants {
    public final static String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
    public final static String VOCAB_KEY = "@vocab";
    public final static String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
    public final static String CX_TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
    public final static String CX_COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    public final static String DCT_NAMESPACE = "http://purl.org/dc/terms/";
    public final static String AAS_SEMANTICS_NAMESPACE = "https://admin-shell.io/aas/3/0/HasSemantics/";
    public final static String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    public final static String TX_AUTH_NAMESPACE = "https://w3id.org/tractusx/auth/";
    public final static String DCAT_NAMESPACE = "http://www.w3.org/ns/dcat#";
}
