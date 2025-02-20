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

import { createTheme } from '@mui/material/styles';

declare module '@mui/material/styles' {
    interface Theme {
        sidebarWidth: number;
    }
    interface ThemeOptions {
        sidebarWidth?: number;
    }
}

const theme = createTheme({
    palette: {
        primary: {
            dark: '#081F4B',
            main: '#264580',
            light: '#365186',
            contrastText: '#fff',
            shadow: 'rgba(0,0,0,0.1) 0px 1px 3px 0px'
        },
        secondary: {
            main: '#0085FF',
            contrastText: '#fff',
        },
        background: {
            default: '#F5F5F7',
            paper: '#fff',
        },
        warning: {
            main: '#e74444',
            light: '#fdedd6',
        },
    },
    typography: {
        fontFamily: ['Inter', 'sans-serif'].join(','),
        h1: {
            fontSize: '1.625rem',
            fontWeight: 600,
            color: '#081F4B',
        },
        h2: {
            fontSize: '1.5rem',
            fontWeight: 600,
        },
        h3: {
            fontSize: '1.375rem',
            fontWeight: 500,
        },
        h4: {
            fontSize: '1.25rem',
            fontWeight: 500,
        },
        h5: {
            fontSize: '1.125rem',
        },
        h6: {
            fontSize: '1rem',
        },
        body1: {
            fontSize: '1rem',
            lineHeight: 1,
        },
        body2: {
            fontSize: '.875rem',
            lineHeight: 1,
        },
        body3: {
            fontSize: '.75rem',
            lineHeight: 1,
            fontFamily: ['Inter', 'sans-serif'].join(','),
        },
        button: {
            fontSize: '0.8rem',
        },
    },
    components: {
        MuiButton: {
            defaultProps: {
                variant: 'contained',
                color: 'secondary',
            },
            styleOverrides: {
                root: {
                    borderRadius: '2rem',
                    fontSize: '0.75rem',
                    lineHeight: 1,
                },
            },
        },
        MuiIconButton: {
            styleOverrides: {
                root: {
                    color: '#000000',
                },
            },
        },
        MuiSvgIcon: {
            styleOverrides: {
                root: {
                    fontSize: 'inherit',
                },
            },
        },
        MuiListItemIcon: {
            styleOverrides: {
                root: {
                    color: 'inherit',
                },
            },
        },
    },
});

export default theme;
