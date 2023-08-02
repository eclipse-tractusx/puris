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

import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/createOrder",
      name: "createOrder",
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import("../views/CreateOrderView.vue"),
    },
    {
      path: "/manageOrders",
      name: "manageOrders",
      component: () => import("../views/ManageOrderView.vue"),
    },
    {
      path: "/catalog",
      name: "catalog",
      component: () => import("../views/CatalogView.vue"),
    },
    {
      path: "/negotiations",
      name: "negotitations",
      component: () => import("../views/NegotiationView.vue"),
    },
    {
      path: "/transfers",
      name: "transfers",
      component: () => import("../views/TransferView.vue"),
    },
    {
      path: "/responses",
      name: "responses",
      component: () => import("../views/OrderResponseView.vue"),
    },
    {
      path: "/connectors",
      name: "connectors",
      component: () => import("../views/ConnectorView.vue"),
    },
    {
      path: "/stocks",
      name: "stocks",
      component: () => import("../views/StockView.vue")
    },
    {
      path: "/supplierDashboard",
      name: "supplierDashboard",
      component: () => import("../views/SupplierDashboard.vue"),
    },
  ],
});

export default router;
