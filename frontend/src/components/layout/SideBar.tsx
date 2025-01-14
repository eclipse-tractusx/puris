/*
Copyright (c) 2022 Volkswagen AG
Copyright (c) 2022 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022 Contributors to the Eclipse Foundation

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

import * as React from 'react';
import { styled, useTheme, Theme, CSSObject } from '@mui/material/styles';
import Box from '@mui/material/Box';
import MuiDrawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import AuthenticationService from '@services/authentication-service';
import { useAuth } from '@hooks/useAuth';
import { OverridableComponent } from '@mui/material/OverridableComponent';
import { SvgIconTypeMap, Typography } from '@mui/material';
import { Role } from '@models/types/auth/role';
import {
    AutoStoriesOutlined,
    ChevronLeftOutlined,
    HandshakeOutlined,
    HelpOutlineOutlined,
    HomeOutlined,
    Inventory2Outlined,
    LogoutOutlined,
    MenuOutlined,
    NotificationsOutlined,
    SyncAltOutlined,
} from '@mui/icons-material';
import { Link, useLocation } from 'react-router-dom';

const openedMixin = (theme: Theme): CSSObject => ({
    width: theme.sidebarWidth,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
    }),
    overflowX: 'hidden',
    position: 'relative',
});

const closedMixin = (theme: Theme): CSSObject => ({
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    overflowX: 'hidden',
    boxSizing: 'border-box',
    width: `calc(${theme.spacing(7)} + 1px)`,
    position: 'relative',

    [theme.breakpoints.up('sm')]: {
        width: `calc(${theme.spacing(8)} + 1px)`,
    },
});

const DrawerHeader = styled('div')(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    minHeight: '0 !important',
    paddingLeft: theme.spacing(2),
    paddingRight: theme.spacing(2),
    ...theme.mixins.toolbar,
}));

const Drawer = styled(MuiDrawer, { shouldForwardProp: (prop) => prop !== 'open' })(({ theme, open }) => ({
    width: theme.sidebarWidth,
    flexShrink: 0,
    whiteSpace: 'nowrap',
    boxSizing: 'border-box',
    ...(open && {
        ...openedMixin(theme),
        '& .MuiDrawer-paper': openedMixin(theme),
    }),
    ...(!open && {
        ...closedMixin(theme),
        '& .MuiDrawer-paper': closedMixin(theme),
    }),
}));

type SideBarItemProps = (
    | {
          variant?: 'link';
          path: string;
      }
    | {
          variant: 'button';
          action?: () => void;
      }
) & {
    name: string;
    icon: React.ReactElement<OverridableComponent<SvgIconTypeMap<object, 'svg'>>>;
    requiredRoles?: Role[];
};

const sideBarItems: SideBarItemProps[] = [
    { name: 'Materials', icon: <HomeOutlined />, path: '/materials' },
    { name: 'Dashboard', icon: <HomeOutlined />, path: '/dashboard' },
    { name: 'Notifications', icon: <NotificationsOutlined />, path: '/notifications' },
    { name: 'Stocks', icon: <Inventory2Outlined />, path: '/stocks' },
    { name: 'Catalog', icon: <AutoStoriesOutlined />, path: '/catalog', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Negotiations', icon: <HandshakeOutlined />, path: '/negotiations', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Transfers', icon: <SyncAltOutlined />, path: '/transfers', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'User Guide', icon: <HelpOutlineOutlined />, path: '/user-guide' },
    { name: 'Logout', icon: <LogoutOutlined />, action: AuthenticationService.logout, variant: 'button' },
];

export default function MiniDrawer() {
    const [open, setOpen] = React.useState(() => true);
    const { pathname } = useLocation();
    const theme = useTheme();
    const { hasRole } = useAuth();
    const handleDrawerOpen = () => {
        setOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
    };

    return (
        <Drawer variant="permanent" open={open}>
            <DrawerHeader>
                {open ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', width: '100%', gap: '.5rem' }}>
                        <img height="30px" src="/puris-logo.svg" alt="Puris icon"></img>

                        <IconButton sx={{ p: 0, borderRadius: 0 }} onClick={handleDrawerClose}>
                            <ChevronLeftOutlined />
                        </IconButton>
                    </Box>
                ) : (
                    <IconButton sx={{ p: 0, borderRadius: 0, mx: 'auto' }} onClick={handleDrawerOpen}>
                        <MenuOutlined />
                    </IconButton>
                )}
            </DrawerHeader>
            <List>
                {sideBarItems.map((item) => {
                    if (item.requiredRoles && !hasRole(item.requiredRoles)) return null;

                    return (
                        <ListItem key={item.name} disablePadding sx={{ display: 'block', px: 1, py: 0.5 }}>
                            <ListItemButton
                                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                                LinkComponent={({ href, ...props }) => <Link to={href} {...props} />}
                                sx={{
                                    gap: open ? 1 : 0,
                                    justifyContent: open ? 'initial' : 'center',
                                    alignItems: 'center',
                                    px: 2.5,
                                    py: 0.5,
                                    borderRadius: 2,
                                    ':hover': {
                                        backgroundColor: theme.palette.primary.main,
                                        color: theme.palette.primary.contrastText,
                                    },
                                    ...(item.variant !== 'button' && pathname.startsWith(item.path)
                                        ? {
                                              backgroundColor: theme.palette.primary.dark,
                                              color: theme.palette.primary.contrastText,
                                          }
                                        : {}),
                                }}
                                onClick={item.variant === 'button' ? item.action : undefined}
                                {...('path' in item ? { href: item.path } : {})}
                            >
                                <ListItemIcon
                                    sx={{
                                        minWidth: 0,
                                        justifyContent: 'center',
                                        mx: open ? 0 : 'auto',
                                    }}
                                >
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText primary={item.name} sx={{ opacity: open ? 1 : 0 }} />
                            </ListItemButton>
                        </ListItem>
                    );
                })}
            </List>
            <List sx={{ marginTop: 'auto', p: 0 }}>
                <ListItem disablePadding sx={{ display: 'block' }}>
                    <ListItemButton
                        LinkComponent={({ href, ...props }) => <Link to={href} {...props} />}
                        sx={{
                            justifyContent: 'center',
                            textAlign: 'center',
                            px: open ? 2.5 : 1,
                        }}
                        href="/aboutLicense"
                    >
                        <Typography variant={open ? 'body1' : 'body3'}>{open ? 'About License' : 'License'}</Typography>
                    </ListItemButton>
                </ListItem>
            </List>
        </Drawer>
    );
}
