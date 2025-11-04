# Project Installation

Please refer to the following instructions and tips to run the puris application. Per TRG we recommend the installation
via helm.

## Running using Helm (deployment)

First configure the application to your needs. you can use the [default values.yaml](./charts/puris/values.yaml) as a
basis. Please find below the most relevant configurations:

- **Ingress** (if you want to enable ingress) for frontend/backend, under `frontend.ingress.` and `backend.ingress.`
- **EDC**, under `backend.puris.edc`
- **DTR** incl. keycloak under `backend.puris.dtr`
- **Own master data**, under `backend.puris.own`
- **Role demonstration setup**, under `backend.puris.demonstrator.role`
- **Postgresql settings**, under `backend.puris.datasource` (only necessary, if `postgres.enabled` is false -
  else autoconfigured).
- **Frontend Keycloak** is disabled by default but can be configured under `frontend.puris.keycloak`.

> **NOTE**
>
> Further information on the individual properties and the installation commands can be found in the following
> [README.md](./charts/puris/README.md).

**Attention**: When using `postgres.enabled` = false and bringing your own database, ensure to set
`backend.puris.jpa.hibernate.ddl-auto` = `validate` to prevent **DATA LOSS**.

## Running for development using mvn/npm or using docker

See the `INSTALL.md` files in the [backend](./backend/INSTALL.md) and [frontend](./frontend/INSTALL.md) folder.

## Running local integration testing user docker compose

See `Install.md` file in folder [local](./local/docker-compose.yaml) for integration testing environment with two
deployed applications and EDCs.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
