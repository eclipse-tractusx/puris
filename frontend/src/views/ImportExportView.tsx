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

import { DropArea } from '@catena-x/portal-shared-components';
import { Accordion, AccordionDetails, AccordionSummary, Box, Link, Stack, Typography } from '@mui/material';
import { ConfidentialBanner } from '@components/ConfidentialBanner';
import { useTitle } from '@contexts/titleProvider';
import { useCallback, useEffect, useRef, useState } from 'react';
import React from 'react';
import { DataImportResult, uploadDocuments } from '@services/import-service';
import { useNotifications } from '@contexts/notificationContext';
import { ArrowDropDownOutlined, CheckOutlined, ErrorOutlineOutlined } from '@mui/icons-material';

interface UploadResult {
  fileName: string;
  result?: DataImportResult;
  error?: string;
}

export const ImportExportView = () => {
    const { setTitle } = useTitle();
    const { notify } = useNotifications();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [uploadResults, setUploadResults] = useState<UploadResult[]>([]);

    const templateFiles = [
        '/delivery-template.xlsx',
        '/demand-template.xlsx',
        '/production-template.xlsx',
        '/stock-template.xlsx',
    ];

    const supportedExtensions = ['.xlsx'];

    const validateFiles = (files: FileList | null) => {
        if (!files?.length) return [];

        return Array.from(files).filter(file => {
        const isValid = supportedExtensions.some(ext => 
            file.name.toLowerCase().endsWith(ext)
        );
        
        if (!isValid) {
            notify({
                title: 'Unsupported File Type',
                description: `File "${file.name}" is not supported. Supported extensions are: ${supportedExtensions.join(', ')}.`,
                severity: 'error',
            });
        }
        return isValid;
        });
    };

    const updateUploadResult = (fileName: string, update: Partial<UploadResult>) => {
        setUploadResults(prev => 
            prev.map(item => 
                item.fileName === fileName ? { ...item, ...update } : item
            )
        );
    };

    const processFiles = async (validFiles: File[]) => {
        const initialResults: UploadResult[] = validFiles.map(file => ({ fileName: file.name }));
        setUploadResults(initialResults);

        const uploadPromises = validFiles.map(async (file) => {
            try {
                const result = await uploadDocuments(file);
                updateUploadResult(file.name, { result });
            } catch (error) {
                updateUploadResult(file.name, { error: error instanceof Error ? error.message : String(error) });
            }
        });

        await Promise.allSettled(uploadPromises);
    };

    const handleFiles = useCallback(async (files: FileList | null) => {
        const validFiles = validateFiles(files);
        if (validFiles.length === 0) return;
        
        try {
            await processFiles(validFiles);
        } catch (error) {
            console.error('Unexpected error during file processing:', error);
        }
    }, [notify]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        handleFiles(e.target.files);
        e.target.value = '';
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        handleFiles(e.dataTransfer.files);
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
    };

    useEffect(() => {
        setTitle('Import');
    }, [setTitle])
    return (
        <Box width="100%" sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <ConfidentialBanner />
            <Stack
                component="section"
                sx={{ 
                    bgcolor: 'white', 
                    gap: 3, 
                    boxSizing: 'border-box', 
                    border: '1px solid #DCDCDC', 
                    borderRadius: '0.75rem !important',
                    overflow: 'hidden', 
                    paddingBottom: 2 
                }}
            >
                <Box sx={{bgcolor:'#081f4b', display: 'flex', alignItems: 'center', px: 3 , minHeight: '2rem' }}>
                    <Typography variant="body2" color='white' >
                        Import
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', px: 3 }}>
                    <Typography variant="body2">
                        Drag and drop files to import. If any error occur, you will be notified. 
                    </Typography>
                    <Typography variant="body2" component="div">
                        {templateFiles.map((file, idx) => {
                            const fileName = file.substring(file.lastIndexOf('/') + 1);
                            return (
                                <React.Fragment key={file}>
                                    <Link
                                        href={file}
                                        download={fileName}
                                        underline="hover"
                                        aria-label={`Download ${fileName} template`}
                                    >
                                        {fileName}
                                    </Link>
                                    {idx < templateFiles.length - 1 && ' | '}
                                </React.Fragment>
                            );
                        })}
                    </Typography>
                </Box>
                <Box sx={{ px: 3}}>
                    <Box onClick={() => fileInputRef.current?.click()} onDrop={handleDrop} onDragOver={handleDragOver}
                        sx={{ '& h3': { fontSize: '1.125rem' }, '& p': { fontSize: '.875rem' } }}>
                        <DropArea
                            size="normal"
                            error=""
                            translations={{
                                errorTitle: 'Sorry, something went wrong',
                                subTitle: 'Supports: xlsx, csv',
                                title: 'Drag & Drop or click to browse',
                            }}
                        />
                    </Box>
                    <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleFileChange}
                        multiple
                        accept=".xlsx,.csv"
                        hidden
                    />
                </Box>
                {uploadResults.length > 0 && (
                    <Box sx={{ px: 3 }}>
                        {uploadResults.map((result, index) => (
                        <UploadResultAccordion key={index} result={result} index={index} />
                        ))}
                    </Box>
                )}
            </Stack>
        </Box>
    );
};

const UploadResultAccordion = ({ result, index }: { result: UploadResult; index: number }) => {
  const hasError = result.error || (result.result?.errors.length ?? 0) > 0;
  const isProcessing = !result.result && !result.error;
  
  const getStatus = () => {
    if (isProcessing) return { message: "Processing...", color: 'transparent', icon: null };
    if (hasError) return { 
      message: result.result?.message || result.error || "", 
      color: '#ffebee', 
      icon: <ErrorOutlineOutlined sx={{ color: 'red' }} />
    };
    return { 
      message: result.result?.message || "", 
      color: '#e8f5e8', 
      icon: <CheckOutlined sx={{ color: 'green' }} />
    };
  };

  const status = getStatus();

  return (
    <Accordion 
      key={index} 
      sx={{ mb: 1, backgroundColor: status.color }}
    >
      <AccordionSummary expandIcon={<ArrowDropDownOutlined />}>
        <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
          {status.icon && <Box sx={{ mr: 2 }}>{status.icon}</Box>}
          
          <Typography variant="body2" sx={{ flex: 1, mr: 2 }}>
            {status.message}
          </Typography>
          
          <Typography variant="body2" sx={{ fontWeight: 'bold', marginLeft: 'auto' }}>
            {result.fileName}
          </Typography>
        </Box>
      </AccordionSummary>

      <AccordionDetails sx={{ maxHeight: "20rem", overflowY: "auto" }}>
        {(result.result?.errors && result.result.errors.length > 0) && (
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 'bold', color: 'red', mb: 2 }}>
              Errors found:
            </Typography>
            {result.result.errors.map((error, errorIndex) => (
              <Box key={errorIndex} sx={{ mb: 2, pl: 2 }}>
                <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                  Row {error.row}:
                </Typography>
                <Box sx={{ ml: 2 }}>
                  {error.errors.map((errorMsg, msgIndex) => (
                    <Typography key={msgIndex} variant="body2" sx={{ color: 'red', mb: 0.5 }}>
                      • {errorMsg}
                    </Typography>
                  ))}
                </Box>
              </Box>
            ))}
          </Box>
        )}
        
        {result.error && (
          <Typography variant="body2" color="red">
            {result.error}
          </Typography>
        )}
        
        {result.result && !result.result.errors.length && !result.error && (
          <Typography variant="body2" sx={{ color: 'green' }}>
            Success
          </Typography>
        )}
      </AccordionDetails>
    </Accordion>
  );
};

