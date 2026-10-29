# Participants

> [!Tip]
> This Case Data is a subset of the [community days setup](../../../etx-community-days/CASE_DATA.md). Atlas is replaced with CCU and Belfast with Semi.

<!-- TOC -->

- [Participants](#participants)
  - [1 Partner "Control Unit Creator" (BPNL000000000ZH5)](#1-partner-control-unit-creator-bpnl000000000zh5)
    - [1.1 Product "Vehicle"](#11-product-vehicle)
      - [1.1.1 Product "Vehicle" Information](#111-product-vehicle-information)
      - [1.1.2 Partner "Control Unit Creator" Supplies Product "Vehicle" to](#112-partner-control-unit-creator-supplies-product-vehicle-to)
      - [1.1.3 BoM of Product "Vehicle"](#113-bom-of-product-vehicle)
      - [1.1.4 Partner "Control Unit Creator" Buys Materials for Product "Vehicle" from](#114-partner-control-unit-creator-buys-materials-for-product-vehicle-from)
  - [2 Partner "Semiconductor Supplier" (BPNL000000000YHU)](#2-partner-semiconductor-supplier-bpnl000000000yhu)
    - [2.1 Product "Gearbox"](#21-product-gearbox)
      - [2.1.1 Product "Gearbox" Information](#211-product-gearbox-information)
      - [2.1.2 Partner "Semiconductor Supplier" Supplies Product "Gearbox" to](#212-partner-semiconductor-supplier-supplies-product-gearbox-to)
      - [2.1.3 BoM of Product "Gearbox"](#213-bom-of-product-gearbox)
      - [2.1.4 Partner "Semiconductor Supplier" Buys Materials for Product "Gearbox" from](#214-partner-semiconductor-supplier-buys-materials-for-product-gearbox-from)
    - [2.2 Product "Tire Model A"](#22-product-tire-model-a)
      - [2.2.1 Product "Tire Model A" Information](#221-product-tire-model-a-information)
      - [2.2.2 Partner "Semiconductor Supplier" Supplies Product "Tire Model A" to](#222-partner-semiconductor-supplier-supplies-product-tire-model-a-to)
      - [2.2.3 BoM of Product "Tire Model A"](#223-bom-of-product-tire-model-a)
      - [2.2.4 Partner "Semiconductor Supplier" Buys Materials for Product "Tire Model A" from](#224-partner-semiconductor-supplier-buys-materials-for-product-tire-model-a-from)
  - [3 Partner "Ceres" (BPNL000000011XBH)](#3-partner-ceres-bpnl000000011xbh)
    - [3.1 Product "Sensor"](#31-product-sensor)
      - [3.1.1 Product "Sensor" Information](#311-product-sensor-information)
      - [3.1.2 Partner "Ceres" Supplies Product "Sensor" to](#312-partner-ceres-supplies-product-sensor-to)
      - [3.1.3 BoM of Product "Sensor"](#313-bom-of-product-sensor)
      - [3.1.4 Partner "Ceres" Buys Materials for Product "Sensor" from](#314-partner-ceres-buys-materials-for-product-sensor-from)
  - [4 Partner "Bristol" (BPNL000000011YAS)](#4-partner-bristol-bpnl000000011yas)
    - [4.1 Product "ECU1"](#41-product-ecu1)
      - [4.1.1 Product "ECU1" Information](#411-product-ecu1-information)
      - [4.1.2 Partner "Bristol" Supplies Product "ECU1" to](#412-partner-bristol-supplies-product-ecu1-to)
      - [4.1.3 BoM of Product "ECU1"](#413-bom-of-product-ecu1)
      - [5.1.4 Partner "Bristol" Buys Materials for Product "ECU1" from](#514-partner-bristol-buys-materials-for-product-ecu1-from)
  - [NOTICE](#notice)

<!-- TOC -->

| #Chapter | NAME                   | BPNL             | SITES                              | ADDRESSES                          | Difficulty |
| -------- | ---------------------- | ---------------- | ---------------------------------- | ---------------------------------- | ---------- |
| 1        | Control Unit Creator   | BPNL000000000ZH5 | BPNS4444444444XX                   | BPNA4444444444AA                   | Easy+      |
| 2        | Semiconductor Supplier | BPNL000000000YHU | BPNS1234567890ZZ, BPNS2222222222SS | BPNA1234567890AA, BPNA2222222222AA | Hard       |
| 3        | Ceres                  | BPNL000000011XBH | BPNS000000011XBH                   | BPNA000000011XBH                   | Normal+    |
| 4        | Bristol                | BPNL000000011YAS | BPNS000000011YAS                   | BPNA000000011YAS                   | Normal+    |

Table 1: *Overview of Participants*

## 1 Partner "Control Unit Creator" (BPNL000000000ZH5)

### 1.1 Product "Vehicle"

#### 1.1.1 Product "Vehicle" Information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name    |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | -------------------- |
| Vehicle Model B            | FJ-87                | FJ-87            | urn:uuid:68904173-ad59-4a77-8412-3e73fcafbd8b | BPNL000000000ZH5  | Control Unit Creator |

#### 1.1.2 Partner "Control Unit Creator" Supplies Product "Vehicle" to

None

#### 1.1.3 BoM of Product "Vehicle"

- urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 (BPNL000000000YHU, Semiconductor Supplier)
- urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b (BPNL000000011YAS, Bristol)
- urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 (BPNL000000000YHU, Semiconductor Supplier)

#### 1.1.4 Partner "Control Unit Creator" Buys Materials for Product "Vehicle" from

| Material Name Manufacturer     | Manufacturer Part ID | Customer Part ID                 | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name      |
| ------------------------------ | -------------------- | -------------------------------- | --------------------------------------------- | ----------------- | ---------------------- |
| Semiconductor Supplier Gearbox | 32494586-73          | Semiconductor Supplier-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL000000000YHU  | Semiconductor Supplier |
| Tire Model A                   | 123564887-01         | TIRE-MODEL-A1                    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL000000000YHU  | Semiconductor Supplier |
| Bristol ECU1                   | ZX-55                | BRISTOL-ECU1                     | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL000000011YAS  | Bristol                |

## 2 Partner "Semiconductor Supplier" (BPNL000000000YHU)

### 2.1 Product "Gearbox"

#### 2.1.1 Product "Gearbox" Information

| Material Name Manufacturer     | Manufacturer Part ID | Customer Part ID                 | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name      |
| ------------------------------ | -------------------- | -------------------------------- | --------------------------------------------- | ----------------- | ---------------------- |
| Semiconductor Supplier Gearbox | 32494586-73          | Semiconductor Supplier-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL000000000YHU  | Semiconductor Supplier |

#### 2.1.2 Partner "Semiconductor Supplier" Supplies Product "Gearbox" to

| Material Name Manufacturer     | Manufacturer Part ID | Customer Part ID                 | Catena-X ID                                   | BPNL Customer    | Customer Name        |
| ------------------------------ | -------------------- | -------------------------------- | --------------------------------------------- | ---------------- | -------------------- |
| Semiconductor Supplier Gearbox | 32494586-73          | Semiconductor Supplier-GEARBOX-1 | urn:uuid:2c57b0e9-a653-411d-bdcd-64787e9fd3a7 | BPNL000000000ZH5 | Control Unit Creator |

#### 2.1.3 BoM of Product "Gearbox"

- urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc (BPNL000000011XBH, Ceres)

#### 2.1.4 Partner "Semiconductor Supplier" Buys Materials for Product "Gearbox" from

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Ceres Sensor               | 6740244-02           | CERES-SENSOR-1   | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL000000011XBH  | Ceres             |

### 2.2 Product "Tire Model A"

#### 2.2.1 Product "Tire Model A" Information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name      |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ---------------------- |
| Tire Model A               | 123564887-01         | TIRE-MODEL-A1    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL000000000YHU  | Semiconductor Supplier |

#### 2.2.2 Partner "Semiconductor Supplier" Supplies Product "Tire Model A" to

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name        |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | -------------------- |
| Tire Model A               | 123564887-01         | TIRE-MODEL-A1    | urn:uuid:e8c48a8e-d2d7-43d9-a867-65c70c85f5b8 | BPNL000000000ZH5 | Control Unit Creator |

#### 2.2.3 BoM of Product "Tire Model A"

- urn:uuid:94d086c6-0124-4f2c-86b2-1d419e47499d (BPNL00000000071A, Caucho)

#### 2.2.4 Partner "Semiconductor Supplier" Buys Materials for Product "Tire Model A" from

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | ----------- | ----------------- | ----------------- |
|                            |                      |                  |             |                   |                   |

## 3 Partner "Ceres" (BPNL000000011XBH)

### 3.1 Product "Sensor"

#### 3.1.1 Product "Sensor" Information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Ceres Sensor               | 6740244-02           | CERES-SENSOR-1   | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL000000011XBH  | Ceres             |

#### 3.1.2 Partner "Ceres" Supplies Product "Sensor" to

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name          |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | ---------------------- |
| Ceres Sensor               | 6740244-02           | CERES-SENSOR-1   | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL000000000YHU | Semiconductor Supplier |
| Ceres Sensor               | 6740244-02           | CERES-SENSOR-2   | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL000000011YAS | Bristol                |

#### 3.1.3 BoM of Product "Sensor"

- urn:uuid:86f69643-3b90-4e34-90bf-789edcf40e7e (BPNL00000000061Z, Delta)

#### 3.1.4 Partner "Ceres" Buys Materials for Product "Sensor" from

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | ----------- | ----------------- | ----------------- |
|                            |                      |                  |             |                   |                   |

## 4 Partner "Bristol" (BPNL000000011YAS)

### 4.1 Product "ECU1"

#### 4.1.1 Product "ECU1" Information

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Bristol ECU1               | ZX-55                | BRISTOL-ECU1     | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL000000011YAS  | Bristol           |

#### 4.1.2 Partner "Bristol" Supplies Product "ECU1" to

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Customer    | Customer Name        |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ---------------- | -------------------- |
| Bristol ECU1               | ZX-55                | BRISTOL-ECU1     | urn:uuid:07cb071f-8716-45fe-89f1-f2f77a1ce93b | BPNL000000000ZH5 | Control Unit Creator |

#### 4.1.3 BoM of Product "ECU1"

- urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc (BPNL000000011XBH, Ceres)

#### 5.1.4 Partner "Bristol" Buys Materials for Product "ECU1" from

| Material Name Manufacturer | Manufacturer Part ID | Customer Part ID | Catena-X ID                                   | BPNL Manufacturer | Manufacturer Name |
| -------------------------- | -------------------- | ---------------- | --------------------------------------------- | ----------------- | ----------------- |
| Ceres Sensor               | 6740244-02           | CERES-SENSOR-2   | urn:uuid:bee5614f-9e46-4c98-9209-61a6f2b2a7fc | BPNL000000011XBH  | Ceres             |

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

This test data has been built on [test data set](https://github.com/eclipse-tractusx/item-relationship-service/tree/a6e5dd317a5168638f6e4e07bf0edff557504335/local/testing/testdata) of the IRS Team who use [CC-BY-4.0 as their license, too](https://github.com/eclipse-tractusx/item-relationship-service/blob/a6e5dd317a5168638f6e4e07bf0edff557504335/LICENSE_non-code). It has been adapted to be easier understandable to beginners.

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
- SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/puris>
