# Running Integration Tests

The collection has two folders:

1. One folder to create a test setup and test most of the master data interfaces
2. One folder to test the created assets via catalog requests.

It creates a setup of a partner and customer that refer to the same material from the other perspective:

- The customer has the material and a material stock
- The supplier has the product and a product stock
- Both partners know each other and each other's EDC
- The setup is ready to trigger the update from customer to supplier

## Test Data

The following lists give an overview about the test data in use. When setting up the data, please consider that the
relevant information is the following:

- Material Numbers (Customer, Supplier, Catena-X)
- Partner and Site information (BPNL, BPNS, BPNA)
  Note: BPN information depends highly on your environment.

Items (Material or Product) involved:

- Semiconductor
    - Name: Semiconductor
    - Material Number Customer: MNR-7307-AU340474.002
    - Material Number Supplier: MNR-8101-ID146955.001
    - Material Number Catena-X: 860fb504-b884-4009-9313-c6fb6cdc776b
- Control Unit
    - Name: Central Control Unit
    - Material Number Customer: MNR-4177-C
    - Material Number Supplier: MNR-4177-S
    - Material Number Catena-X: none

### Customer

Overall the customer has the following information:

- BPNL: BPNL4444444444XX
- Name: Control Unit Creator Inc.
- Site
    - BPNS: BPNS4444444444XX
    - Site Name: Control Unit Creator Production Site
    - Site Address:
        - BPNA: BPNA4444444444AA
        - Street and Number: 13th Street 47
        - Zip Code, City: 10011 New York
        - Country: USA
- Materials available: Semiconductor
- Products available: Control Unit
    - Material Stock 1:
        - Partner: Semiconductor Supplier (BPNL1234567890ZZ)
        - Material: Semiconductor
        - Quantity: 500 pieces
        - Blocked: true
        - Location BPNS: BPNS4444444444XX
        - Location BPNA: BPNA4444444444AA
        - Order Position Reference:
            - Customer Order Number: CNbr-1
            - Customer Order Position Number: C-Pos-1
            - Supplier Order Number: SNbr-1

### Demand

There are 5 Demands created by the collection. They all share the same

- material (semiconductor)
- partner (scenario partner)
- demandLocationBpns (CUSTOMER_BPNS)

The following table shows the differences.

| ETA        | Quantity   | Category       | Supplier Location BPNS set |
|------------|------------|----------------|----------------------------|
| now        | 500 pieces | Default (0001) | yes                        |
| now +1 day | 510 pieces | (A1S1)         | yes                        |
| now +2 day | 500 pieces | Series (SR99)  | yes                        |
| now +3 day | 500 pieces | Series (SR99)  | yes                        |
| now +4 day | 400 pieces | Series (SR99)  | no                         |

### Delivery

There are 5 Deliveries created for each collection. They all share the same

- originBpns (SUPPLIER_BPNS)
- destinationBpns (CUSTOMER_BPNS)

#### Supplier deliveries

- ownMaterialNumber (MATERIAL_NUMBER_SUPPLIER)
- partnerBpnl (CUSTOMER_BPNL)

| Departure   | Type       | Arrival      | Type       | Quantity   | Incoterm       | Origin BPNA set | Destination BPNA set | Customer Order Number | Customer Position Number | Supplier Order Number |
|-------------|------------|--------------|------------|------------|----------------|-----------------|----------------------|-----------------------|--------------------------|-----------------------|
| now -1 day  | actual     | now          | actual     | 50  pieces | FAS            | yes             | yes                  | null                  | null                     | null                  |
| now         | actual     | now + 1 days | estimated  | 100 pieces | DAP            | yes             | yes                  | null                  | null                     | null                  |
| now +1 day  | estimated  | now + 2 days | estimated  | 200 pieces | DPU            | yes             | no                   | C-Nbr-1               | C-Position-01            | null                  |
| now +2 days | estimated  | now + 3 days | estimated  | 300 pieces | CPT            | no              | yes                  | C-Nbr-1               | C-Position-01            | S-Nbr-1               |
| now +3 days | estimated  | now + 4 days | estimated  | 400 pieces | CIP            | no              | no                   | null                  | null                     | null                  |

#### Customer deliveries

- ownMaterialNumber (MATERIAL_NUMBER_CUSTOMER)
- partnerBpnl (SUPPLIER_BPNL)

