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
        cy.visit("/");
        cy.login();
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

            for (let i = 0; i < menuLinks.length; i++) {
                cy.get('[data-testid*="sidebar-menu-item"] a')
                    .should("have.length", 6)
                    .eq(i)
                    .click();
                cy.url().should("match", new RegExp(menuLinks[i].target));
            }

            cy.get('[data-testid="sidebar-item-license"]').should("exist");
            cy.get('[data-testid="sidebar-item-license"] a').click();
            cy.url().should("match", /\/aboutLicense$/);
            cy.get(
                '[data-testid*="sidebar-menu-item"][aria-selected="true"]'
            ).should("not.exist");
        });
    });
});
