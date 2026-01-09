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

describe('material details view', () => {
    beforeEach(() => {
        cy.login('customer');
        cy.visit('/materials');
        cy.url().should('include', '/materials'); // confirm page load
        cy.wait(1000);
    })

    it('exists and contains all necessary components for all test materials', () => {
        cy.fixture('customer-data.json').then(({ partners, materials }) => {
            materials.forEach((material) => {
                const directions = [];
                if (material.materialFlag) directions.push('Inbound');
                if (material.productFlag) directions.push('Outbound');

                directions.forEach((direction) => {
                    cy.visit(`/materials/${direction.toLowerCase()}/${material.ownMaterialNumber}`);
                    
                    cy.verifyConfidentialityBanner();
                    cy.verifyCopyrightFooter();

                    cy.getByTestId('back-button').should('exist');

                    const informationType = direction === 'Inbound' ? 'Demand' : 'Production';
                    cy.contains(`${informationType} Information for ${material.name} (${material.ownMaterialNumber}), (${direction})`).should('exist');

                    // check if the correct action buttons are visible
                    let applicablePartners = [];
                    if (direction === 'Inbound') {
                        cy.getByTestId('add-demand-button').should('exist');
                        cy.getByTestId('add-production-button').should('not.exist');
                        applicablePartners = partners.filter(p => p.supplies.includes(material.ownMaterialNumber))
                    } else {
                        cy.getByTestId('add-demand-button').should('not.exist');
                        cy.getByTestId('add-production-button').should('exist');
                        applicablePartners = partners.filter(p => p.buys.includes(material.ownMaterialNumber))
                    }
                    cy.getByTestId('add-stock-button').should('exist');
                    cy.getByTestId('add-delivery-button').should('exist');
                    cy.getByTestId('schedule-erp-button').should('exist');
                    cy.getByTestId('refresh-partner-data-button').should('exist');

                    // check that the appropriate number of summary panels is shown
                    cy.getByTestId('own-summary-panel').should('have.length', 2).each(panel => {
                        cy.wrap(panel).contains(direction === 'Inbound' ? 'Material Demand' : 'Planned Production').should('exist');
                        cy.wrap(panel).contains(direction === 'Inbound' ? 'Incoming Deliveries' : 'Outgoing Shipments').should('exist');
                        cy.wrap(panel).contains('Projected Item Stock').should('exist');
                        cy.wrap(panel).contains('Days of Supply').should('exist');
                    });

                    applicablePartners.forEach(partner => {
                        // check that the summary for the partner is collapsed by default
                        cy.getByTestId(`reported-summary-panel-${Cypress.env('supplier').bpnl}`).as('partner-panel').should('not.be.visible');
                        // check that the row labels are correct
                        cy.get('@partner-panel').contains(direction === 'Outbound' ? 'Material Demand' : 'Planned Production').should('exist');
                        cy.get('@partner-panel').contains(direction === 'Outbound' ? 'Incoming Deliveries' : 'Outgoing Shipments').should('exist');
                        cy.get('@partner-panel').contains('Projected Item Stock').should('exist');
                        // check that the collapsible shows the partner name and bpnl
                        cy.getByTestId(`collapsible-summary-button-${Cypress.env('supplier').bpnl}`).as('partner-button').should('exist');
                        cy.get('@partner-button').contains(partner.name);
                        cy.get('@partner-button').contains(Cypress.env('supplier').bpnl);

                        // click the collapsible button and verify that the partner summary is shown
                        cy.get('@partner-button').click();
                        cy.get('@partner-panel').should('be.visible');
                        
                        partner.sites.forEach(site => {
                            // check that the summary for the partner site is collapsed by default
                            cy.getByTestId(`reported-summary-panel-${site.bpns}`).as('site-panel').should('not.be.visible');
                            // check that the row labels are correct
                            cy.get('@site-panel').contains(direction === 'Outbound' ? 'Material Demand' : 'Planned Production').should('exist');
                            cy.get('@site-panel').contains(direction === 'Outbound' ? 'Incoming Deliveries' : 'Outgoing Shipments').should('exist');
                            cy.get('@site-panel').contains('Projected Item Stock').should('exist');
                            cy.get('@site-panel').contains('Days of Supply').should('exist');

                            // check that the collapsible shows the partner name, site name and bpns
                            cy.getByTestId(`collapsible-summary-button-${site.bpns}`).as('site-button').should('exist');
                            cy.get('@site-button').contains(partner.name);
                            cy.get('@site-button').contains(site.name);
                            cy.get('@site-button').contains(site.bpns);

                            // click the collapsible button and verify that the site summary is shown
                            cy.get('@site-button').click();
                            cy.get('@site-panel').should('be.visible');
                        });
                    })
                });
            });
        });
    });
});
