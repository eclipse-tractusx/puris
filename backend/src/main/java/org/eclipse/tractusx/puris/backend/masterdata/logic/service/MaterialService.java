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
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.List;

public interface MaterialService {

    Material create(Material material);

    Material update(Material material);

    List<Material> findAllMaterials();

    List<Material> findAllProducts();

    Material findByOwnMaterialNumber(String ownMaterialNumber);

    Material findByMaterialNumberCx(String materialNumberCx);
    
    List<Material> findAll();
    
    Material updateTimestamp(String ownMaterialNumber);

    /**
     * This method will do a best effort attempt to return a Material Entity for the given input arguments.
     * All arguments are potentially nullable. But the more arguments you provide, the better.
     *
     * The material will be selected
     * <li>by the materialNumberCx, if possible</li>
     * <li>otherwise by the customerMaterialNumber, if possible</li>
     * <li>otherwise by the supplierMaterialNumber, if possible</li>
     *
     * If a materialNumberCx is given and there exists a Material Entity that matches, this Material will
     * always be chosen.
     *
     * Otherwise, an attempt will be made to find a matching Material by the other arguments. In this case,
     * if a specific partner is given as an argument, a matching supplierMaterialNumber will only be considered,
     * if this is actually this partner's material number.
     *
     * This method will write warnings to the log, if mismatches or ambiguities were found.
     *
     * Since this method is only meant to be used from a Customer's Perspective, the customerMaterialNumber will
     * be treated as ownMaterialNumber.
     *
     * @param materialNumberCx the CatenaX - MaterialNumber
     * @param customerMatNbr   the MaterialNumber on the customer's side
     * @param supplierMatNbr   the MaterialNumber on the supplier's side
     * @param partner          the Partner
     * @return                 the Material, or null if no matching material was found
     */
    Material findFromCustomerPerspective(String materialNumberCx, String customerMatNbr, String supplierMatNbr, Partner partner);

    /**
     * This method will do a best effort attempt to return a Material Entity for the given input arguments.
     * All arguments are potentially nullable. But the more arguments you provide, the better.
     *
     * The material will be selected
     * <li>by the materialNumberCx, if possible</li>
     * <li>otherwise by the customerMaterialNumber, if possible</li>
     * <li>otherwise by the supplierMaterialNumber, if possible</li>
     *
     * If a materialNumberCx is given and there exists a Material Entity that matches, this Material will
     * always be chosen.
     *
     * Otherwise, an attempt will be made to find a matching Material by the other arguments. In this case,
     * if a specific partner is given as an argument, a matching customerMaterialNumber will only be considered,
     * if this is actually this partner's material number.
     *
     * This method will write warnings to the log, if mismatches or ambiguities were found.
     *
     * Since this method is only meant to be used from a Supplier's Perspective, the supplierMaterialNumber will
     * be treated as ownMaterialNumber.
     *
     * @param materialNumberCx the CatenaX - MaterialNumber
     * @param customerMatNbr   the MaterialNumber on the customer's side
     * @param supplierMatNbr   the MaterialNumber on the supplier's side
     * @param partner          the Partner
     * @return                 the Material, or null if no matching material was found
     */
    Material findFromSupplierPerspective(String materialNumberCx, String customerMatNbr, String supplierMatNbr, Partner partner);


}
