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

describe("sidebar", () => {
    beforeEach(() => {
        cy.login('customer');
        cy.visit("/materials");
        cy.url().should('include', '/materials'); // confirm page load
        cy.wait(1000);
    });

    it("shows 7 menu items and one is selected", () => {
        cy.fixture("menu.json").then((menu) => {
            cy.getByTestId("sidebar").should("exist");
            cy.getByTestIdContains("sidebar-menu-item").should(
                "have.length",
                menu.length
            );
            cy.get(
                '[data-testid*="sidebar-menu-item"][aria-selected="true"]'
            ).should("have.length", 1);
            cy.get(
                '[data-testid*="sidebar-menu-item"][aria-selected="true"]'
            ).should("contain", menu[0].name);

            const menuLinks = menu.filter((item) => item.target);

            menuLinks.forEach((item) => {
                cy.get('[data-testid*="sidebar-menu-item"] a')
                    .contains(item.name)
                    .click();
            
                cy.url().should("match", new RegExp(item.target));
            });
        });
    });
});
