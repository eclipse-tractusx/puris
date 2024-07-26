/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  assetsInclude: ['**/*.md'],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, './src'),
      "@assets": path.resolve(__dirname, './src/assets'),
      "@components": path.resolve(__dirname, './src/components'),
      "@contexts": path.resolve(__dirname, './src/contexts'),
      "@features": path.resolve(__dirname, './src/features'),
      "@hooks": path.resolve(__dirname, './src/hooks'),
      "@models": path.resolve(__dirname, './src/models'),
      "@services": path.resolve(__dirname, './src/services'),
      "@util": path.resolve(__dirname, './src/util'),
      "@views": path.resolve(__dirname, './src/views')
    },
  },
})
