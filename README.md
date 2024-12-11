<div align="center">
  <h2 align="center">PURIS</h2>
  The Predictive Unit Real-Time Information Service (PURIS) for Short Term Demand and Capacity Management
</div>

## Overview

The project is made of a backend and a frontend. Look into the respective folders and their documentation to get
information about prerequirements and getting started guides.

## Dependencies

Beside the dependencies provided in the Helm Chart, the following dependencies have been tested for R24.05 to run PURIS:

| Application                                                                                                       | App Version | Chart Version |
|-------------------------------------------------------------------------------------------------------------------|-------------|---------------|
| [Tractus-X Connector](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/charts/tractusx-connector)       | 0.8.0       | 0.8.0         |
| [Digital Twin Registry](https://github.com/eclipse-tractusx/sldt-digital-twin-registry/tree/main/charts/registry) | 0.5.0       | 0.5.0         |

## Overview of Implemented Standards

The application follows the following Catena-X standards (business-wise) to the following degree:

| Standard                                                                                                                                           | Level of implementation                   |
|----------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| [CX-0118 Delivery Information Exchange 2.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0118-ActualDeliveryInformationExchange)          | Compliant.                                |
| [CX-0120 Short-Term Material Demand Exchange 2.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0120-ShortTermMaterialDemandExchange)      | Compliant.                                | 
| [CX-0121 Planned Production Output Exchange 1.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0121-PlannedProductionOutputExchange)       | Compliant.                                |
| [CX-0122 Item Stock Exchange 2.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0122-ItemStockExchange)                                    | Compliant.                                |                                                                                         
| [CX-0145 Days of Supply Exchange 1.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0145-DaysofsupplyExchange)                             | Missing EDC and Frontend integration.     |                                                                           
| [CX-0146 Supply Chain Disruption Notifications 1.0.0](https://catenax-ev.github.io/docs/next/standards/CX-0146-SupplyChainDisruptionNotifications) | Missing functionality to react and close. |                                                             

## Known Knows

### Data Sovereignty

Currently, edc assets are always configured to match exactly one kind policy. These policies can be defined during
deployment (see [Admin Guide](docs/admin/Admin_Guide.md)). Data is offered to each partner, who has been added to
the PURIS FOSS's master data pool depending on the business relationship (partner is a customer / supplier).

For productive use, the following features should be implemented:

- configuration of contracts including accepting and refusing contracts via UI
- more user-friendly configuration of contracts including bi-lateral contracts

## License

The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.

## Notice for Docker Image

Below you can find the information regarding Docker Notice for this frontend.

- [Frontend](./frontend/DOCKER_NOTICE.md)
- [Backend](./backend/DOCKER_NOTICE.md)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
