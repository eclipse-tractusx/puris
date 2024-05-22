# Interface Documentation

This document provides information on the interfaces. It more or less links you to the relevant sections in other
documentations.

## Overview of Data Exchange Interfaces

The [arc42 documentation](../architecture/Index.md) provides an overview based on the following chapters:

- [Building Block View](../architecture/05_building_block_view.md) shows which kind of interfaces are present
- [Runtime View](../architecture/06_runtime_view.md) shows how data with participants is exchanged (implementation of
  standardization candidate)

## Swagger documentation

Please refer to the open API specification provided in `docs/api/openAPI.yaml`.

To have a running swagger ui, feel free to check out the INT environment.

```
http://puris-customer.int.demo.catena-x.net/swagger-ui/index.html
```

To have a running and executable swagger ui, feel free to also deploy the backend application according to the
[Install.md](../../backend/INSTALL.md) in the backend and use the following path.
To authorize requests copy the value of key `CUSTOMER_BACKEND_API_KEY` in `local/.env`.

```
http://localhost:8081/catena/swagger-ui/index.html
```

_Note: The port and the path depend on the configuration of the spring backend (`port` and `context path`)._

## Postman

There is a [postman collection](../../local/postman/README.md) containing
information on how to provide master data and some basic data to test the application.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/puris