| Departure   | Type       | Arrival      | Type       | Quantity   | Incoterm       | Origin BPNA set | Destination BPNA set | Customer Order Number | Customer Position Number | Supplier Order Number |
|-------------|------------|--------------|------------|------------|----------------|-----------------|----------------------|-----------------------|--------------------------|-----------------------|
| now -1 day  | actual     | now          | actual     | 50  pieces | CIF            | yes             | yes                  | null                  | null                     | null                  |
| now         | actual     | now + 1 days | estimated  | 100 pieces | EXW            | yes             | yes                  | null                  | null                     | null                  |
| now +1 day  | estimated  | now + 2 days | estimated  | 200 pieces | FAS            | yes             | no                   | C-Nbr-1               | C-Position-01            | null                  |
| now +2 days | estimated  | now + 3 days | estimated  | 300 pieces | FOB            | no              | yes                  | C-Nbr-1               | C-Position-01            | S-Nbr-1               |
| now +3 days | estimated  | now + 4 days | estimated  | 400 pieces | CFR            | no              | no                   | null                  | null                     | null                  |

### Supplier

Overall the supplier has the following information:

- BPNL: BPNL1234567890ZZ
- Name: Semiconductor Supplier Inc.
- Site 1
    - BPNS: BPNS1234567890ZZ
    - Site Name: Semiconductor Supplier Inc. Production Site
    - Site Address:
        - BPNA: BPNA1234567890AA
        - Street and Number: Wall Street 101
        - Zip Code, City: 10001 New York
        - Country: USA
- Site 2
    - BPNS: BPNS2222222222SS
    - Site Name: Semiconductor Supplier Inc. Secondary Site
    - Site Address:
        - BPNA: BPNA2222222222AA
        - Street and Number: Sunset Blvd. 345
        - Zip Code, City: 90001 Los Angeles
        - Country: USA
- Materials available: none
- Products available: Semiconductor
- Stocks
    - Product Stock 1:
        - Partner: Control Unit Creator Inc. (BPNL4444444444XX)
        - Material: Semiconductor
        - Quantity: 100 pieces
        - Blocked: true
        - Location BPNS: BPNS1234567890ZZ
        - Location BPNA: BPNA1234567890AA
        - Order Position Reference:
            - Customer Order Number: CNbr-2
            - Customer Order Position Number: C-Pos-2
            - Supplier Order Number: SNbr-2
    - Product Stock 2:
        - Partner: Control Unit Creator Inc. (BPNL4444444444XX)
        - Material: Semiconductor
        - Quantity: 400 pieces
        - Blocked: false
        - Order Position Reference:
            - Customer Order Number: CNbr-2
            - Customer Order Position Number: C-Pos-2
            - Supplier Order Number: SNbr-2

### Production

There are 6 Production Outputs created by the collection. They all share the same

- material (semiconductor)
- partner (scenario partner)
- productionSiteBpns (SUPPLIER_BPNS)

The following table shows the differences.

| ETA                | Quantity   | Order Position Reference (customer order, customer order position, supplier order) |
|--------------------|------------|------------------------------------------------------------------------------------|
| now                | 600 pieces | CNbr-2, C-Pos-2, SNbr-2                                                            |
| now +1 day         | 600 pieces | none                                                                               |
| now +2 day         | 550 pieces | CNbr-2, C-Pos-2, SNbr-2                                                            |
| now +3 day         | 650 pieces | none                                                                               |
| now +4 day         | 200 pieces | none                                                                               |
| now +4 day +1 hour | 300 pieces | none                                                                               |

## Preparations

To run integration tests import the collection and the environment file to postman.
The environment files has empty fields for the respective key information. The keys are set via environment variables
on folder level.

One can use the local deployment to run the integration test locally. Follow the instructions in the
[INSTALL.md](../INSTALL.md) until the keys are generated.

- The api keys needed by the environment can be copied from the [environment file](../.env) that is generated by the
  [generate-keys.sh](../generate-keys.sh) script.
- Adjust the properties file to run without `puris.demonstrator.role`. Find the properties files for the puris backend
  of the [customer](../tractus-x-edc/config/customer/puris-backend.properties) and of
  the [supplier](../tractus-x-edc/config/supplier/puris-backend.properties) to set the property
  `puris.demonstrator.role` to empty value.

Same settings should be used for other environments. But you also need to update other variables accordingly in the
environment file.

## Run the integration test

You can run the integration tests on per folder level using right click > run folder.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
