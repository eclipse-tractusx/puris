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
package org.eclipse.tractusx.puris.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Orders created in this PURIS instance.
 */
@Entity
@Table(name = "scmorder")
@NoArgsConstructor
@Getter
@Setter
public class Order extends JpaBaseEntity {

  @Column(unique = true)
  @NotNull
  private String orderId;

  private String orderDate;

  private String name;

  private String description;

  @JsonIgnore private boolean sent = false;

  @Embedded private CustomerInformation customerInformation;

  @Embedded private SupplierInformation supplierInformation;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn
  @NotNull
  private Set<OrderPosition> orderPositions;

}
