/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
import { ThemeProvider } from '@mui/material/styles';
import { Outlet } from 'react-router-dom';
import { Footer } from './Footer';
import { Box, Stack } from '@mui/material';
import theme from '../../theme';
import SideBar from './SideBar';

export const Layout = () => {
    return (
        <ThemeProvider theme={theme}>
            <Box display="flex" height="100vh" width="100%" sx={{ backgroundColor: '#f5f5f7' }}>
                <SideBar />
                <Stack
                    padding="1.25rem 1rem"
                    overflow="auto"
                    flexGrow={1}
                    spacing={5}
                    fontFamily={theme.typography.fontFamily}
                    fontSize={theme.typography.body1.fontSize}
                >
                    <Box flexGrow={1}>
                        <Outlet />
                    </Box>
                    <Footer />
                </Stack>
            </Box>
        </ThemeProvider>
    );
};
