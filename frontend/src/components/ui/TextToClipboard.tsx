/*
Copyright (c) 2054 Volkswagen AG
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

import { useNotifications } from '@contexts/notificationContext';
import { ContentCopyOutlined } from '@mui/icons-material';
import { Button, Typography } from '@mui/material';

type TextToClipboardProps = {
    text: string;
    variant?: 'default' | 'dark' | 'light';
};

export function TextToClipboard({ text, variant = 'default' }: TextToClipboardProps) {
    const { notify } = useNotifications();
    const handleCopyText = async (event: React.MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();
        try {
            await navigator.clipboard.writeText(text);
            notify({
                title: 'Copied to Clipboard',
                description: text,
                severity: 'success'
            });
        } catch (error) {
            notify({
                title: 'Error copying to Clipboard',
                description: '',
                severity: 'error'
            });
        }
    };
    const variantStyles = {
        light: { color: '#ccc' },
        dark: { color: '#000' },
        default: { color: null },
    };
    return (
        <Button variant="text" sx={{ padding: 0, justifyContent: 'start', width: 'fit-content' }} onClick={handleCopyText}>
            <Typography variant="body3" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem', ...variantStyles[variant] }}>
                {text}
                <ContentCopyOutlined />
            </Typography>
        </Button>
    );
}
