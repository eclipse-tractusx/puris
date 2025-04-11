#!/bin/sh

#
# Copyright (c) 2025 Volkswagen AG
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

open=0
browser="chrome"
valid_browsers="chrome webkit firefox edge electron"

while [ "$1" != "" ]; do
  case $1 in
    -o)
      open=1
      ;;
    --browser)
      shift
      browser=$1
      if ! echo "$valid_browsers" | grep -wq "$browser"; then
        echo "Invalid browser: $browser"
        echo "Valid options are: chrome, webkit, firefox, edge, electron."
        exit 1
      fi
      ;;
    -h)
      echo "If no option is provided, the tests will be run headless."
      echo "You can use options to alter behavior:"
      echo "-o: opens the cypress test runner in a browser window."
      echo "--browser <browser>: specifies the browser to use (chrome, webkit, firefox, edge, electron)."
      echo "\nExiting..."
      exit 1
      ;;
  esac
  shift
done

if [ $open -eq 1 ]; then
  echo "Starting cypress test runner"
  docker compose -f ./testing/docker-compose-e2e-open.yaml up
  docker compose -f ./testing/docker-compose-e2e-open.yaml down -v
else
  echo "executing e2e tests..."
  export BROWSER=$browser
  echo "Using browser: $BROWSER"
  docker compose -f ./testing/docker-compose-e2e.yaml up
  docker compose -f ./testing/docker-compose-e2e.yaml down -v
fi
