<div align="center">
  <h2 align="center">PURIS</h2>
  The Catena-X Predictive Unit Real-Time Information Service (PURIS) for Short Term Demand and Capacity Management
</div>

## Overview

The project is made of a backend and a frontend. Look into the respective folders and their documentation to get
information about prerequirements and getting started guides.

## Known Knows

### Data Sovereignty

Currently, edc assets are always configured to match exactly one kind policy. These policies can be defined during
deployment (see [Admin Guide](./docs/adminGuide/Admin_Guide.md)). Data is offered to each partner, who has been added to
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
