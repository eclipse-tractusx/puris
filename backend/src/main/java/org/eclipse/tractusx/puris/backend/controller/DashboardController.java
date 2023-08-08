/*
 * Copyright (c) 2022,2023 Volkswagen AG
 * Copyright (c) 2022,2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.model.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller returning stats for the frontends Dashboard.
 */
@RestController
@RequestMapping("dashboard")
public class DashboardController {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired OrderRepository orderRepository;

  @Autowired
  EdcAdapterService edcAdapter;

  /**
   * Collect information for frontends dashboard.
   *
   * @return information about created and published orders, used to show frontends dashboard.
   * @throws IOException when connection to EDC fails.
   */
  @GetMapping("data")
  @CrossOrigin
  public JsonNode getData() throws IOException {
    var orders = orderRepository.findAll();
    var node = MAPPER.createObjectNode();
    node.put("orders", orders.size());
    node.put("ordersSent", edcAdapter.getFromEdc(null, "data", "assets"));
    node.put("responses", edcAdapter.getFromEdc(null, "data", "transferprocess"));
    return node;
  }
}
