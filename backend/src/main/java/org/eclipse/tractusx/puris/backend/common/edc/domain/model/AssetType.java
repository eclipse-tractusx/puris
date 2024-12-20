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

package org.eclipse.tractusx.puris.backend.common.edc.domain.model;

public enum AssetType {
    DTR("none", "none", "none", "none"),
    ITEM_STOCK_SUBMODEL("urn:samm:io.catenax.item_stock:2.0.0#ItemStock", "$value", "ItemStock", "2.0"),
    PRODUCTION_SUBMODEL("urn:samm:io.catenax.planned_production_output:2.0.0#PlannedProductionOutput", "$value", "PlannedProductionOutput", "2.0"),
    DEMAND_SUBMODEL("urn:samm:io.catenax.short_term_material_demand:1.0.0#ShortTermMaterialDemand", "$value", "ShortTermMaterialDemand", "1.0"),
    DELIVERY_SUBMODEL("urn:samm:io.catenax.delivery_information:2.0.0#DeliveryInformation", "$value", "DeliveryInformation", "2.0"),
    NOTIFICATION("urn:samm:io.catenax.demand_and_capacity_notification:2.0.0#DemandAndCapacityNotification", "none", "none", "2.0"),
    DAYS_OF_SUPPLY("urn:samm:io.catenax.days_of_supply:2.0.0#DaysOfSupply", "$value", "DaysOfSupply", "2.0"),
    PART_TYPE_INFORMATION_SUBMODEL("urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation", "$value", "none", "1.0");

    public final String URN_SEMANTIC_ID;
    public final String REPRESENTATION;
    public final String ERP_KEYWORD;
    public final String ERP_SAMMVERSION;

    AssetType(String URN_SEMANTIC_ID, String REPRESENTATION, String ERP_KEYWORD, String ERP_SAMMVERSION) {
        this.URN_SEMANTIC_ID = URN_SEMANTIC_ID;
        this.REPRESENTATION = REPRESENTATION;
        this.ERP_KEYWORD = ERP_KEYWORD;
        this.ERP_SAMMVERSION = ERP_SAMMVERSION;
    }
}
