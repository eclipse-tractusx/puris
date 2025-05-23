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
import { Button, Stack, SvgIconTypeMap, Typography } from '@mui/material';
import { Role } from '@models/types/auth/role';
import {
    AutoStoriesOutlined,
    ChevronLeftOutlined,
    ContentCopyOutlined,
    HandshakeOutlined,
    HelpOutlineOutlined,
    HomeOutlined,
    InfoOutlined,
    LogoutOutlined,
    MenuOutlined,
    NotificationsOutlined,
    SyncAltOutlined,
} from '@mui/icons-material';
import { visuallyHidden } from '@mui/utils';
import { Link, useLocation } from 'react-router-dom';
import { useOwnPartner } from '@hooks/useOwnPartner';
import { useNotifications } from '@contexts/notificationContext';

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
    { name: 'Notifications', icon: <NotificationsOutlined />, path: '/notifications' },
    { name: 'Catalog', icon: <AutoStoriesOutlined />, path: '/catalog', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Negotiations', icon: <HandshakeOutlined />, path: '/negotiations', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'Transfers', icon: <SyncAltOutlined />, path: '/transfers', requiredRoles: ['PURIS_ADMIN'] },
    { name: 'User Guide', icon: <HelpOutlineOutlined />, path: '/user-guide' },
    { name: 'About License', icon: <InfoOutlined/>, path: '/about-license'},
    { name: 'Logout', icon: <LogoutOutlined />, action: AuthenticationService.logout, variant: 'button' },
];

export default function MiniDrawer() {
    const [open, setOpen] = React.useState(() => true);
    const { pathname } = useLocation();
    const theme = useTheme();
    const { hasRole } = useAuth();
    const { ownPartner } = useOwnPartner();
    const { notify } = useNotifications();
    const handleDrawerOpen = () => {
        setOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
    };

    const handleCopyBpnl = async () => {
        await navigator.clipboard.writeText(ownPartner?.bpnl ?? '');
        notify({
            title: 'Copied to Clipboard',
            description: 'Your company BPNL was copied to the clipboard',
            severity: 'success'
        });
    };

    return (
        <Drawer variant="permanent" open={open} data-testid="sidebar">
            <DrawerHeader>
                {open ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', width: '100%', gap: '.5rem' }}>
                        <img height="30px" src="/puris-logo.svg" alt="Puris icon"></img>

                        <IconButton sx={{ p: 0, borderRadius: 0 }} onClick={handleDrawerClose}>
                            <ChevronLeftOutlined />
                            <Typography variant="body1" sx={visuallyHidden}>Collapse Sidebar</Typography>
                        </IconButton>
                    </Box>
                ) : (
                    <IconButton sx={{ p: 0, borderRadius: 0, mx: 'auto' }} onClick={handleDrawerOpen}>
                        <MenuOutlined />
                        <Typography variant="body1" sx={visuallyHidden}>Expand Sidebar</Typography>
                    </IconButton>
                )}
            </DrawerHeader>
            <List>
                {sideBarItems.map((item) => {
                    if (item.requiredRoles && !hasRole(item.requiredRoles)) return null;

                    return (
                        <ListItem
                            key={item.name}
                            disablePadding
                            sx={{ display: 'block', px: 1, py: 0.5 }}
                            data-testid={`sidebar-menu-item-${item.name.replace(' ', '').toLowerCase()}`}
                            aria-selected={item.variant !== 'button' && pathname.startsWith(item.path)}
                        >
                            <ListItemButton
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
            {open ? <Stack gap="0.25rem" paddingInline=".5rem" paddingBlock="1rem" marginTop="auto" data-testid="sidebar-item-license">
                <Typography
                    variant="body2"
                    component="h3"
                    fontWeight="600"
                    sx={{ maxWidth: '12rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                >
                    {ownPartner?.name}
                </Typography>
                <Button variant="text" sx={{ padding: 0, justifyContent: 'start', width: 'fit-content'}} onClick={handleCopyBpnl}>
                    <Typography variant="body3">{ownPartner?.bpnl} <ContentCopyOutlined /></Typography>
                </Button>
            </Stack> : null}
        </Drawer>
    );
}
