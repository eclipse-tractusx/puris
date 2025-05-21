# Participants

You will get one of the participants depending on the number of participants.

| NAME                            | BPNL             | BPNS             | BPNA             |
| ------------------------------- | ---------------- | ---------------- | ---------------- |
| oem-b (OEM B)                   | BPNL00000003AVTH | BPNS000000815DMY | BPNA000000815DMY |
| tier-a (Tier A)                 | BPNL00000003B2OM | BPNS00000003B2OM | BPNA00000003B2OM |
| tier-b (Tier B)                 | BPNL00000003B5MJ | BPNS00000003B5MJ | BPNA00000003B5MJ |
| tier-c (Tier C)                 | BPNL00000003CSGV | BPNS00000003CSGV | BPNA00000003CSGV |
| sub-tier-a (Sub Tier A)         | BPNL00000003B3NX | BPNS00000003B3NX | BPNA00000003B3NX |
| sub-tier-b (Sub Tier B)         | BPNL00000003AXS3 | BPNS00000003AXS3 | BPNA00000003AXS3 |
| n-tier (N Tier)                 | BPNL00000003B0Q0 | BPNS00000003B0Q0 | BPNA00000003B0Q0 |
| natural-rubber (Natural Rubber) | BPNL00000007OR16 | BPNS000000000001 | BPNA000000000001 |

Table 1: *Overview of Participants*

## OEM_B (BPNL00000003AVTH)

Priorities:

- Act as Supplier (not applicable)
- Act as Customer

### Vehicle

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Vehicle Model B            | FJ-87                | FJ-87            | urn:uuid:68904173-ad59-4a77-8412-3e73fcafbd8b | BPNL00000003AVTH  | OEM_B             |

#### Supplies products

None

#### BoM

- urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 (BPNL00000003B2OM, Tier A)
- urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b (BPNL00000003B5MJ, Tier B)
- urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 (BPNL00000003B2OM, Tier A)

#### Buys products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Tier A Gearbox             | 32494586-73          | TIER-A-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL00000003B2OM  | TIER_A            |
| Tire Model A               | 123564887-01         | TIRE-MODEL-A1    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL00000003B2OM  | TIER_A            |
| ZX-55                      | Tier B ECU1          | TIER-B-ECU1      | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL00000003B5MJ  | TIER_B            |

## Tier A (BPNL00000003B2OM)

Priorities:

- Act as Supplier
    - Create `Tire` Product
        - Customer 1: `OEM B`
    - Create `Gearbox` Product
        - Customer 1: `OEM B`
- Act as Customer
    - Create `Natural Rubber Product` Material
        - Supplier 1: `Natural Rubber`
    - Create `Tier A Plastics` Material
        - Supplier 1: `N-Tier`
    - Create `Sub Tier A Sensor` Material
        - Supplier 1: `Sub Tier A`

Find the materials / products and the related partner information below.

### Gearbox

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Tier A Gearbox             | 32494586-73          | TIER-A-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL00000003B2OM  | TIER_A            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | ------------- |
| Tier A Gearbox             | 32494586-73          | TIER-A-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL00000003AVTH | OEM_B         |

#### BoM

- urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc (BPNL00000003B3NX, SUB_TIER_A)
- urn:uuid:4518c080-14fb-4252-b8de-4362d615868d (BPNL00000003B0Q0, N_TIER)

#### Buys products (TBD)

| Material Name Manufacturer | Manufacturer Part Id | Customer Part ID    | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ----------------- | ----------------- |
| Sub Tier A Sensor          | 6740244-02           | SUB-TIER-A-SENSOR-1 | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL00000003B3NX  | SUB_TIER_A        |
| N Tier A Plastics          | 7A987KK-04           | N-TIER-A-PLASTICS-1 | urn:uuid:4518c080-14fb-4252-b8de-4362d615868d | BPNL00000003B2OM  | N_TIER            |

### Tire Model A

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Tire Model A               | 123564887-01         | TIRE-MODEL-A1    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL00000003B2OM  | TIER_A            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | ------------- |
| Tire Model A               | 123564887-01         | TIRE-MODEL-A1    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL00000003AVTH | OEM_B         |

#### BoM

- urn:uuid:94d086c6-0124-4f2c-86b2-1d419e47499d (BPNL00000007OR16, Natural Rubber)

