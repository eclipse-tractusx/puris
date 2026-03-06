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
import React, { useState } from 'react';
import { Box, Button, Chip, Stack } from '@mui/material';
import { GridRenderCellParams } from '@mui/x-data-grid';
import { Table } from '@catena-x/portal-shared-components';
import usePartnerDataUpdateBatch from '../hooks/usePartnerDataUpdateBatch';
import PartnerDataBatchDetailModal from './PartnerDataBatchDetailModal';
import { BatchRunDto } from '@models/types/data/batch';

const statusColor = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'COMPLETED_WITH_ERRORS':
      return 'warning';
    case 'FAILED':
      return 'error';
    case 'IN_PROGRESS':
      return 'info';
    default:
      return 'default';
  }
};

export const BatchView = () => {
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(0);
  const [sortModel, setSortModel] = useState<any>([{ field: 'startTime', sort: 'desc' }]);
  const [selected, setSelected] = useState<string | null>(null);
  const sortParam = sortModel && sortModel.length > 0 ? `${sortModel[0].field},${sortModel[0].sort}` : undefined;
  const { runs, isLoadingRuns, refreshRuns, triggerManualBatch } = usePartnerDataUpdateBatch(undefined, page, pageSize, sortParam);
  const rows = runs?.content as BatchRunDto[] ?? [];

  // refresh when paging changes to ensure data is reloaded
  React.useEffect(() => {
    void refreshRuns();
  }, [page, pageSize, refreshRuns]);

  // refresh when sort changes; reset to first page
  React.useEffect(() => {
    setPage(0);
    void refreshRuns();
  }, [sortParam]);

  const columns = [
    { field: 'startTime', headerName: 'Start Time', flex: 1, renderCell: (params: any) => new Date(params.row.startTime).toLocaleString() },
    { field: 'endTime', headerName: 'End Time', flex: 1, renderCell: (params: any) => params.row.endTime ? new Date(params.row.endTime).toLocaleString() : '-' },
    { field: 'durationInSeconds', headerName: 'Duration (s)', flex: 0.5 },
    { field: 'status', headerName: 'Status', flex: 0.5, renderCell: (params: GridRenderCellParams) => (
        <Chip label={params.value} color={statusColor(params.value)} size="small" />
    ) },
    { field: 'totalEntries', headerName: 'Total', flex: 0.4 },
    { field: 'totalErrorCount', headerName: 'Errors', flex: 0.4 },
  ];

  return (
    <Box>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <Button variant="contained" color="primary" onClick={async () => { const ok = await triggerManualBatch(); if (ok) refreshRuns(); }}>
          Update Partner Data Now
        </Button>
        <Button variant="outlined" onClick={() => refreshRuns()}>Refresh</Button>
      </Stack>

      <div style={{ height: 600, width: '100%' }}>
        <Table
          title="Partner Data Update Runs"
          columns={columns}
          rows={rows}
          rowCount={runs?.totalElements ?? 0}
          loading={isLoadingRuns}
          reload={() => refreshRuns()}
          pagination
          paginationMode="server"
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(model: any) => { setPage(model.page); setPageSize(model.pageSize); }}
          sortingMode="server"
          sortModel={sortModel}
          onSortModelChange={(model) => { setSortModel(model); }}
          pageSizeOptions={[5, 10, 20, 50]}
          onSelection={(ids) => setSelected((ids && ids.length > 0) ? (ids[0] as string) : null)}
          onRowClick={(params: any) => {
            const id = params?.id ?? params?.row?.id ?? (params?.row && params.row[0]);
            setSelected(id ?? null);
          }}
          onRowDoubleClick={(params: any) => {
            const id = params?.id ?? params?.row?.id ?? null;
            setSelected(id);
          }}
        />
      </div>

      <PartnerDataBatchDetailModal open={!!selected} onClose={() => setSelected(null)} runId={selected} />
    </Box>
  );
};

export default BatchView;
