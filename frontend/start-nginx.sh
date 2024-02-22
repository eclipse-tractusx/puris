#
# Copyright (c) 2022,2024 Volkswagen AG
# Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

#!/usr/bin/env sh
echo "Starting NGINX"
export EXISTING_VARS=$(printenv | awk -F= '{print $1}' | sed 's/^/\$/g' | paste -sd,);

echo "==============="
echo "ENVIRONMENT VARIABLES:"
printenv
echo "==============="

for file in $JSFOLDER;
do
  echo -e "\r\n"
  echo $file

  # other command cuts off length of files. sponge has no file-size limitation.
  #cat $file | envsubst $EXISTING_VARS | tee $file
  #envsubst $EXISTING_VARS < $file | tee $file
  envsubst $EXISTING_VARS < $file | sponge $file
  echo -e "\r\n"
done

envsubst $EXISTING_VARS < /etc/nginx/nginx.conf | sponge /etc/nginx/nginx.conf

nginx -g 'daemon off;'
