/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
import { useState } from 'react';
import { Dialog, DialogTitle, DialogContent, Tooltip, Theme } from '@mui/material';
import { GridColDef, GridPaginationModel, GridRowClassNameParams, GridRowModel } from '@mui/x-data-grid';
import { Table } from '@catena-x/portal-shared-components';
import usePartnerDataUpdateBatch from '../hooks/usePartnerDataUpdateBatch';
import { BatchRunEntryDto } from '@models/types/data/batch';

type Props = {
  open: boolean;
  onClose: () => void;
  runId?: string | null;
};

const PartnerDataBatchDetailModal = ({ open, onClose, runId }: Props) => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { entries, isLoadingEntries, refreshEntries } = usePartnerDataUpdateBatch(runId ?? undefined, page, pageSize);

  const rows = entries?.content as BatchRunEntryDto[] ?? [];
  const total = entries?.page.totalElements ?? 0;

  const columns: GridColDef[] = [
    { field: 'ownMaterialNumber', headerName: 'Material', flex: 1 },
    { field: 'partnerName', headerName: 'Partner', flex: 1 },
    { field: 'informationType', headerName: 'Information Type', flex: 1 },
    { field: 'direction', headerName: 'Direction', flex: 0.5 },
    { field: 'status', headerName: 'Status', flex: 0.5 },
    { field: 'errorMessage', headerName: 'Error', flex: 1, renderCell: (params) => (
        params.value ? <Tooltip title={params.value}><span className="error-text">View</span></Tooltip> : null
    )},
  ];

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>Partner Data Update - Details</DialogTitle>
      <DialogContent>
        <div style={{ height: 400, width: '100%' }}>
          <Table
            title="Entries"
            columns={columns}
            rows={rows}
            rowCount={total}
            loading={isLoadingEntries}
            reload={() => refreshEntries()}
            pagination
            paginationMode="server"
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model: GridPaginationModel) => { setPage(model.page); setPageSize(model.pageSize); }}
            pageSizeOptions={[5, 10, 20, 50]}
            getRowId={(r: GridRowModel) => r.id}
            getRowClassName={(params: GridRowClassNameParams) => params.row?.status === 'ERROR' ? 'error-row' : ''}
            sx={{
              '& .error-row': {
                bgcolor: 'transparent',
                borderLeft: (theme: Theme) => `4px solid ${theme.palette.error.main}`,
              },
              '& .error-row .MuiDataGrid-cell': {
                color: (theme: Theme) => theme.palette.error.main,
              },
              '& .error-text': {
                color: 'inherit',
                fontWeight: 600,
              },
            }}
          />
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default PartnerDataBatchDetailModal;
