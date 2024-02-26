#!/usr/bin/env sh

#
# Copyright (c) 2023,2024 Volkswagen AG
# Copyright (c) 2023,2024 Contributors to the Eclipse Foundation
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

echo "Starting NGINX"
export EXISTING_VARS=$(printenv | awk -F= '{print $1}' | sed 's/^/\$/g' | paste -sd,);

echo "==============="
echo "ENVIRONMENT VARIABLES:"
printenv
echo "==============="

for file in $JSFOLDER;
do
  echo "Substitute environment variables in $file"

  tmp_file="$file.tmp"
  echo "envsubstitute $file to $tmp_file"
  envsubst $EXISTING_VARS < $file > $tmp_file

  echo "replace file ($file) by tmp file ($tmp_file)"
  rm $file
  mv $tmp_file $file

  echo -e "\r\n"
done

nginx_conf="/etc/nginx/nginx.conf"
tmp_nginx_conf="$nginx_conf.tmp"

echo "envsubstitute $nginx_conf to $tmp_file"
envsubst $EXISTING_VARS < $nginx_conf > $tmp_nginx_conf

echo "replace config ($nginx_conf) by tmp file ($tmp_nginx_conf)"
rm $nginx_conf
mv $tmp_nginx_conf $nginx_conf

nginx -g 'daemon off;'
