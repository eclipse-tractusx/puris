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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.model.Order;
import org.eclipse.tractusx.puris.backend.model.repo.OrderPositionRepository;
import org.eclipse.tractusx.puris.backend.model.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for creating and managing Orders/Call-offs.
 */
@RestController
@RequestMapping("orders")
public class OrderController {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired OrderRepository orderRepository;

  @Autowired OrderPositionRepository orderPositionRepository;

  /**
   * Get all orders.
   *
   * @return List of all orders.
   */
  @GetMapping("order")
  public ResponseEntity<List<Order>> getAllOrders() {
    return ResponseEntity.ok(orderRepository.findAll());
  }

  /**
   * Get a list of all sent orders.
   *
   * @return list of all orders where sent=true.
   */
  @GetMapping("orders/sent")
  public ResponseEntity<List<Order>> getSentOrders() {
    return ResponseEntity.ok(
        orderRepository.findAll().stream().filter(Order::isSent).collect(Collectors.toList()));
  }

  /**
   * Get a list of all pending orders.
   *
   * @return list of all orders where sent=false.
   */
  @GetMapping("orders/pending")
  public ResponseEntity<List<Order>> getNotSentOrder() {
    return ResponseEntity.ok(
        orderRepository.findAll().stream()
            .filter(order -> !order.isSent())
            .collect(Collectors.toList()));
  }

  /**
   * Get an order by id.
   *
   * @param id id of the order to get.
   * @return requested order or 404 not found.
   */
  @GetMapping("order/id/{id}")
  public ResponseEntity<?> getOrder(@PathVariable String id) {
    var order = orderRepository.findByOrderId(id);
    if (order.isEmpty()) {
      return ResponseEntity.status(404)
          .body(String.format("Message not found with id {\"%s\"}", id));
    } else {
      return ResponseEntity.ok(order.get());
    }
  }

  /**
   * Create a new order.
   *
   * @param content JSON representation of the order to create.
   * @return OK if order was created, or information about why order could not be created.
   */
  @PostMapping("order")
  public ResponseEntity<?> createOrder(@RequestBody String content) {
    try {
      var order = MAPPER.readValue(content, Order.class);
      if (!checkOrder(order)) {
        return ResponseEntity.status(400)
            .body("Cannot proceed because order is incomplete!");
      }
      if (orderRepository.findByOrderId(order.getOrderId()).isPresent()) {
        return ResponseEntity.status(400)
            .body(
                String.format(
                    "Cannot proceed because object with id {\"%s\"} was already created.",
                    order.getOrderId()));
      } else {
        orderRepository.saveAndFlush(order);
        return ResponseEntity.ok(String.format("{\"id\":\"%s\" }", order.getOrderId()));
      }

    } catch (JsonProcessingException e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
  }

  /**
   * Delete an order.
   *
   * @param id id of the order to delete.
   * @return OK or info why order could not be deleted.
   */
  @DeleteMapping("order")
  public ResponseEntity<?> deleteOrder(@RequestParam String id) {
    var toDelete = orderRepository.findByOrderId(id);
    if (toDelete.isEmpty()) {
      return ResponseEntity.status(400)
          .body(String.format("Cannot delete because {\"%s\"} is unknown \nyet.", id));
    } else {
      if (toDelete.get().isSent()) {
        return ResponseEntity.status(400).body("Cannot delete already sent order!");
      }
      orderRepository.delete(toDelete.get());
      return ResponseEntity.ok().build();
    }
  }

  /**
   * Check if an order is complete and no orderPositions have duplicate IDs.
   *
   * @param order order to check.
   * @return true if order is complete.
   */
  private boolean checkOrder(Order order) {
    var orderCheck =
        order.getOrderId() != null
            && !order.getOrderId().isBlank()
            && order.getName() != null
            && !order.getName().isBlank()
            && order.getOrderDate() != null
            && !order.getOrderDate().isBlank();
    var posCheck = true;
    for (var pos : order.getOrderPositions()) {
      if (orderPositionRepository.findByPositionId(pos.getPositionId()).isPresent()) {
        return false;
      }
      posCheck &= pos.getItemName() != null;
      posCheck &= !pos.getItemName().isBlank();
      posCheck &= pos.getQuantity() != null;
      posCheck &= pos.getDesiredDate() != null;
      posCheck &= !pos.getDesiredDate().isBlank();
    }
    return orderCheck && posCheck;
  }
}
