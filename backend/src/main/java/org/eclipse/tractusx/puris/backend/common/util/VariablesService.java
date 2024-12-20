/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Getter
@Service
/**
 * This class contains the relevant
 */
public class VariablesService {

    @Value("${server.port}")
    /**
     * The port used by this apps server application.
     */
    private String serverPort;

    @Value("${puris.baseurl}")
    private String purisBaseUrl;

    /**
     * The puris base url as defined in the property puris.baseurl,
     * ending with a slash ('/').
     */
    public String getPurisBaseUrl() {
        return purisBaseUrl.endsWith("/") ? purisBaseUrl : purisBaseUrl + "/";
    }

    @Value("${puris.demonstrator.role}")
    /**
     * Must be set to "CUSTOMER" or "SUPPLIER" if
     * you want to start with some initial settings
     * defined in the DataInjectionCommandLineRunner
     */
    private String demoRole;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * The context path as defined in the property server.servlet.context-path,
     * ending with a slash ('/').
     */
    public String getContextPath() {
        return contextPath.replace("/", "") + "/";
    }

    /**
     * The url under which this application's item stock request endpoint can
     * be reached by external machines.
     */
    public String getItemStockSubmodelEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "item-stock/request";
    }

    @Value("${puris.itemstocksubmodel.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String itemStockSubmodelAssetId;

    /**
     * The url under which this application's planned production request endpoint can
     * be reached by external machines.
     */
    public String getProductionSubmodelEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "planned-production/request";
    }

    @Value("${puris.productionsubmodel.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String productionSubmodelAssetId;

    /**
     * The url under which this application's material demand request endpoint can
     * be reached by external machines.
     */
    public String getDemandSubmodelEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "material-demand/request";
    }

    @Value("${puris.demandsubmodel.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String demandSubmodelAssetId;

    /**
     * The url under which this application's delivery information request endpoint can
     * be reached by external machines.
     */
    public String getDeliverySubmodelEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "delivery-information/request";
    }

    @Value("${puris.deliverysubmodel.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String deliverySubmodelAssetId;

    /**
     * The url under which this application's demand and capacity notification request endpoint can
     * be reached by external machines.
     */
    public String getNotificationEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "demand-and-capacity-notification/request";
    }

    @Value("${puris.notification.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String notificationAssetId;

    /**
     * The url under which this application's request endpoint can
     * be reached by external machines.
     */
    public String getDaysOfSupplySubmodelEndpoint() {
        return getPurisBaseUrl() + getContextPath() + "days-of-supply/request";
    }

    @Value("${puris.daysofsupplysubmodel.apiassetid}")
    /**
     * The assetId that shall be assigned to the request API
     * during asset creation.
     */
    private String daysOfSupplySubmodelAssetId;

    @Value("${puris.frameworkagreement.credential}")
    /**
     * The name of the framework agreement to be used.
     */
    private String purisFrameworkAgreement;

    @Value("${puris.frameworkagreement.version}")
    /**
     * The version of the framework agreement to be used.
     */
    private String purisFrameworkAgreementVersion;

    @Value("${puris.purpose.name}")
    /**
     * The name of the purpose to be used for submodel contract policies.
     */
    private String purisPurposeName;

    @Value("${puris.purpose.version}")
    /**
     * The version of the purpse to be  used for submodel contract policies.
     */
    private String purisPurposeVersion;

    @Value("${puris.api.key}")
    /**
     * The key for accessing the api.
     */
    private String apiKey;

    @Value("${puris.dtr.url}")
    /**
     * The url of your decentralized DTR
     */
    private String dtrUrl;

    /**
     * The url under which this application's part type request endpoint can
     * be reached by external machines.
     */
    public String getParttypeInformationServerendpoint() {
        return getPurisBaseUrl() + getContextPath() + "parttypeinformation";
    }

    @Value("${puris.generatematerialcatenaxid}")
    /**
     * A flag that signals whether the MaterialService
     * should auto-generate a CatenaXId for a newly
     * created material.
     */
    private boolean generateMaterialCatenaXId;

    @Value("${edc.controlplane.key}")
    /**
     * The api key of your control plane
     */
    private String edcApiKey;
    @Value("${edc.controlplane.management.url}")
    /**
     * Your control plane's management url
     */
    private String edcManagementUrl;

    @Value("${edc.controlplane.protocol.url}")
    /**
     * Your control plane's protocol url
     */
    private String edcProtocolUrl;

    @Value("${edc.dataplane.public.url}")
    /**
     * Your data plane's public url
     */
    private String edcDataplanePublicUrl;

    @Value("${own.bpnl}")
    /**
     * The BPNL that was assigned to you.
     */
    private String ownBpnl;

    @Value("${own.name}")
    /**
     * A human-readable description of yourself, e.g.
     * the name of your company.
     */
    private String ownName;

    @Value("${own.bpns}")
    /**
     * A BPNS that was assigned to you.
     */
    private String ownDefaultBpns;

    @Value("${own.site.name}")
    /**
     * A human-readable description of the site that you referenced in
     * the ownDefaultBpns.
     */
    private String ownDefaultSiteName;

    @Value("${own.bpna}")
    /** A BPNA that was assigned to you. If you initialised the
     * ownDefaultBpns variable, then it must be a BPNA that is associated
     * to that BPNS.
     */
    private String ownDefaultBpna;

    @Value("${own.streetandnumber}")
    /**
     * The street and number associated to the ownDefaultBpna
     */
    private String ownDefaultStreetAndNumber;

    @Value("${own.zipcodeandcity}")
    /**
     * The zip code and name of the city associated to the ownDefaultBpna
     */
    private String ownDefaultZipCodeAndCity;

    @Value("${own.country}")
    /**
     * The country in which your ownDefaultBpna-address is located.
     */
    private String ownDefaultCountry;

    public String getItemStockSubmodelApiAssetId() {
        return itemStockSubmodelAssetId + "@" + ownBpnl;
    }

    public String getProductionSubmodelApiAssetId() {
        return productionSubmodelAssetId + "@" + ownBpnl;
    }

    public String getDemandSubmodelApiAssetId() {
        return demandSubmodelAssetId + "@" + ownBpnl;
    }

    public String getDeliverySubmodelApiAssetId() {
        return deliverySubmodelAssetId + "@" + ownBpnl;
    }

    public String getDaysOfSupplySubmodelApiAssetId() {
        return daysOfSupplySubmodelAssetId + "@" + ownBpnl;
    }

    public String getNotificationApiAssetId() {
        return notificationAssetId + "@" + ownBpnl;
    }

    public String getPartTypeSubmodelApiAssetId() {
        return "PartTypeInformationSubmodelApi@" + getOwnBpnl();
    }

    public String getPurisFrameworkAgreementWithVersion() {
        return getPurisFrameworkAgreement() + ":" + getPurisFrameworkAgreementVersion();
    }

    public String getPurisPurposeWithVersion() {
        return getPurisPurposeName() + ":" + getPurisPurposeVersion();
    }
}
