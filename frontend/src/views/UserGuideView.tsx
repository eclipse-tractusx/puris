import Markdown from 'react-markdown';

import markdownFile from '@assets/userGuide/User_Guide.md';
import { useEffect, useState } from 'react';
import { Box } from '@mui/material';

export const UserGuideView = () => {
  const [markdown, setMarkdown] = useState<string>('');
  useEffect(() => {
    fetch(markdownFile)
      .then((response) => response.text())
      .then((text) => {
        setMarkdown(text);
      });
  }, []);
  return (<Box marginInline="auto" maxWidth="48rem">
    <Markdown className='markdown'>
      {markdown}
    </Markdown>
  </Box>);
}
