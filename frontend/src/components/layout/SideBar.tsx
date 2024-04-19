/*
Copyright (c) 2022,2024 Volkswagen AG
Copyright (c) 2022,2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2022,2024 Contributors to the Eclipse Foundation

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

import { Link, NavLink } from 'react-router-dom';

import HomeIcon from '@/assets/icons/home.svg';
import CatalogIcon from '@/assets/icons/catalog.svg';
import StockIcon from '@/assets/icons/stock.svg';
import TrashIcon from '@/assets/icons/trash.svg';
import { Typography } from '@catena-x/portal-shared-components';
import { Role } from '@models/types/auth/role';
import { useAuth } from '@hooks/useAuth';

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
    icon: string;
    requiredRoles?: Role[];
};

const sideBarItems: SideBarItemProps[] = [
    {
        name: 'Dashboard',
        icon: HomeIcon,
        path: '/dashboard',
    },
    {
        name: 'Stocks',
        icon: StockIcon,
        path: '/stocks',
    },
    {
        name: 'Catalog',
        icon: CatalogIcon,
        path: '/catalog',
        requiredRoles: ['PURIS_ADMIN'],
    },
    {
        name: 'Negotiations',
        icon: CatalogIcon,
        path: '/negotiations',
        requiredRoles: ['PURIS_ADMIN'],
    },
    {
        name: 'Transfers',
        icon: CatalogIcon,
        path: '/transfers',
        requiredRoles: ['PURIS_ADMIN'],
    },
    {
        name: 'Logout',
        icon: TrashIcon,
        variant: 'button',
    },
];

const calculateClassName = ({ isActive = false, isPending = false, isTransitioning = false }) => {
    const defaultClasses = 'flex items-center px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 active:bg-gray-300 w-full';
    return `${defaultClasses}${isActive || isPending || isTransitioning ? ' bg-gray-300' : ''}`;
}

const SideBarItem = (props: SideBarItemProps) => {
    const { hasRole } = useAuth();
    if (props.requiredRoles && !hasRole(props.requiredRoles)) {
        return null;
    }
    return (
        <li key={props.name}>
            {props.variant === 'button' ? (
                <button className={calculateClassName({})} onClick={props.action}>
                    <img className="mr-2" src={props.icon} alt="Icon" /> <span className="min-w-0 break-words">{props.name}</span>
                </button>
            ) : (
                <NavLink to={props.path} className={calculateClassName}>
                    <img className="mr-2" src={props.icon} alt="Icon" /> <span className="min-w-0 break-words">{props.name}</span>
                </NavLink>
            )}
        </li>
    );
}

export const SideBar = () => {
    return (
        <aside className="flex flex-col flex-shrink-0 gap-5 h-full w-64 border-r py-5 px-3 overflow-y-auto">
            <header className="flex justify-center">
                <Typography variant="h2" className="text-3xl font-semibold text-center text-blue-800">
                    PURIS
                </Typography>
            </header>
            <nav>
                <ul className="flex flex-col gap-3">
                    {sideBarItems.map((item) => (
                        <SideBarItem key={item.name} {...item} />
                    ))}
                </ul>
            </nav>
            <footer className="flex justify-center mt-auto">
                <Link to="/aboutLicense" className="hover:text-gray-500">
                    About License
                </Link>
            </footer>
        </aside>
    );
}
