# Solution Strategy

**Organization**

PURIS FOSS

- follows the related standardization candidates or even published standards (CX-0112).
- is developed parallel to the consortial SAFe project.

**Up-to-dateness / real-time**

- Stock information has always the latest amount. E.g. at 6 a.m. there is a stock of 60 parts of material and at 8 a.m.
  there is a stock of 80 parts of material.
- Demand and Production Output are measured "per day" e.g., today's demand and next thursday's demand.

**Interoperable Data Exchange and Pattern**

- Use SAMM aspect models to exchange PURIS data (see domain model).
- Use the EDC to participate in Catena-X.
    - Data Providers can offer their data or data providing API as a _Data Asset_.
    - Data Consumers can consume a Data Provider's _Data Asset_.
- Data is exchanged using an asynchronous pull and push mechanism.

**Management of EDC and Digital Twins**

PURIS FOSS

- creates contract offers for partners itself
- registers digital twins depending on the relationship of partners for a specific material
- implements value-only submodel interfaces for information objects exchanged based on Digital Twin and Industry Core
  standards.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
