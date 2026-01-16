/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation
Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
Copyright (c) 2025 IAV GmbH

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
import { postStocks, updateStocks } from '@services/stocks-service';
import { useNotifications } from '@contexts/notificationContext';
import { DirectionType } from '@models/types/erp/directionType';
import { useSiteDesignations } from '../hooks/useSiteDesignations';
import { Site } from '@models/types/edc/site';

type StockCreationModalProps = {
  open: boolean;
  stock: Partial<Stock> | null;
  stockType: StockType;
  onClose: () => void;
  onSave: (updated: Stock) => void
};
const isValidStock = (stock: Partial<Stock>) =>
  stock &&
  stock.quantity &&
  stock.measurementUnit &&
  stock.partner &&
  stock.stockLocationBpns &&
  stock.stockLocationBpna &&
  isValidOrderReference(stock);

export const StockCreationModal = ({
  open,
  onClose,
  onSave,
  stock,
  stockType
}: StockCreationModalProps) => {
  const [temporaryStock, setTemporaryStock] = useState<Partial<Stock>>(stock ?? {});
  const { partners } = usePartners(stockType, stock?.material?.ownMaterialNumber ?? null);
  const { siteDesignations } = useSiteDesignations(stock?.material?.ownMaterialNumber ?? null, stockType === 'material' ? DirectionType.Inbound : DirectionType.Outbound);
  const { notify } = useNotifications();
  const [formError, setFormError] = useState(false);
  const sites = siteDesignations?.reduce((acc: Site[], sd) => temporaryStock?.partner?.bpnl && sd.partnerBpnls.includes(temporaryStock.partner.bpnl) ? [...acc, sd.site] : acc, []) ?? [];
  const [originalData, setOriginalData] = useState<Partial<Stock>>(stock ?? {});
  const mode = temporaryStock?.uuid ? 'edit' : 'create';
  const isFormChanged = JSON.stringify(temporaryStock) !== JSON.stringify(originalData);

  useEffect(() => {
    setTemporaryStock(stock ?? {});
    setOriginalData(stock ?? {});
  }, [stock]);


  const handleSaveClick = () => {
    temporaryStock.customerOrderNumber ||= undefined;
    temporaryStock.customerOrderPositionNumber ||= undefined;
    temporaryStock.supplierOrderNumber ||= undefined;
    if (!isValidStock(temporaryStock)) {
      setFormError(true);
      return;
    }
    setFormError(false);

    const method = mode === 'create' ? postStocks : updateStocks;
    const successLabel = mode === 'create' ? 'Stock Added' : 'Stock Updated';
    const successDescription = mode === 'create' ? 'The Stock has been added' : 'The Stock has been successfully updated';


    method(stockType, {
      ...temporaryStock,
      lastUpdatedOn: new Date().toISOString()
    })
      .then((d) => {
        onSave(d);
        notify({
          title: successLabel,
          description: successDescription,
          severity: 'success'
        });
      })
      .catch((error) => {
        notify({
          title: error.status === 409 ? 'Conflict' : 'Error requesting update',
          description: error.status === 409 ? 'Stock conflicting with an existing one' : error.error,
          severity: 'error'
        });
      }).finally(() => handleClose());
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
              error={formError && !temporaryStock?.partner}
              onChange={(_, value) => setTemporaryStock({ ...temporaryStock, partner: value ?? undefined })}
              value={temporaryStock.partner ?? null}
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
                setTemporaryStock({ ...temporaryStock, stockLocationBpns: value?.bpns ?? undefined })
              }
              value={sites?.find((s) => s.bpns === temporaryStock.stockLocationBpns) ?? null}
              label="Stock Site*"
              placeholder="Select a Site"
              data-testid="stock-site-field"
              disabled={mode === 'edit' || !temporaryStock.partner}
            />
          </Grid>

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="stockLocationBpna"
              options={
                sites?.find((site) => site.bpns === temporaryStock.stockLocationBpns)?.addresses ?? []
              }
              getOptionLabel={(option) => option.streetAndNumber ?? ''}
              error={formError}
              isOptionEqualToValue={(option, value) => option?.bpna === value?.bpna}
              onChange={(_, value) =>
                setTemporaryStock({ ...temporaryStock, stockLocationBpna: value?.bpna ?? undefined })
              }
              value={
                sites
                  ?.find((site) => site.bpns === temporaryStock.stockLocationBpns)
                  ?.addresses?.find((a) => a.bpna === temporaryStock.stockLocationBpna) ?? null
              }
              label="Stock Address*"
              placeholder="Select an address"
              disabled={!temporaryStock.stockLocationBpns || mode === 'edit'}
              data-testid="stock-address-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Quantity*</FormLabel>
            <Input
              id="quantity"
              type="number"
              placeholder="Enter quantity"
              value={temporaryStock.quantity ?? ''}
              error={formError && !temporaryStock.quantity}
              onChange={(e) => setTemporaryStock({ ...temporaryStock, quantity: parseFloat(e.target.value) })}
              sx={{ marginTop: '.5rem' }}
              data-testid="stock-quantity-field"
            />
          </Grid>

          <Grid item xs={6}>
            <LabelledAutoComplete
              id="uom"
              value={
                temporaryStock.measurementUnit
                  ? {
                    key: temporaryStock.measurementUnit,
                    value: getUnitOfMeasurement(temporaryStock.measurementUnit)
                  }
                  : null
              }
              options={UNITS_OF_MEASUREMENT}
              getOptionLabel={(option) => option?.value ?? ''}
              label="UOM*"
              placeholder="Select unit"
              error={formError && !temporaryStock.measurementUnit}
              onChange={(_, value) => setTemporaryStock((curr) => ({ ...curr, measurementUnit: value?.key }))}
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
              error={formError && !isValidOrderReference(temporaryStock)}
              value={temporaryStock?.customerOrderNumber ?? ''}
              onChange={(event) =>
                setTemporaryStock({ ...temporaryStock, customerOrderNumber: event.target.value })
              }
              data-testid="stock-customer-order-number-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Customer Order Position</FormLabel>
            <Input
              id="customer-order-position-number"
              type="text"
              error={formError && !isValidOrderReference(temporaryStock)}
              value={temporaryStock?.customerOrderPositionNumber ?? ''}
              onChange={(event) =>
                setTemporaryStock({
                  ...temporaryStock,
                  customerOrderPositionNumber: event.target.value,
                })
              }
              data-testid="stock-customer-order-position-field"
            />
          </Grid>

          <Grid item xs={6}>
            <FormLabel>Supplier Order Number</FormLabel>
            <Input
              id="supplier-order-number"
              type="text"
              value={temporaryStock?.supplierOrderNumber ?? ''}
              onChange={(event) =>
                setTemporaryStock({ ...temporaryStock, supplierOrderNumber: event.target.value })
              }
              data-testid="stock-supplier-order-number-field"
            />
          </Grid>


          <Grid item xs={6} alignContent="end">
            <Stack direction="row" alignItems="center">
              <Checkbox
                checked={temporaryStock?.isBlocked ?? false}
                onChange={(event) =>
                  setTemporaryStock({ ...temporaryStock, isBlocked: event.target.checked })
                }
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
            onClick={handleSaveClick}
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
