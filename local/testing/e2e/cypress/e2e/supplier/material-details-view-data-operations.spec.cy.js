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

describe('material data operations', () => {
    beforeEach(() => {
        cy.origin(Cypress.env('supplierUrl'), () => {
            cy.visit('/materials');
        })
        cy.login();
    });

    it('allows creating, displaying and deleting planned production', () => {
        cy.origin(Cypress.env('supplierUrl'), () => {
            // define local versions of the custom commands since they aren't available within cy.origin
            const getByTestId = (testid) => cy.get(`[data-testid="${testid}"]`);
            const selectAutocompleteOption = (testid, option) => {
                getByTestId(testid).click();
                cy.get('.MuiAutocomplete-popper').contains(option).click();
                getByTestId(testid).get(`input[value="${option}"]`).should('exist');
            };
            const selectRelativeDate = (testid, dateOffset) => {
                getByTestId(testid).find('[aria-label="Choose date"]').click();
                cy.get('[role="rowgroup"] button').each((button, index, list) => {
                    const backgroundColor = window.getComputedStyle(button[0]).backgroundColor;
                    if (backgroundColor === 'rgb(147, 147, 147)') {
                        cy.wrap(list[index + dateOffset]).click();
                    }
                });
            };
            cy.fixture('supplier-data.json').then(({production}) => {
                cy.visit(`/materials/outbound/${production.ownMaterialNumber}`);
                
                // open production modal
                getByTestId('add-production-button').click();
                getByTestId('production-modal').should('be.visible');
                
                // submitting an empty form should cause errors in mandatory fields
                getByTestId('save-production-button').click();
                getByTestId('production-site-field').get('input[aria-invalid="true"]').should('exist');
                getByTestId('production-completion-time-field').get('input[aria-invalid="true"]').should('exist');
                getByTestId('production-partner-field').get('input[aria-invalid="true"]').should('exist');
                getByTestId('production-quantity-field').get('input[aria-invalid="true"]').should('exist');
                getByTestId('production-uom-field').get('input[aria-invalid="true"]').should('exist');

                // fill out the form with the supplied test production
                selectAutocompleteOption('production-site-field', production.site);
                selectRelativeDate('production-completion-time-field', 0)
                getByTestId('production-quantity-field').type(production.quantity);
                selectAutocompleteOption('production-uom-field', production.measurementUnit);
                selectAutocompleteOption('production-partner-field', production.partner);
                getByTestId('production-customer-order-number-field').type(production.customerOrderNumber);
                getByTestId('production-customer-order-position-field').type(production.customerPositionId);
                getByTestId('production-supplier-order-number-field').type(production.supplierOrderNumber);

                // submit the test production
                getByTestId('save-production-button').click();
                getByTestId('production-modal').should('not.exist');
                getByTestId('toast-success').should('be.visible');

                // expand the current calendar week if necessary and open the production modal for the current day
                const targetWeekIndex = new Date().getDay() === 1 ? 1 : 0;
                cy.get(`[data-testid*="cw-summary"]`).eq(targetWeekIndex).then(($cw) => {
                    if (!$cw.attr('aria-expanded')) {
                    cy.wrap($cw).find('h4 + button').click();
                    }
                });
                cy.get(`[data-testid*="cw-summary"]`).eq(targetWeekIndex).find(`[data-testid*="day-${new Date().getDay()}"] button`).eq(0).click();
                getByTestId('production-modal').should('be.visible');
                
                // check that the added production is displayed in the table and delete it
                cy.get('[role="rowgroup"] [role="row"]')
                    .filter(`:contains(${production.quantity} ${production.measurementUnit})`)
                    .filter(`:contains(${production.partner})`)
                    .filter(`:contains(${production.customerOrderNumber})`)
                    .filter(`:contains(${production.customerPositionId})`)
                    .filter(`:contains(${production.supplierOrderNumber})`)
                    .first()
                    .as('matchingRow');
                cy.get('@matchingRow').should('exist').find('[data-testid="delete-production"]').click();
            });
        });
    })
});