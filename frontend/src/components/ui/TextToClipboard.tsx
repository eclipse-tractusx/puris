import { useNotifications } from '@contexts/notificationContext';
import { ContentCopyOutlined } from '@mui/icons-material';
import { Button, Typography } from '@mui/material';

type TextToClipboardProps = {
    text?: string;
    color?: string;
};

export function TextToClipboard({ text = '', color }: TextToClipboardProps) {
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
    return (
        <Button variant="text" sx={{ padding: 0, justifyContent: 'start', width: 'fit-content' }} onClick={handleCopyText}>
            <Typography variant="body3" sx={{ display: 'flex', alignItems: 'center', gap: '0.25rem', ...(color && { color }) }}>
                {text}
                <ContentCopyOutlined />
            </Typography>
        </Button>
    );
}
