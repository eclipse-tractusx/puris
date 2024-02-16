# Interface Documentation

This document provides information on the interfaces. It more or less links you to the relevant sections in other
documentations.

## Overview of Data Exchange Interfaces

The [arc42 documentation](../arc42/Index.md) provides an overview based on the following chapters:

- [Building Block View](../arc42/05_building_block_view.md) shows which kind of interfaces are present
- [Runtime View](../arc42/06_runtime_view.md) shows how data with participants is exchanged (implementation of
  standardization candidate)

## Swagger documentation

You can refer to the INT environment application to view the swagger ui.

```
http://puris-customer.int.demo.catena-x.net/swagger-ui/index.html
```

If not reachable, you can also deploy the backend application according to the [Install.md](../../backend/INSTALL.md) in
the backend and use the following path.

```
http://localhost:8081/catena/swagger-ui/index.html
```

_Note: The port and the path depend on the configuration of the spring backend (`port` and `context path`)._

## Postman

There is a [postman collection](../../local/postman/README.md) containing
information on how to provide master data and some basic data to test the application.
