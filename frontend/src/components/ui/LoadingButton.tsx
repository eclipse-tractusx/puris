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

import { SvgIconComponent } from '@mui/icons-material';
import { Button, CircularProgress } from '@mui/material';

type LoadingButtonProps = {
    Icon: SvgIconComponent;
    isLoading: boolean;
    children: React.ReactNode;
} & React.ComponentProps<typeof Button>;

export function LoadingButton({ Icon, isLoading, children, onClick, sx = {}, ...props }: LoadingButtonProps) {
    return (
        <Button onClick={!isLoading ? onClick : undefined}  sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem', ...sx }} {...props}>
            {isLoading ? <CircularProgress color="inherit" size=".75rem" /> : <Icon/>} {children}
        </Button>
    );
}
