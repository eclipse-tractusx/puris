# Running Integration Tests

To run integration tests import the collection and the environment file to postman.
Follow the description on collection level to identify the secrets (API KEYS) you need to set in the environment 
variables.

The collection has two folders:
1. One folder to create a test setup and test most of the master data interfaces
2. One folder to test the created assets via catalog requests.

It creates a setup of a partner and customer that refer to the same material from the other perspective:
- The customer has the material and a material stock
- The supplier has the product and a product stock
- Both partners know each other and each other's EDC
- The setup is ready to trigger the update from customer to supplier
