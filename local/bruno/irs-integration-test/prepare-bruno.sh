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

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ----------------------------------------------------------------------
# Companies
# ----------------------------------------------------------------------

declare COMPANY=(
  "customer"
  "supplier"
  "bristol"
  "ceres"
)

# ----------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------

ensure_https() {
  local url=$1
  if [[ $url =~ ^https?:// ]]; then
    echo "$url"
  else
    echo "https://${url}"
  fi
}

# ----------------------------------------------------------------------
# Vault helpers
# ----------------------------------------------------------------------

load_config() {
  local env=$1
  local cfg="./config/${env}.cfg"
  [[ -f $cfg ]] || { echo -e "${RED}Config file not found: $cfg${NC}"; exit 1; }
  # shellcheck disable=SC1090
  source "$cfg"
  # make sure the vault CLI talks to the configured instance (BAO_ADDR is
  # supported as an alias, e.g. for OpenBao setups)
  export VAULT_TOKEN
  export VAULT_ADDR="${VAULT_ADDR:-${BAO_ADDR}}"
  [[ -n "${VAULT_ADDR}" ]] || { echo -e "${RED}Neither VAULT_ADDR nor BAO_ADDR set in $cfg${NC}"; exit 1; }
  [[ -n "${VAULT_TOKEN}" ]] || { echo -e "${RED}VAULT_TOKEN not set in $cfg${NC}"; exit 1; }
  [[ -n "${KV_NAME}" ]] || { echo -e "${RED}KV_NAME not set in $cfg${NC}"; exit 1; }
  echo -e "${GREEN}Loaded config: $env${NC}"
}

vault_login() {
  echo -e "${BLUE}Logging into Vault at ${VAULT_ADDR}…${NC}"
  LOGIN_OUTPUT=$(vault token lookup 2>&1)
  [[ $? -eq 0 ]] || { echo -e "${RED}Failed to login to Vault${NC}"; echo "$LOGIN_OUTPUT"; exit 1; }
  echo -e "${GREEN}✓ Vault login successful${NC}"
}

get_vault_secret_json() {
  local path=$1
  echo -e "${BLUE}Getting secret: ${KV_NAME}/${path}${NC}" >&2
  local sec
  sec=$(vault kv get -format=json "${KV_NAME}/${path}" 2>/dev/null)
  [[ -n $sec ]] || { echo -e "${RED}Failed to get secret: ${KV_NAME}/${path}${NC}" >&2; exit 1; }
  echo "$sec"
}

check_prerequisites() {
  command -v jq    &>/dev/null || { echo -e "${RED}jq not installed${NC}"; exit 1; }
  command -v vault &>/dev/null || { echo -e "${RED}vault CLI not installed${NC}"; exit 1; }
}

generate_env_file() {
  local env_file="./.env"

  # backup old env file (if any) and prepare appending to a fresh one
  if [[ -f $env_file ]]; then
    local backup_file="${env_file}-$(date +%Y%m%d-%H%M%S)"
    mv "$env_file" "$backup_file"
    echo -e "${BLUE}Backed up existing env file to: $backup_file${NC}"
  fi

  for comp in "${COMPANY[@]}"; do
    local pref
    pref=$(echo "$comp" | tr '[:lower:]' '[:upper:]')
    # local file="${out_dir}/${comp}.bru"

    # ----- comp identity -------------------------------------------------
    local id_json
    id_json=$(get_vault_secret_json "companies/${comp}" | jq -r '.data.data')
    local comp_bpnl comp_bpns comp_bpna
    comp_bpnl=$(echo "$id_json" | jq -r '.bpnl')
    comp_bpns=$(echo "$id_json" | jq -r '.bpns')
    comp_bpna=$(echo "$id_json" | jq -r '.bpna')

    # ----- EDC ------------------------------------------------------
    local edc_json comp_edc_url comp_edc_key
    edc_json=$(get_vault_secret_json "companies/${comp}/edc" | jq -r '.data.data')
    comp_edc_url=$(ensure_https "$(echo "$edc_json" | jq -r '."hostname-internet"')")
    comp_edc_key=$(echo "$edc_json" | jq -r '."api-key"')
    # edc_api_json=$(get_vault_secret_json "companies/${comp}/edc/api" | jq -r '.data.data')

    # ----- PURIS ----------------------------------------------------
    local puris_json comp_puris_url comp_puris_key
    puris_json=$(get_vault_secret_json "companies/${comp}/puris" | jq -r '.data.data')
    comp_puris_url=$(ensure_https "$(echo "$puris_json" | jq -r '."backend-hostname"')")
    comp_puris_key=$(echo "$puris_json" | jq -r '."backend-api-key"')
    # puris_api_json=$(get_vault_secret_json "companies/${comp}/puris/api" | jq -r '.data.data')

    # ----- DTR ------------------------------------------------------
    local dtr_json comp_dtr_url
    dtr_json=$(get_vault_secret_json "companies/${comp}/dtr" | jq -r '.data.data')
    comp_dtr_url=$(ensure_https "$(echo "$dtr_json" | jq -r '."hostname"')")
    # local comp_dtr_idp_json comp_dtr_idp_url comp_dtr_idp_realm
    # comp_dtr_idp_json=$(get_vault_secret_json "env/central-idp" | jq -r '.data.data')
    local comp_dtr_idp_json comp_dtr_idp_client_id comp_dtr_idp_client_secret
    comp_dtr_idp_json=$(get_vault_secret_json "companies/${comp}/dtr/keycloak" | jq -r '.data.data')
    comp_dtr_idp_url=$(ensure_https "$(echo "$comp_dtr_idp_json" | jq -r '."hostname"')")
    comp_dtr_idp_realm=$(echo "$comp_dtr_idp_json" | jq -r '."realm"')
    comp_dtr_idp_client_id=$(echo "$comp_dtr_idp_json" | jq -r '."id"')
    comp_dtr_idp_client_secret=$(echo "$comp_dtr_idp_json" | jq -r '."secret"')

    # ----- IRS ------------------------------------------------------
    local comp_irs_json comp_irs_url
    comp_irs_json=$(get_vault_secret_json "companies/${comp}/irs" | jq -r '.data.data')
    comp_irs_url=$(ensure_https "$(echo "$comp_irs_json" | jq -r '."base-url"')")
    local comp_irs_api_json comp_irs_api_key_regular comp_irs_api_key_admin
    comp_irs_api_json=$(get_vault_secret_json "companies/${comp}/irs/api" | jq -r '.data.data')
    comp_irs_api_key_regular=$(echo "$comp_irs_api_json" | jq -r '."api-key-regular"')
    comp_irs_api_key_admin=$(echo "$comp_irs_api_json" | jq -r '."api-key-admin"')

    # TODO: migrate to PURIS
    # ----- Discovery API ------------------------------------------------------
    # Note: we consider that discovery and so on are the same
    # reuse central idp 
    local comp_discovery_idp_url
    comp_discovery_idp_url=$(ensure_https "$(echo "$central_idp_json" | jq -r '."hostname"')")
    comp_discovery_idp_realm=$(echo "$central_idp_json" | jq -r '."realm"')
    local comp_discovery_idp_json comp_discovery_idp_id comp_discovery_idp_secret
    comp_discovery_idp_json=$(get_vault_secret_json "companies/${comp}/irs/client-secret/discovery" | jq -r '.data.data')
    comp_discovery_idp_id=$(echo "$comp_discovery_idp_json" | jq -r '."id"')
    comp_discovery_idp_secret=$(echo "$comp_discovery_idp_json" | jq -r '."secret"')
    
    # ------------------------------------------------------------------
    # append company information to the env file
    # ------------------------------------------------------------------
    local COMP=$(echo "$comp" | tr '[:lower:]' '[:upper:]')
    echo "${COMP}_BPNL=${comp_bpnl}
${COMP}_BPNS=${comp_bpns}
${COMP}_BPNA=${comp_bpna}
${COMP}_EDC_URL=${comp_edc_url}
${COMP}_EDC_API_KEY=${comp_edc_key}
${COMP}_PURIS_BACKEND_URL=${comp_puris_url}
${COMP}_PURIS_BACKEND_API_KEY=${comp_puris_key}
${COMP}_DTR_URL=${comp_dtr_url}
${COMP}_DTR_IDP_URL=${comp_dtr_idp_url}
${COMP}_DTR_IDP_REALM=${comp_dtr_idp_realm}
${COMP}_DTR_IDP_CLIENT_ID=${comp_dtr_idp_client_id}
${COMP}_DTR_IDP_CLIENT_SECRET=${comp_dtr_idp_client_secret}
${COMP}_DISCOVERY_IDP_URL=${comp_discovery_idp_url}
${COMP}_DISCOVERY_IDP_REALM=${comp_discovery_idp_realm}
${COMP}_DISCOVERY_IDP_CLIENT_ID=${comp_discovery_idp_id}
${COMP}_DISCOVERY_IDP_CLIENT_SECRET=${comp_discovery_idp_secret}
${COMP}_IRS_URL=${comp_irs_url}
${COMP}_IRS_ADMIN_API_KEY=${comp_irs_api_key_admin}
${COMP}_IRS_REGULAR_API_KEY=${comp_irs_api_key_regular}" >> $env_file
    
  done
}

# ----------------------------------------------------------------------
# CLI handling
# ----------------------------------------------------------------------

show_usage() {
  echo "Usage: $0 -e ENV"
  echo "  -e ENV   Load configuration from ./config/ENV.cfg (must set VAULT_ADDR or BAO_ADDR, KV_NAME, VAULT_TOKEN …)"
  echo "  -h       Show this help"
}

main() {
  local env=""

  while getopts "e:h" opt; do
    case $opt in
      e) env=$OPTARG ;;
      h) show_usage; exit 0 ;;
      *) show_usage; exit 1 ;;
    esac
  done

  [[ -z "$env" ]] && { echo -e "${RED}Error: -e ENV is required${NC}"; show_usage; exit 1; }

  check_prerequisites
  load_config "$env"
  vault_login

  generate_env_file

  echo -e "${GREEN}Env file generated${NC}"
}

main "$@"