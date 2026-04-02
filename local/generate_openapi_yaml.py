#
# Copyright (c) 2025 Volkswagen AG
# Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
# Copyright (c) 2025 Contributors to the Eclipse Foundation
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

import json
import yaml
import requests
import xml.etree.ElementTree as ET
 
def set_max_items_for_arrays(schema):
    if isinstance(schema, dict):
        if 'type' in schema and schema['type'] in ['array', 'string']:
            schema.setdefault('maxItems', 50)
        for key, value in schema.items():
            set_max_items_for_arrays(value)
    elif isinstance(schema, list):
        for item in schema:
            set_max_items_for_arrays(item)
 
def set_min_items_for_arrays(schema):
    if isinstance(schema, dict):
        if 'type' in schema and schema['type'] in ['number']:
            schema.setdefault('minimum', 0)
        for key, value in schema.items():
            set_max_items_for_arrays(value)
    elif isinstance(schema, list):
        for item in schema:
            set_max_items_for_arrays(item)
 
 
def restrict_additional_properties(schema):
    if isinstance(schema, dict):
        # Set additionalProperties to false for any object that has properties
        if 'properties' in schema and 'type' in schema and schema['type'] == 'object':
            schema.setdefault('additionalProperties', False)
        # Set additionalProperties to false for any schema
        if 'schema' in schema:
            schema['schema'].setdefault('additionalProperties', False)
        for key, value in schema.items():
            restrict_additional_properties(value)
    elif isinstance(schema, list):
        for item in schema:
            restrict_additional_properties(item)


def remove_not_implemented_endpoints(openapi_data):
    if 'paths' not in openapi_data:
        return
    
    paths_to_remove = []
    
    for path, path_data in openapi_data['paths'].items():
        if not isinstance(path_data, dict):
            continue
        methods_to_remove = []
        # Check each HTTP method for this path
        for method, method_data in path_data.items():
            if not isinstance(method_data, dict) or 'responses' not in method_data:
                continue
            # Check if this method has a 501 response with "Not Implemented"
            responses = method_data['responses']
            if '501' in responses:
                response_501 = responses['501']
                if isinstance(response_501, dict) and 'description' in response_501:
                    if response_501['description'] == "Not Implemented":
                        methods_to_remove.append(method)
        # Remove the identified methods
        for method in methods_to_remove:
            del path_data[method]
        # If no methods remain for this path, mark the path for removal
        # Only consider actual HTTP methods (not parameters, summary, etc.)
        http_methods = {'get', 'post', 'put', 'delete', 'patch', 'head', 'options'}
        remaining_methods = [key for key in path_data.keys() if key.lower() in http_methods]
        if not remaining_methods:
            paths_to_remove.append(path)
    # Remove empty paths
    for path in paths_to_remove:
        del openapi_data['paths'][path]
# Download JSON data from the API
response = requests.get('http://localhost:8081/catena/v3/api-docs')
response.raise_for_status()  # Raise an exception for bad status codes
json_data = response.json()

# Remove endpoints with 501 "Not Implemented" responses
remove_not_implemented_endpoints(json_data)

# Modify JSON to set maxItems to 3 for all arrays
set_max_items_for_arrays(json_data)

set_min_items_for_arrays(json_data)
 
# Modify JSON to restrict additionalProperties for objects
restrict_additional_properties(json_data)

# read app version from pom.xml
tree = ET.parse('../backend/pom.xml')
root = tree.getroot()
# Define the Maven namespace
namespace = {'maven': 'http://maven.apache.org/POM/4.0.0'}
# Find the version element (project/version)
version_element = root.find('maven:version', namespace)
app_version = version_element.text if version_element is not None else '0.0.0'

# Update the version in the OpenAPI info section
if 'info' not in json_data:
    json_data['info'] = {}
json_data['info']['version'] = app_version

# Convert JSON to YAML
yaml_data = yaml.dump(json_data, allow_unicode=True)
 
# Save YAML data
with open('../docs/api/openAPI.yaml', 'w') as yaml_file:
    yaml_file.write(yaml_data)