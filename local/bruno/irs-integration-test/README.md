# PURIS & IRS Test Setup

The IRS and PURIS test setup spins up 4 companies as stated in [CASE_DATA.md](./CASE_DATA.md). It's a subset of the [community days setup](../../../etx-community-days/CASE_DATA.md).

The goal of this integration is to setup the four companies with PURIS and IRS data. It's an adaption of the [puris-integration-test](../puris-integration-test/) replacing the `CUSTOMER` and `SUPPLIER` refernces by `OWN` and `PARNTER` variables so that one can run it with environments specific to the respective role (`{OWN_ACTING_COMPANY}-{supplies/buys}-{product/material}-{PARTNER_ACTING_COMPANY}).

> [!warning]
> Open: Don't let customer use the "don't register DTR feature" to ease setup.

To ease the PURIS FOSS Setup it makes sense to start with the setup of the suppliers. For seeding just do the folllowing:

> [!Tip]
> Requires jq and vault cli.
>

Prepare a config like `config/example.cfg` with your vault address, token and kv engine.

```bash
# get .env for bruno
./prepare-bruno.sh -e example.cfg

# load data into puris, setup irs
./run-bruno.sh
```

Potential improvement: Switch to file upload for operational data

Definition of Setups

1. Setup Supplier (01-PURIS/B-Supplier)
   1. 01-MAD
   2. 02-Provide Operational Data to Customer
2. Setup IRS (02-IRS)
   1. 01-Cleanup Policy Store"
   2. 02-Setup IRS Contract"
   3. 03-Setup Global Policies"
   4. 04-Setup self contract definitions (24.05)"
3. Setup Customer (01-PURIS/A-Customer)
   1. 01-MAD
   2. 02-Provide Operational Data to Supplier

Environments

1. ceres-supplies-sensor-to-bristol.bru (Setup Supplier) x
2. ceres-supplies-sensor-to-semi.bru (Setup Supplier) x
3. bristol-buys-sensor-from-ceres.bru (Setup Customer) x
4. bristol-supplies-ecu-to-ccu.bru (Setup Supplier) x
5. semi-buys-sensor-from-ceres.bru (Setup Customer) x
6. semi-supplies-gearbox-to-ccu.bru (Setup Supplier) x
7. semi-supplies-tire-a-to-ccu.bru (Setup Supplier) x
8. ccu-buys-ecu-from-bristol.bru (Setup Customer) x
9. ccu-buys-gearbox-from-semi.bru (Setup Customer) x
10. ccu-buys-tire-from-semi.bru (Setup Customer) x
11. ccu-supplies-vehicle-to-end-customer.bru (Setup Supplier) x
