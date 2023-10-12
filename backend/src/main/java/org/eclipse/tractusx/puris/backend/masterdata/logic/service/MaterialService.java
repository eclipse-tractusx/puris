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

import java.util.List;

public interface MaterialService {

    Material create(Material material);

    Material update(Material material);

    List<Material> findAllMaterials();

    List<Material> findAllProducts();



//    Material findByUuid(UUID materialUuid);

    Material findByOwnMaterialNumber(String ownMaterialNumber);

    Material findByMaterialNumberCx(String materialNumberCx);

    List<Material> findAll();

//    Material findMaterialByMaterialNumberCustomer(String materialNumberCustomer);
//
//    Material findProductByMaterialNumberCustomer(String materialNumberCustomer);

}
