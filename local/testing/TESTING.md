# E2E Testing

End-to-end testing for the PURIS FOSS application is done using Cypress. The Cypress environment is run inside a custom docker container based on the `cypress/included` image.

## Prerequisites

* PURIS FOSS application is running locally in docker compose
* the app was seeded with integration test data using the `-i` flag

## Running the tests

The tests can be run using the command:

```shell
cd local/testing/e2e
sh run-e2e.sh
```

This will run the tests in the cli using a headless browser.

## Available browsers

By default tests are run in Google Chrome. In addition tests can also be run using Edge, Firefox, Electron and WebKit to cover the majority of possible users.

To run the tests using a specific browser you can use the `--browser` command line argument. For example if you want to run WebKit:

```shell
cd local/testing/e2e
sh run-e2e.sh --browser webkit
```

**Note:** Webkit is used to test the underlying browser engine of Safari since Safari itself is only available on MacOS. It is installed using Playwright.

## Developing tests

When developing tests, the `run-e2e.sh` command can simply be re-run whenever specs, fixtures or support files change. Only changes to the cypress config should require re-building the docker image.

## Test structure

The test specs are structured in 3 groups:

### General Tests

General test specs test app features independent of materials. Examples of general features include navigation, error handling and static views.

The tests are conducted using the customer application, but shall be representative of both applications.

### Role-based Tests

Customer and Supplier tests are run on their respective application. They are focused on testing material based views and the creation, display and deletion of demands, productions, deliveries as well as stocks.

Exchange of created data is **NOT** subject of role-based tests.
