/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/

const { defineConfig } = require("cypress");
const fs = require('fs');

module.exports = defineConfig({
    e2e: {
        setupNodeEvents(on, config) {
          // delete videos for successful tests
          on('after:spec', (spec, results) => {
            if (results && results.video) {
              const failures = results.tests.some((test) =>
                test.attempts.some((attempt) => attempt.state === 'failed')
              )
              if (!failures) {
                fs.unlinkSync(results.video)
              }
            }
          })
        },
        baseUrl: 'http://localhost:3000',
        viewportWidth: 1280,
        viewportHeight: 720,
        experimentalWebKitSupport: true
    },
    env: {
      supplierUrl: 'http://localhost:3001'
    },
    video: true,
});
