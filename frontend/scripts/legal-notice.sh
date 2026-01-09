#!/bin/bash

#
# Copyright (c) 2024 Contributors to the Eclipse Foundation
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

# Define placeholders from legal-notice.json
name_anchor='NAME_PLACEHOLDER'
version_anchor='VERSION_PLACEHOLDER'
license_anchor='LICENSE_PLACEHOLDER'
commit_id_anchor='COMMIT_ID_PLACEHOLDER'
server_url_anchor='SERVER_URL_PLACEHOLDER'
repository_anchor='REPOSITORY_PLACEHOLDER'
ref_anchor='REF_PLACEHOLDER'

# Read values from package.json using jq
name=$(jq -r '.name' package.json)
license=$(jq -r '.license' package.json)
version=$(jq -r '.version' package.json)

# Get commit id
commit_id=$(git rev-parse HEAD)

# Get tag version
tag_version=$(git tag --points-at HEAD | head -n 1)

# Get GitHub context from environment variables
server_url=$SERVER_URL
repository=$REPOSITORY
ref=$REF_NAME

# Read legal-notice.json as reference
legal_notice_reference=$(cat src/assets/aboutPage.json)

# Replace the tag version with the package version if unavailable
current_version=${tag_version:-${version}-untagged}

# Function to check if placeholder substitution was successful
check_substitution() {
  if [[ $1 = *"$2"* ]]; then
    echo "Error: Failed to replace $2"
    exit 1
  fi
}

# Replace placeholders with actual values
legal_notice_name="${legal_notice_reference/$name_anchor/$name}"
check_substitution "$legal_notice_name" "$name_anchor"
echo "Replaced name"

legal_notice_version="${legal_notice_name/$version_anchor/$current_version}"
check_substitution "$legal_notice_version" "$version_anchor"
echo "Replaced version"

legal_notice_license="${legal_notice_version/$license_anchor/$license}"
check_substitution "$legal_notice_license" "$license_anchor"
echo "Replaced license"

legal_notice_commit="${legal_notice_license/$commit_id_anchor/$commit_id}"
check_substitution "$legal_notice_commit" "$commit_id_anchor"
echo "Replaced commit ID"

legal_notice_server_url="${legal_notice_commit//$server_url_anchor/$server_url}"
check_substitution "$legal_notice_server_url" "$server_url_anchor"
echo "Replaced server URL"

legal_notice_repository="${legal_notice_server_url//$repository_anchor/$repository}"
check_substitution "$legal_notice_repository" "$repository_anchor"
echo "Replaced repository"

legal_notice_ref="${legal_notice_repository//$ref_anchor/$ref}"
check_substitution "$legal_notice_ref" "$ref_anchor"
echo "Replaced ref name"

# Write the final result to legal-notice.json
echo "$legal_notice_ref" > src/assets/aboutPage.json
