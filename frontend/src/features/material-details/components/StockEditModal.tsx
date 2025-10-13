/*
Copyright (c) 2025 Volkswagen AG
Copyright (c) 2025 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)

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

import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogTitle,
  FormLabel,
  Grid,
  InputLabel,
  Stack,
  capitalize
} from '@mui/material';
import {
  Input
} from '@catena-x/portal-shared-components';
import { Save, Close } from '@mui/icons-material';
import { UNITS_OF_MEASUREMENT } from '@models/constants/uom';
import { Stock, StockType } from '@models/types/data/stock';
import { getUnitOfMeasurement, isValidOrderReference } from '@util/helpers';
import { LabelledAutoComplete } from '@components/ui/LabelledAutoComplete';
import { GridItem } from '@components/ui/GridItem';
import { usePartners } from '@features/stock-view/hooks/usePartners';
import { useSites } from '@features/stock-view/hooks/useSites';
import { postStocks } from '@services/stocks-service';
import { useNotifications } from '@contexts/notificationContext';
import { ModalMode } from '@models/types/data/modal-mode';

type StockEditModalProps = {
  open: boolean;
  stock: Partial<Stock> | null;
  stockType: StockType;
  onClose: () => void;
  onSave: (updated: Stock) => void;
  mode: ModalMode;
};

const isValidStock = (stock: Partial<Stock>) =>
  stock &&
  stock.quantity &&
  stock.measurementUnit &&
  stock.partner &&
  stock.stockLocationBpns &&
  stock.stockLocationBpna &&
  isValidOrderReference(stock);

export const StockEditModal = ({
  open,
  onClose,
  onSave,
  stock,
  stockType,
  mode
}: StockEditModalProps) => {
  const { notify } = useNotifications();
  const { sites } = useSites();
  const { partners } = usePartners(stockType, stock?.material?.ownMaterialNumber ?? null);

  const [formData, setFormData] = useState<Partial<Stock>>(stock ?? {});
  const [originalData, setOriginalData] = useState<Partial<Stock>>(stock ?? {});
  const [formError, setFormError] = useState(false);

  useEffect(() => {
    setFormData(stock ?? {});
    setOriginalData(stock ?? {});
  }, [stock]);

  const isFormChanged = JSON.stringify(formData) !== JSON.stringify(originalData);

  const handleChange = <T extends keyof Stock>(field: T, value: Stock[T] | undefined) => {
    setFormData((prev: any) => ({ ...prev, [field]: value }));
  };

  const handleSave = () => {
    formData.customerOrderNumber ||= undefined;
    formData.customerOrderPositionNumber ||= undefined;
    formData.supplierOrderNumber ||= undefined;
    if (!isValidStock(formData)) {
      setFormError(true);
      return;
    }
    setFormError(false);
    postStocks(stockType, {
      ...formData,
      lastUpdatedOn: new Date().toISOString()
    },
      mode)
      .then(() => {
        notify({
          title: 'Stock Updated',
          description: 'Stock has been saved',
          severity: 'success'
        });
        onSave(formData as Stock);
        handleClose();
      })
      .catch((error) => {
        notify({
          title: error.status === 409 ? 'Conflict' : 'Error requesting update',
          description: error.status === 409 ? 'Date conflicting with another Stock' : error.error,
          severity: 'error',
        });
        handleClose();
      });
  };

  const handleClose = () => {
    setFormError(false);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} data-testid="stock-edit-modal">
      <DialogTitle variant="h3" textAlign="center">
        {capitalize(mode)} {capitalize(stockType)} Stock
      </DialogTitle>
      <Stack padding="0 2rem 2rem" sx={{ width: '60rem' }}>
        <Grid container spacing={2} padding=".25rem">
          <GridItem label="Material Number" value={stock?.material?.ownMaterialNumber ?? ''} />

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="partner"
              options={partners ?? []}
              getOptionLabel={(option) => option?.name ?? ''}
              label="Partner*"
              placeholder="Select a Partner"
              error={formError && !formData.partner}
              onChange={(_, value) => handleChange('partner', value ?? undefined)}
              value={formData.partner ?? null}
              isOptionEqualToValue={(option, value) => option?.uuid === value?.uuid}
              data-testid="stock-partner-field"
              disabled={mode === 'edit'}
            />
          </Grid>

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="stockLocationBpns"
              options={sites ?? []}
              getOptionLabel={(option) => option.name ?? ''}
              error={formError}
              isOptionEqualToValue={(option, value) => option?.bpns === value?.bpns}
              onChange={(_, value) =>
                setFormData({
                  ...formData,
                  stockLocationBpns: value?.bpns ?? undefined,
                  stockLocationBpna: undefined
                })
              }
              value={sites?.find((s) => s.bpns === formData.stockLocationBpns) ?? null}
              label="Stock Site*"
              placeholder="Select a Site"
              data-testid="stock-site-field"
              disabled={mode === 'edit'}
            />
          </Grid>

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="stockLocationBpna"
              options={
                sites?.find((site) => site.bpns === formData.stockLocationBpns)?.addresses ?? []
              }
              getOptionLabel={(option) => option.streetAndNumber ?? ''}
              error={formError}
              isOptionEqualToValue={(option, value) => option?.bpna === value?.bpna}
              onChange={(_, value) =>
                handleChange('stockLocationBpna', value?.bpna)
              }
              value={
                sites
                  ?.find((site) => site.bpns === formData.stockLocationBpns)
                  ?.addresses?.find((a) => a.bpna === formData.stockLocationBpna) ?? null
              }
              label="Stock Address*"
              placeholder="Select an address"
              disabled={!formData.stockLocationBpns || mode === 'edit'}
              data-testid="stock-address-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Quantity*</FormLabel>
            <Input
              id="quantity"
              type="number"
              placeholder="Enter quantity"
              value={formData.quantity ?? ''}
              error={formError && !formData.quantity}
              onChange={(e) => handleChange('quantity', parseFloat(e.target.value))}
              sx={{ marginTop: '.5rem' }}
              data-testid="stock-quantity-field"
            />
          </Grid>

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="uom"
              value={
                formData.measurementUnit
                  ? {
                    key: formData.measurementUnit,
                    value: getUnitOfMeasurement(formData.measurementUnit)
                  }
                  : null
              }
              options={UNITS_OF_MEASUREMENT}
              getOptionLabel={(option) => option?.value ?? ''}
              label="UOM*"
              placeholder="Select unit"
              error={formError && !formData.measurementUnit}
              onChange={(_, value) => handleChange('measurementUnit', value?.key)}
              isOptionEqualToValue={(option, value) => option?.key === value?.key}
              data-testid="stock-uom-field"
              disabled={mode === 'edit'}
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Customer Order Number</FormLabel>
            <Input
              id="customer-order-number"
              type="text"
              error={formError && !isValidOrderReference(formData)}
              value={formData?.customerOrderNumber ?? ''}
              onChange={(event) =>
                handleChange('customerOrderNumber', event.target.value ?? '')
              }
              data-testid="stock-customer-order-number-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Customer Order Position</FormLabel>
            <Input
              id="customer-order-position-number"
              type="text"
              error={formError && !isValidOrderReference(formData)}
              value={formData?.customerOrderPositionNumber ?? ''}
              onChange={(event) =>
                handleChange('customerOrderPositionNumber', event.target.value ?? '')
              }
              data-testid="stock-customer-order-position-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Supplier Order Number</FormLabel>
            <Input
              id="supplier-order-number"
              type="text"
              value={formData?.supplierOrderNumber ?? ''}
              onChange={(event) =>
                handleChange('supplierOrderNumber', event.target.value ?? '')
              }
              data-testid="stock-supplier-order-number-field"
            />
          </Grid>


          <Grid item xs={6} alignContent="end">
            <Stack direction="row" alignItems="center">
              <Checkbox
                checked={formData?.isBlocked ?? false}
                onChange={(event) => handleChange('isBlocked', event.target.checked)}
                data-testid="stock-blocked-field"
              />
              <InputLabel htmlFor="isBlocked"> is Blocked </InputLabel>
            </Stack>
          </Grid>
        </Grid>

        <Box display="flex" gap="1rem" width="100%" justifyContent="end" marginTop="2rem">
          <Button variant="outlined" color="primary" sx={{ display: 'flex', gap: '.25rem' }} onClick={handleClose}>
            <Close></Close> Close
          </Button>
          <Button
            sx={{ display: 'flex', gap: '.25rem' }}
            onClick={handleSave}
            data-testid="save-delivery-button"
            disabled={mode == 'edit' && !isFormChanged}
          >
            <Save></Save> Save
          </Button>
        </Box>
      </Stack>
    </Dialog>
  );
};
