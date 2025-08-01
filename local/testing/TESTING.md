# E2E Testing

End-to-end testing for the PURIS FOSS application is done using Cypress. The Cypress environment is run inside a custom docker container based on the `cypress/included` image.

## Prerequisites

* PURIS FOSS application is running locally in docker compose
* the app was seeded with integration test data using the `-i` flag

## Configuring the tests

Before running the tests make sure to configure the `baseUrl` in `cypress.config.json` and `supplierUrl` in `cypress.env.json`. `baseUrl` is expected to match the customer frontend url.

In order to run the tests with login, the environment file `cypress.env.json` needs to be configured with the appropriate information. Set `environment` to `int` if you plan to test in the integration environment. Otherwise leave the default value `local`. Fill in your login information for the IDP. Make sure that the company names match the configured names in the IDP.

## Running the tests

The tests can be run using the command:

```shell
cd local/testing/e2e
sh run-e2e.sh
```

This will run the tests in the cli using a headless browser.

## Available browsers

By default tests are run in Electron. In addition tests can also be run using Edge, Firefox and Chrome to cover the majority of possible users.

To run the tests using a specific browser you can use the `--browser` command line argument. For example if you want to run WebKit:

```shell
cd local/testing/e2e
sh run-e2e.sh --browser chrome
```

## Developing tests

When developing tests, it should not be necessary to rebuild the docker image.

## Test structure

The test specs are structured in 3 groups:

### General Tests

General test specs test app features independent of materials. Examples of general features include navigation, error handling and static views.

The tests are conducted using the customer application, but shall be representative of both applications.

### Role-based Tests

Customer and Supplier tests are run on their respective application. They are focused on testing material based views and the creation, display and deletion of demands, productions, deliveries as well as stocks.

Exchange of created data is **NOT** subject of role-based tests.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
