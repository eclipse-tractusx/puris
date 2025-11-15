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
import { Dialog, DialogActions, DialogContent, DialogTitle, Button, Typography } from '@mui/material';
import { forwardRef, useImperativeHandle, useState } from 'react';

export type ConfirmUpdateOptions = {
  message: string;
};

export type ConfirmUpdateHandle = {
  open: (opts: ConfirmUpdateOptions) => Promise<boolean>;
};

export const ConfirmUpdateDialog = forwardRef<ConfirmUpdateHandle>(function ConfirmUpdateDialog(_, ref) {
  const [open, setOpen] = useState(false);
  const [opts, setOpts] = useState<ConfirmUpdateOptions>({ message: '' });
  const [resolver, setResolver] = useState<((v: boolean) => void) | null>(null);

  useImperativeHandle(ref, () => ({
    open: (nextOpts: ConfirmUpdateOptions) => {
      setOpts(nextOpts);
      setOpen(true);
      return new Promise<boolean>((resolve) => setResolver(() => resolve));
    },
  }));

  const close = (result: boolean) => {
    setOpen(false);
    if (resolver) resolver(result);
    setResolver(null);
  };

  return (
    <Dialog open={open} onClose={() => close(false)}>
      <DialogTitle>Do you want to update?</DialogTitle>
      <DialogContent>
        <Typography>{opts.message}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => close(false)}>No (Cancel)</Button>
        <Button variant="contained" onClick={() => close(true)}>Yes</Button>
      </DialogActions>
    </Dialog>
  );
});