#### Buys products

| Material Name Manufacturer           | Manufacturer Part Id | Customer Part ID        | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| ------------------------------------ | -------------------- | ----------------------- | --------------------------------------------- | ----------------- | ----------------- |
| Natural Rubber Product (40KG blocks) | 9953421-03           | NATURAL-RUBBER-BLOCK-40 | urn:uuid:94d086c6-0124-4f2c-86b2-1d419e47499d | BPNL00000007OR16  | Natural Rubber    |

## Sub Tier A (BPNL00000003B3NX)

Priorities:

- Act as Supplier
    - Create `Sub Tier A Sensor` Product
        - Customer 1: `Tier B`
        - Customer 2: `Tier A`
- Act as Customer
    - Create `N-Tier Plastics` Material
        - Supplier 1: `N-Tier`

Find the materials / products and the related partner information below.

### Sensor

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID    | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ----------------- | ----------------- |
| Sub Tier A Sensor          | 6740244-02           | SUB-TIER-A-SENSOR-1 | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL00000003B3NX  | SUB_TIER_A        |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID    | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ---------------- | ------------- |
| Sub Tier A Sensor          | 6740244-02           | SUB-TIER-A-SENSOR-1 | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL00000003B2OM | TIER_A        |
| Sub Tier A Sensor          | 6740244-02           | SUB-TIER-A-SENSOR-2 | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL00000003B5MJ | TIER_B        |

#### BoM

- urn:uuid:86f69643-3b90-4e34-90bf-789edcf40e7e (BPNL00000003B0Q0, N_TIER)

#### Buys products

| Material Name Manufacturer | Manufacturer Part Id | Customer Part ID   | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------ | --------------------------------------------- | ----------------- | ----------------- |
| N Tier A NTier Product     | 7A047KK-01           | N-TIER-A-PRODUCT-1 | urn:uuid:86f69643-3b90-4e34-90bf-789edcf40e7e | BPNL00000003B0Q0  | N_TIER            |

## N Tier A (BPNL00000003B0Q0)

Priorities:

- Act as Supplier
    - Create `N-Tier Product` Product
        - Customer 1: `Sub Tier A`
    - Create `Tier A Plastics` Product
        - Customer 1: `Tier A`
- Act as Customer (not applicable)

Find the materials / products and the related partner information below.

### NTier Product

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID   | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------ | --------------------------------------------- | ----------------- | ----------------- |
| N Tier A NTier Product     | 7A047KK-01           | N-TIER-A-PRODUCT-1 | urn:uuid:86f69643-3b90-4e34-90bf-789edcf40e7e | BPNL00000003B0Q0  | N_TIER            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID   | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ------------------ | --------------------------------------------- | ---------------- | ------------- |
| N Tier A NTier Product     | 7A047KK-01           | N-TIER-A-PRODUCT-1 | urn:uuid:86f69643-3b90-4e34-90bf-789edcf40e7e | BPNL00000003B3NX | SUB_TIER_A    |

#### BoM

none

#### Buys products

none

### N Tier A Plastics

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID    | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ----------------- | ----------------- |
| N Tier A Plastics          | 7A987KK-04           | N-TIER-A-PLASTICS-1 | urn:uuid:4518c080-14fb-4252-b8de-4362d615868d | BPNL00000003B2OM  | TIER_A            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID    | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ---------------- | ------------- |
| N Tier A Plastics          | 7A987KK-04           | N-TIER-A-PLASTICS-1 | urn:uuid:4518c080-14fb-4252-b8de-4362d615868d | BPNL00000003B0Q0 | N_TIER        |

#### BoM

none

#### Buys products

none

## Tier B (BPNL00000003B5MJ)

Priorities:

- Act as Supplier
    - Create `ECU1` Product
        - Customer 1: `OEM B`
- Act as Customer
    - Create `Sub Tier B Glue` Material
        - Supplier 1: `Sub Tier B`
    - Create `Sub Tier A Sensor` Material
        - Supplier 1: `Sub Tier A`

Find the materials / products and the related partner information below.

### ECU1

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Tier B ECU1                | ZX-55                | TIER-B-ECU1      | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL00000003B5MJ  | TIER_B            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | ------------- |
| Tier B ECU1                | ZX-55                | TIER-B-ECU1      | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL00000003AVTH | OEM_B         |

