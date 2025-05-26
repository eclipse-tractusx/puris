/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/

describe("navigation", () => {
    beforeEach(() => {
        cy.visit('/')
        cy.login('customer');
    })

    it("allows navigating to all sidebar menu pages", () => {
        cy.fixture("menu.json").then((menu) => {
            menu.forEach((item) => {
                if (item.target) {
                    cy.visit(item.target);
                    cy.url().should("match", new RegExp(item.target));
                }
            });
        });
    });

    it("redirects to the material list view when no path is provided", () => {
        cy.visit("/");
        cy.url().should("match", /\/materials$/);
    });

    it("redirects to not found page when an invalid url is provided", () => {
        cy.visit("/invalid-url");
        cy.contains("Error 404").should("exist");
        cy.contains("Page not found").should("exist");
    });
});
