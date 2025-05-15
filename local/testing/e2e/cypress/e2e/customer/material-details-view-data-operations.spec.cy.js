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
        cy.visit('/materials');
        cy.login();
    })

    it('allows creating, displaying and deleting material demand', () => {
        cy.fixture('customer-data.json').then(({demand}) => {
            cy.visit(`/materials/inbound/${demand.ownMaterialNumber}`);
            
            // open demand modal
            cy.getByTestId('add-demand-button').click();
            cy.getByTestId('demand-modal').should('be.visible');
            
            // check fields for default values and clear them
            cy.getByTestId('demand-category-field').find('input[value="Default"]').should('exist');
            cy.clearAutocompleteSelection('demand-category-field');

            // submitting an empty form should cause errors in mandatory fields
            cy.getByTestId('save-demand-button').click();
            cy.getByTestId('demand-location-field').find('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('demand-day-field').find('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('demand-category-field').find('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('demand-quantity-field').find('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('demand-uom-field').find('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('demand-partner-field').find('input[aria-invalid="true"]').should('exist');
            
            // fill out the form with the supplied test demand
            cy.selectAutocompleteOption('demand-location-field', demand.demandSite);
            cy.selectRelativeDate('demand-day-field', 0)
            cy.selectAutocompleteOption('demand-category-field', demand.demandCategory);
            cy.getByTestId('demand-quantity-field').type(demand.quantity);
            cy.selectAutocompleteOption('demand-uom-field', demand.measurementUnit);
            cy.getByTestId('demand-supplier-site-field').find('input:disabled').should('exist'); // check if supplier site field is disabled before entering partner
            cy.selectAutocompleteOption('demand-partner-field', demand.partner);
            cy.getByTestId('demand-supplier-site-field').find('input:disabled').should('not.exist'); // check if supplier site field is enabled after entering partner
            cy.selectAutocompleteOption('demand-supplier-site-field', demand.supplierSite);

            // submit the test demand
            cy.getByTestId('save-demand-button').click();
            cy.getByTestId('demand-modal').should('not.exist');
            cy.getByTestId('toast-success').should('be.visible');

            // expand the current calendar week if necessary and open the demand modal for the current day
            const targetWeekIndex = new Date().getDay() === 1 ? 1 : 0;
            cy.getByTestIdContains('cw-summary').eq(targetWeekIndex).then(($cw) => {
                if (!$cw.attr('aria-expanded')) {
                  cy.wrap($cw).find('h4 + button').click();
                }
              });
            cy.getByTestIdContains('cw-summary').eq(targetWeekIndex).find(`[data-testid*="day-${new Date().getDay()}"] button`).eq(0).click();
            cy.getByTestId('demand-modal').should('be.visible');
            
            // check that the added demand is displayed in the table and delete it
            cy.get('[role="rowgroup"] [role="row"]')
              .filter(`:contains(${demand.quantity} ${demand.measurementUnit})`)
              .filter(`:contains(${Cypress.env('supplier').bpnl})`)
              .filter(`:contains(${demand.supplierSiteBpns})`)
              .filter(`:contains(${demand.demandCategory})`)
              .first()
              .as('matchingRow');
            cy.get('@matchingRow').should('exist').find('[data-testid="delete-demand"]').click();
        });
    })

    it('allows creating, displaying and deleting incoming deliveries', () => {
        cy.fixture('customer-data.json').then(({delivery}) => {
            cy.visit(`/materials/inbound/${delivery.ownMaterialNumber}`);
            
            // open delivery modal
            cy.getByTestId('add-delivery-button').click();
            cy.getByTestId('delivery-modal').should('be.visible');

            // check fields for default values and clear them
            cy.getByTestId('delivery-departure-type-field').find('input[value="Estimated"]').should('exist');
            cy.clearAutocompleteSelection('delivery-departure-type-field');
            cy.getByTestId('delivery-arrival-type-field').find('input[value="Estimated"]').should('exist');
            cy.clearAutocompleteSelection('delivery-arrival-type-field');
            
            // submitting an empty form should cause errors in mandatory fields
            cy.getByTestId('save-delivery-button').click();
            cy.getByTestId('delivery-own-bpns-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-departure-type-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-arrival-type-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-departure-time-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-arrival-time-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-partner-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-partner-bpns-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-quantity-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-uom-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('delivery-incoterm-field').get('input[aria-invalid="true"]').should('exist');
            
            // fill out the form with the supplied test delivery
            cy.selectAutocompleteOption('delivery-own-bpns-field', delivery.destination);
            cy.selectAutocompleteOption('delivery-departure-type-field', 'Estimated');
            cy.selectAutocompleteOption('delivery-arrival-type-field', 'Estimated');
            cy.selectRelativeDate('delivery-departure-time-field', 0);
            cy.selectRelativeDate('delivery-arrival-time-field', 2);
            cy.getByTestId('delivery-partner-bpns-field').find('input:disabled').should('exist'); // check if origin field is disabled before entering partner
            cy.selectAutocompleteOption('delivery-partner-field', delivery.partner);
            cy.getByTestId('delivery-partner-bpns-field').find('input:disabled').should('not.exist'); // check if origin field is enabled after entering partner
            cy.selectAutocompleteOption('delivery-partner-bpns-field', delivery.origin);
            cy.getByTestId('delivery-quantity-field').type(delivery.quantity);
            cy.selectAutocompleteOption('delivery-uom-field', delivery.measurementUnit);
            cy.getByTestId('delivery-tracking-number-field').type(delivery.trackingNumber);
            cy.selectAutocompleteOption('delivery-incoterm-field', delivery.incoterm);
            cy.getByTestId('delivery-customer-order-number-field').type(delivery.customerOrderNumber);
            cy.getByTestId('delivery-customer-order-position-field').type(delivery.customerPositionId);
            cy.getByTestId('delivery-supplier-order-number-field').type(delivery.supplierOrderNumber);

            // submit the test delivery
            cy.getByTestId('save-delivery-button').click();
            cy.getByTestId('delivery-modal').should('not.exist');
            cy.getByTestId('toast-success').should('be.visible');

            // expand the appropriate calendar week if necessary and open the delivery modal for the day after tomorrow
            const targetWeekIndex = new Date().getDay() + 2 > 6 ? 1 : 0;
            cy.getByTestIdContains('cw-summary').eq(targetWeekIndex).then(($cw) => {
                if (!$cw.attr('aria-expanded')) {
                  cy.wrap($cw).find('h4 + button').click();
                }
              });
            cy.getByTestIdContains('cw-summary').eq(targetWeekIndex).find(`[data-testid*="day-${(new Date().getDay() + 2) % 7}"] button`).eq(1).click();

            // check that the added delivery is displayed in the table and delete it
            cy.get('[role="rowgroup"] [role="row"]')
              .filter(`:contains(${delivery.quantity} ${delivery.measurementUnit})`)
              .filter(`:contains(${Cypress.env('supplier').bpnl})`)
              .filter(`:contains(${delivery.customerOrderNumber})`)
              .filter(`:contains(${delivery.customerPositionId})`)
              .filter(`:contains(${delivery.supplierOrderNumber})`)
              .filter(`:contains(${delivery.trackingNumber})`)
              .filter(`:contains(${delivery.incoterm})`)
              .first()
              .as('matchingRow');
            cy.get('@matchingRow').should('exist').find('[data-testid="delete-delivery"]').click();
        });
    })

    it('allows creating, displaying and deleting stocks', () => {
        cy.fixture('customer-data.json').then(({stock}) => {
            cy.visit(`/materials/inbound/${stock.ownMaterialNumber}`);
            
            // open stock modal
            cy.getByTestId('add-stock-button').click();
            cy.getByTestId('stock-modal').should('be.visible');
            
            // submitting an empty form should cause errors in mandatory fields
            cy.getByTestId('save-stock-button').click();
            cy.getByTestId('stock-partner-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('stock-site-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('stock-address-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('stock-quantity-field').get('input[aria-invalid="true"]').should('exist');
            cy.getByTestId('stock-uom-field').get('input[aria-invalid="true"]').should('exist');

            // fill out the form with the supplied test stock
            cy.selectAutocompleteOption('stock-partner-field', stock.partner);
            cy.getByTestId('stock-address-field').find('input:disabled').should('exist'); // check if address field is disabled before entering site
            cy.selectAutocompleteOption('stock-site-field', stock.site);
            cy.getByTestId('stock-address-field').find('input:disabled').should('not.exist'); // check if address field is enabled after entering site
            cy.selectAutocompleteOption('stock-address-field', stock.address);
            cy.getByTestId('stock-quantity-field').type(stock.quantity);
            cy.selectAutocompleteOption('stock-uom-field', stock.measurementUnit);
            cy.getByTestId('stock-customer-order-number-field').type(stock.customerOrderNumber);
            cy.getByTestId('stock-customer-order-position-field').type(stock.customerPositionId);
            cy.getByTestId('stock-supplier-order-number-field').type(stock.supplierOrderNumber);

            if (stock.isBlocked) {
                cy.getByTestId('stock-is-blocked-field').click();
            }

            // submit the test stock
            cy.getByTestId('save-stock-button').click();
            cy.getByTestId('stock-modal').should('not.exist');
            cy.getByTestId('toast-success').should('be.visible');

            cy.getByTestId('actual-stock').first().click()
            cy.getByTestId('stock-modal').should('be.visible');

            // check that the added stock is displayed in the table and delete it
            cy.get('[role="rowgroup"] [role="row"]')
              .filter(`:contains(${stock.quantity} ${stock.measurementUnit})`)
              .filter(`:contains(${stock.siteBpns})`)
              .filter(`:contains(${stock.partner})`)
              .filter(`:contains(${stock.customerOrderNumber})`)
              .filter(`:contains(${stock.customerPositionId})`)
              .filter(`:contains(${stock.supplierOrderNumber})`)
              .filter(`:contains(${stock.isBlocked ? 'Yes' : 'No'})`)
              .first()
              .as('matchingRow');
            cy.get('@matchingRow').should('exist').find('[data-testid="delete-stock"]').click();
        });
    })
});