#### BoM

- urn:uuid:3cdd2826-5df0-4c7b-b540-9eeccecb2301 (PNL00000003AXS3, SUB_TIER_B)
- urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc (BPNL00000003B3NX, SUB_TIER_A)

#### Buys products

| Material Name Manufacturer | Manufacturer Part Id | Customer Part ID    | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ------------------- | --------------------------------------------- | ----------------- | ----------------- |
| Sub Tier A Sensor          | 6740244-02           | SUB-TIER-A-SENSOR-2 | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL00000003B3NX  | SUB_TIER_A        |
| Sub Tier B Glue            | 6775244-06           | SUB-TIER-B-GLUE-1   | urn:uuid:3cdd2826-5df0-4c7b-b540-9eeccecb2301 | BPNL00000003AXS3  | SUB_TIER_B        |

## Sub Tier B (BPNL00000003AXS3)

Priorities:

- Act as Supplier
    - Create `Sub tier B Glue` Product
        - Customer 1: `Tier B`
- Act as Customer (not applicable)

Find the materials / products and the related partner information below.

### Glue

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID  | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ----------------- | --------------------------------------------- | ----------------- | ----------------- |
| Sub Tier B Glue            | 6775244-06           | SUB-TIER-B-GLUE-1 | urn:uuid:3cdd2826-5df0-4c7b-b540-9eeccecb2301 | BPNL00000003AXS3  | SUB_TIER_B        |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID  | Catena-X ID                                   | BPNL Customer    | Customer Name |
| -------------------------- | -------------------- | ----------------- | --------------------------------------------- | ---------------- | ------------- |
| Sub Tier B Glue            | 6775244-06           | SUB-TIER-B-GLUE-1 | urn:uuid:3cdd2826-5df0-4c7b-b540-9eeccecb2301 | BPNL00000003B5MJ | TIER_B        |

#### BoM

None

#### Buys products

None

## Natural Rubber (BPNL00000007OR16)

### Natural Rubber Product (40KG blocks)

#### Product information

| Material Name Manufacturer           | Manufacturer Part ID | Customer Part ID        | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| ------------------------------------ | -------------------- | ----------------------- | --------------------------------------------- | ----------------- | ----------------- |
| Natural Rubber Product (40KG blocks) | 9953421-03           | NATURAL-RUBBER-BLOCK-40 | urn:uuid:94d086c6-0124-4f2c-86b2-1d419e47499d | BPNL00000007OR16  | Natural Rubber    |

#### Supplies products

| Material Name Manufacturer           | Manufacturer Part ID | Customer Part ID        | Catena-X ID                                   | BPNL Customer    | Customer Name |
| ------------------------------------ | -------------------- | ----------------------- | --------------------------------------------- | ---------------- | ------------- |
| Natural Rubber Product (40KG blocks) | 9953421-03           | NATURAL-RUBBER-BLOCK-40 | urn:uuid:94d086c6-0124-4f2c-86b2-1d419e47499d | BPNL00000003B2OM | TIER_A        |

#### BoM

- urn:uuid:b0926d3c-6a8f-4fc7-81a4-88c50817358a (BPNL00000003CSGV, Tier C)

#### Buys products

| Material Name Manufacturer | Manufacturer Part Id | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Natural Rubber             | A26581-11            | NATURAL-RUBBER-1 | urn:uuid:b0926d3c-6a8f-4fc7-81a4-88c50817358a | BPNL00000000BJTL  | Tier C            |

## Tier C (BPNL00000000BJTL)

Priorities:

- Act as Supplier
    - Create `Natural Rubber` Product
        - Customer 1: `Natural Rubber`
- Act as Customer (not applicable)

Find the materials / products and the related partner information below.

### Natural Rubber

#### Product information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Natural Rubber             | A26581-11            | NATURAL-RUBBER-1 | urn:uuid:b0926d3c-6a8f-4fc7-81a4-88c50817358a | BPNL00000000BJTL  | TIER C            |

#### Supplies products

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name  |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | -------------- |
| Natural Rubber             | A26581-11            | NATURAL-RUBBER-1 | urn:uuid:b0926d3c-6a8f-4fc7-81a4-88c50817358a | BPNL00000007OR16 | Natural Rubber |

#### BoM

None

#### Buys products

None
