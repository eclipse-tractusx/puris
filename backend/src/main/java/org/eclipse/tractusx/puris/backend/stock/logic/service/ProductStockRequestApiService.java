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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;

/**
 * Interface provides the API capabilities to request ProductStock information and to handle requests.
 */
public interface ProductStockRequestApiService {

    /**
     * This method should be called in a separate Thread.
     *
     * It will evaluate the given ProductStockRequest and check, whether this Partner is
     * currently known as a customer for the given products. Then this method will assemble
     * all necessary information from database, generate ProductStockSammDto's and then send
     * them to the Partner via his product-stock-response-api.
     *
     * <p>Please note that this method currently does not support multple BPNS's/BPNA's per Partner.</p>
     *
     * @param productStockRequest a ProductStockRequest you received from a Customer Partner
     */
    void handleRequest(ProductStockRequest productStockRequest);

    /**
     * This method requests an update for the stock of the material for the given supplierPartner.
     *
     * @param material material to get the latest stock quantity for
     * @param supplierPartner partner to get the update from
     */
    void doRequest(Material material, Partner supplierPartner);
}
