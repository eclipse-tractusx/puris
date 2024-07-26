#
# Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

import os

# Search above /scripts (root)
directory = "../."
license_header = """Contributors to the Eclipse Foundation"""

# don't check SE life cycle folders
blacklisted_folders = [
    "node_modules",
     ".git",
     "build",
     "target",
     "dist",
     ".idea",
     ".mvn"
]

# exclude non_code artifacts
excluded_extensions = [
    ".png",
    ".jpg",
    ".gif",
    ".pdf",
    ".md",
    ".svg",
    ".puml",
    ".properties",
    ".keys",
    ".secret",
    ".key",
    ".iml",
    ".txt",
    ".json",
    ".ico",
    ".jsonld",
    ".tgz",
    ".cert",
    ".pem",
    ".conf"
]

# exclude specific files by name patterns
excluded_file_patterns  = [
    "README",
    "LICENSE",
    "config",
    ".gitignore",
    ".helmignore",
    ".prettierrc",
    ".env",
    "mvnw",
    "DEPENDENCIES"
]

def file_contains_license(file_path, license_header):
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
        content = file.read()
        return license_header in content

def find_files_without_license(directory, license_header, blacklisted_folders):
    files_without_license = []
    for root, _, files in os.walk(directory):
        if any(blacklisted_folder in root for blacklisted_folder in blacklisted_folders):
            continue
        for file in files:
            file_path = os.path.join(root, file)

            # Skip files with excluded extensions
            if any(file.endswith(ext) for ext in excluded_extensions):
                print(f"Skipping excluded file: {file_path}")
                continue

             # Skip specific excluded files by start pattern
            if any(file.startswith(pattern) for pattern in excluded_file_patterns):
                print(f"Skipping specific excluded file by pattern: {file_path}")
                continue

            if not file_contains_license(file_path, license_header):
                files_without_license.append(file_path)
    return files_without_license

# Run the script
files_without_license = find_files_without_license(directory, license_header, blacklisted_folders)

# Print the list of files without the license header
if files_without_license:
    print("Files without the license header:")
    for file in files_without_license:
        print(file)
else:
    print("All files contain the license header.")
