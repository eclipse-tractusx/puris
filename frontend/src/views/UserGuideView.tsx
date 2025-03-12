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

import Markdown from 'react-markdown';

import markdownFile from '@assets/User_Guide.md';
import { useEffect, useState } from 'react';
import { Box } from '@mui/material';
import { useTitle } from '@contexts/titleProvider';

export const UserGuideView = () => {
    const [markdown, setMarkdown] = useState<string>('');
    const { setTitle } = useTitle();

    useEffect(() => {
        setTitle('User Guide');
    }, [setTitle]);
    useEffect(() => {
        fetch(markdownFile)
            .then((response) => response.text())
            .then((text) => {
                setMarkdown(text);
            });
    }, []);
    return (
        <Box marginInline="auto" maxWidth="60rem">
            <Markdown className="markdown">{markdown}</Markdown>
        </Box>
    );
};
