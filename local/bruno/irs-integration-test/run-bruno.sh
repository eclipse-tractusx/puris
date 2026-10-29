#!/usr/bin/env bash
#
# Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

# Seed PURIS & IRS via IRS bruno collection, from end of chain to Atlas.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"
BRUNO_DIR="$(cd "${SCRIPT_DIR}/bruno" && pwd)"
echo "Running in $SCRIPT_DIR, switching to bruno dir $BRUNO_DIR"
cd $BRUNO_DIR || exit 1

# Setup folders required for supplier role
# Note: Supplier setup needs to be run from right to left first
SUP1="01-PURIS/B-SUPPLIER/01-MAD"
SUP2="01-PURIS/B-SUPPLIER/02-Provide Operational Data to Customer"
# Setup folders required for IRS setup
# Note: With IRS supporting Saturn (policy profile 25.09), we need to switch
IRS1="02-IRS/01-SETUP-2405/01-Cleanup Policy Store"
IRS2="02-IRS/01-SETUP-2405/02-Setup IRS Contract"
IRS3="02-IRS/01-SETUP-2405/03-Setup Global Policies"
IRS4="02-IRS/01-SETUP-2405/04-Setup self contract definitions (24.05)"
# Setup folders required for customer role
# Note: Customer setup needs to be run from right to left after the supplier setup
#       That way the BoM can be setup.
CUS1="01-PURIS/A-CUSTOMER/01-MAD"
CUS2="01-PURIS/A-CUSTOMER/02-Provide Operational Data to Supplier"

# company | role | env
PLAN=(
  "CERES   supplier ceres-supplies-sensor-to-semi"
  "CERES   supplier ceres-supplies-sensor-to-bristol"
  "CERES   irs      ceres-supplies-sensor-to-bristol"

  "BRISTOL supplier bristol-supplies-ecu-to-atlas"
  "BRISTOL irs      bristol-supplies-ecu-to-atlas"
  "BRISTOL customer bristol-buys-sensor-from-ceres"

  "SEMI supplier semi-supplies-tire-a-to-atlas"
  "SEMI supplier semi-supplies-gearbox-to-atlas"
  "SEMI irs      semi-supplies-tire-a-to-atlas"
  "SEMI customer semi-buys-sensor-from-ceres"

  "CCU   supplier ccu-supplies-vehicle-to-end-customer"
  "CCU   irs      ccu-supplies-vehicle-to-end-customer"
  "CCU   customer ccu-buys-ecu-from-bristol"
  "CCU   customer ccu-buys-gearbox-from-semi"
  "CCU   customer ccu-buys-tire-from-semi"
)

SUMMARY=()
for line in "${PLAN[@]}"; do
  read -r company role env <<<"$line"
  if [ "$role" = supplier ]; then
    folders=("$SUP1" "$SUP2")
  elif [ "$role" = irs ]; then
    folders=("$IRS1" "$IRS2" "$IRS3" "$IRS4")
  else
    folders=("$CUS1" "$CUS2")
  fi
  for f in "${folders[@]}"; do
    echo
    echo "################################################################"
    echo "### $company  ($role)  env=$env"
    echo "### folder: $f"
    echo "################################################################"
    if bru run "$f" -r --env "$env"; then r=OK; else r=FAIL; fi
    echo ">>> RESULT: $r  [$company / $env / $(basename "$f")]"
    SUMMARY+=("$r  $company  $env  $(basename "$f")")
  done
done

echo
echo "=========================== SUMMARY ==========================="
printf '%s\n' "${SUMMARY[@]}"
echo "=========================== DONE =============================="
