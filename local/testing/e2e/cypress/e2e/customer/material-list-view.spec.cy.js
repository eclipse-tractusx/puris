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

describe('material list view', () => {
    beforeEach(() => {
        cy.visit('/materials');
        cy.login();
    });

    it('contains a confidentiality banner and copyright footer', () => {
        cy.verifyConfidentialityBanner();
        cy.verifyCopyrightFooter();
    })

    it('shows the correct sidebar item as selected', () => {
      cy.get('[data-testid="sidebar-menu-item-materials"][aria-selected="true"]').should('exist');
    })

    it('shows all materials and allows navigating to material details and back', function () {
        cy.fixture('customer-data.json').then(({ materials }) => {
            materials.forEach((material) => {
                const directions = [];
                if (material.materialFlag) directions.push('Inbound');
                if (material.productFlag) directions.push('Outbound');
                
                directions.forEach((direction) => {
                    cy.get(
                        `[data-rowindex]:has([data-field="direction"] > [title="${direction}"]):has([data-field="ownMaterialNumber"] > [title="${material.ownMaterialNumber}"])`
                    ).should('be.visible').click();
                    cy.url().should('include', `/materials/${direction.toLowerCase()}/${material.ownMaterialNumber}`);
                    cy.getByTestId('back-button').should('be.visible').click();
                    cy.url().should('match', /\/materials$/);
                });
            });
        });
    });

    it('allows searching by material name and number', () => {
        cy.fixture('customer-data.json').then(({ materials }) => {
            const expectedNumberOfMaterialRows = materials.reduce(
                (acc, { materialFlag, productFlag }) => acc + (materialFlag && productFlag ? 2 : !materialFlag && !productFlag ? 0 : 1),
                0
            );
            cy.get('[role="row"][data-rowindex]').as('all-rows').should('have.length', expectedNumberOfMaterialRows);
            materials.forEach((material) => {
                const directions = [];
                if (material.materialFlag) directions.push('Inbound');
                if (material.productFlag) directions.push('Outbound');

                cy.get('[data-testid="search-input"] input').as('search-input').type(material.name);
                cy.wait(100);
                cy.get('@all-rows').should('have.length', directions.length);
                cy.get('@search-input').clear();
                cy.get('@all-rows').should('have.length', expectedNumberOfMaterialRows);
                cy.get('@search-input').type(material.ownMaterialNumber);
                cy.wait(100);
                cy.get('@all-rows').should('have.length', directions.length);
                cy.get('@search-input').clear();
            });
        });
    });

    it('allows filtering by direction', () => {
        cy.fixture('customer-data.json').then(({ materials }) => {
            const expectedNumberOfInboundMaterials = materials.filter((mat) => mat.materialFlag).length;
            const expectedNumberOfOutboundMaterials = materials.filter((mat) => mat.productFlag).length;
            const expectedNumberOfMaterialRows = expectedNumberOfInboundMaterials + expectedNumberOfOutboundMaterials;

            // check the initial number of rows displayed
            cy.get('[role="row"][data-rowindex]').as('all-rows').should('have.length', expectedNumberOfMaterialRows);

            // filter by direction "Outbound" and check the number of rows displayed
            cy.getByTestId('direction-selector').click();
            cy.getByTestId('direction-selector-inbound').click();
            cy.get('@all-rows').should('have.length', expectedNumberOfInboundMaterials);

            // filter by direction "Inbound" and check the number of rows displayed
            cy.getByTestId('direction-selector').click();
            cy.getByTestId('direction-selector-outbound').click();
            cy.get('@all-rows').should('have.length', expectedNumberOfOutboundMaterials);

            // filter by direction "All" and check the number of rows displayed
            cy.getByTestId('direction-selector').click();
            cy.getByTestId('direction-selector-all').click();
            cy.get('@all-rows').should('have.length', expectedNumberOfMaterialRows);
        });
    });

    it('allows sorting by property', () => {
        cy.fixture('customer-data.json').then(({ materials }) => {
            const expectedNumberOfMaterialRows = materials.reduce(
                (acc, { materialFlag, productFlag }) => acc + (materialFlag && productFlag ? 2 : !materialFlag && !productFlag ? 0 : 1),
                0
            );
            const properties = ['ownMaterialNumber', 'description', 'daysOfSupply', 'lastUpdatedOn', 'direction'];
            const arraysEqual = (arr1, arr2) => {
                return !arr1.some((value, index) => value !== arr2[index]);
            };

            properties.forEach((property) => {
                // sort by property ascending
                cy.get(`[role="columnheader"][data-field="${property}"]`).click();
                cy.get(`[role="row"][data-rowindex] [data-field="${property}"] > [title]`)
                    .should('have.length', expectedNumberOfMaterialRows)
                    .then((cells) => {
                        const cellTitles = [];
                        cells.each((_, cell) => {
                            cellTitles.push(cell.title);
                        });
                        const sortedCellTitles = [...cellTitles].sort((a, b) => a.localeCompare(b));
                        assert(arraysEqual(cellTitles, sortedCellTitles), `Materials are sorted correctly by ${property} ascending`);
                    });

                // sort by property descending
                cy.get(`[role="columnheader"][data-field="${property}"]`).click();
                cy.get(`[role="row"][data-rowindex] [data-field="${property}"] > [title]`)
                    .should('have.length', expectedNumberOfMaterialRows)
                    .then((cells) => {
                        const cellTitles = [];
                        cells.each((_, cell) => {
                            cellTitles.push(cell.title);
                        });
                        const sortedCellTitles = [...cellTitles].sort((a, b) => b.localeCompare(a));
                        assert(arraysEqual(cellTitles, sortedCellTitles), `Materials are sorted correctly by ${property} descending`);
                    });
            });
        });
    });
});
