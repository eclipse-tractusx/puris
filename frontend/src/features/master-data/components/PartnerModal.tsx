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
import { useNotifications } from "@contexts/notificationContext";
import { Add, AddCircleOutline, Close, RemoveCircleOutline, Send } from "@mui/icons-material";
import { Input } from '@catena-x/portal-shared-components';
import { Box, Button, Dialog, DialogTitle, Divider, Grid, IconButton, InputLabel, Stack, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { Site } from "@models/types/edc/site";
import { Address } from "@models/types/edc/address";
import { Partner } from "@models/types/edc/partner";
import { BPNA, BPNS } from "@models/types/edc/bpn";


type PartnerCreateModalProps = {
    open: boolean;
    onClose: () => void;
    onSave: (partner: Partial<Partner>) => Promise<void> | void;
};

type AddressForm = {
    bpna: string;
    street: string;
    number: string;
    zipCode: string;
    city: string;
    country: string;
};

type SiteForm = { bpns: string; name: string; addresses: AddressForm[]; };

type PartnerForm = {
    name: string;
    bpnl: string;
    edcUrl: string;
    addresses: AddressForm[];
    sites: SiteForm[];
};

const isHttpUrl = (s: string) => {
    try { const u = new URL(s.trim()); return u.protocol === 'http:' || u.protocol === 'https:'; } catch { return false; }
};

const RE_BPNL = /^BPNL[A-Z0-9]{12}$/;
const RE_BPNA = /^BPNA[A-Z0-9]{12}$/;
const RE_BPNS = /^BPNS[A-Z0-9]{12}$/;
const RE_NUMERIC = /^\d+$/;

const createEmptyAddress = (): AddressForm => ({ bpna: '', street: '', number: '', zipCode: '', city: '', country: '' });
const createEmptySite = (): SiteForm => ({ bpns: '', name: '', addresses: [createEmptyAddress()] });
const partnerToForm = (): PartnerForm => ({ name: '', bpnl: '', edcUrl: '', addresses: [], sites: [createEmptySite()] });

const formToPartner = (form: PartnerForm): Partial<Partner> => {
    const mapAddress = (a: AddressForm): Address => ({
        bpna: a.bpna as BPNA,
        streetAndNumber: [a.street, a.number].filter(Boolean).join(' ').trim(),
        zipCodeAndCity: [a.zipCode, a.city].filter(Boolean).join(' ').trim(),
        country: a.country.trim(),
    });
    const mapSite = (s: SiteForm): Site => ({ bpns: s.bpns as BPNS, name: s.name.trim(), addresses: s.addresses.map(mapAddress) });
    return { name: form.name.trim(), bpnl: form.bpnl.trim() as any, edcUrl: form.edcUrl.trim(), addresses: form.addresses.map(mapAddress), sites: form.sites.map(mapSite) };
};

const isValidPartnerForm = (form: PartnerForm) => {
    if (!form.name.trim() || !RE_BPNL.test(form.bpnl.trim()) || !isHttpUrl(form.edcUrl)) return false;
    
    const addrOk = (a: AddressForm) => RE_BPNA.test(a.bpna.trim()) && a.street.trim() && RE_NUMERIC.test(a.number.trim()) && RE_NUMERIC.test(a.zipCode.trim()) && a.city.trim() && a.country.trim();
    if (!form.addresses.every(addrOk)) return false;

    if (!form.sites.length) return false;
    const sitesOk = form.sites.every((s) => RE_BPNS.test(s.bpns.trim()) && s.addresses.length && s.addresses.every(addrOk));
    return sitesOk;
};

export const PartnerCreateModal = ({ open, onClose, onSave }: PartnerCreateModalProps) => {
    const [form, setForm] = useState<PartnerForm>(partnerToForm());
    const [formError, setFormError] = useState(false);
    const { notify } = useNotifications();

    useEffect(() => { if (!open) return; setFormError(false); }, [open]);

    const handleClose = () => {
        setFormError(false);
        setForm(partnerToForm());
        onClose();
    };

    const handleFieldChange = (field: keyof PartnerForm, value: string) => {
        setForm((prev) => ({ ...prev, [field]: field === 'bpnl' ? value.toUpperCase() : value }));
    };

    const handleAddressChange = (index: number, field: keyof AddressForm, value: string) => {
        setForm((prev) => {
            const sanitized = field === 'number' || field === 'zipCode' ? value.replace(/\D/g, '') : field === 'bpna' ? value.toUpperCase() : value;

            const addresses = prev.addresses.map((addr, i) => i === index ? { ...addr, [field]: sanitized } : addr );
            return { ...prev, addresses };
        });
    };

    const handleAddAddress = () => {
        setForm((prev) => ({ ...prev, addresses: [...prev.addresses, createEmptyAddress()] }));
    };

    const handleRemoveAddress = (index: number) => {
        setForm((prev) => ({
            ...prev,
            addresses: prev.addresses.filter((_, i) => i !== index),
        }));
    };

    const handleSiteFieldChange = (siteIndex: number, field: keyof SiteForm, value: string) => {
        setForm((prev) => {
            const sanitized = field === 'bpns' ? value.toUpperCase() : value;
            const sites = prev.sites.map((site, i) => i === siteIndex ? { ...site, [field]: sanitized } : site);
            return { ...prev, sites };
        });
    };

    const handleAddSite = () => {
        setForm((prev) => ({ ...prev, sites: [...prev.sites, createEmptySite()]}));
    };

    const handleRemoveSite = (siteIndex: number) => {
        setForm((prev) => {
            if (siteIndex === 0) return prev;
            return { ...prev, sites: prev.sites.filter((_, i) => i !== siteIndex) };
        });
    };

    const handleSiteAddressChange = (siteIndex: number, addressIndex: number, field: keyof AddressForm, value: string,
    ) => {
        setForm((prev) => {
            const sites = prev.sites.map((site, i) => {
                if (i !== siteIndex) return site;

                const sanitized = field === 'number' || field === 'zipCode' ? value.replace(/\D/g, '') : field === 'bpna' ? value.toUpperCase() : value;
                const addresses = site.addresses.map((addr, j) => j === addressIndex ? { ...addr, [field]: sanitized } : addr);
                return { ...site, addresses };
            });
            return { ...prev, sites };
        });
    };

    const handleAddSiteAddress = (siteIndex: number) => {
        setForm((prev) => {
            const sites = prev.sites.map((site, i) => i === siteIndex ? { ...site, addresses: [...site.addresses, createEmptyAddress()] } : site);
            return { ...prev, sites };
        });
    };

    const handleRemoveSiteAddress = (siteIndex: number, addressIndex: number) => {
        setForm((prev) => {
            const sites = prev.sites.map((site, i) => {
                if (i !== siteIndex) return site;
                if (addressIndex === 0) return site;
                return { ...site, addresses: site.addresses.filter((_, j) => j !== addressIndex) };
            });
            return { ...prev, sites };
        });
    };

    const handleSaveClick = async () => {
        if (!isValidPartnerForm(form)) {
            setFormError(true);
            return;
        }

        setFormError(false);
        try {
            const partnerToSave = formToPartner(form);
            await onSave(partnerToSave);
            notify({
                title: 'Partner created',
                description: 'Partner has been created',
                severity: 'success',
            });
            handleClose();
        } catch (error: any) {
            notify({
                title: 'Error saving partner',
                description: error?.message ?? 'Unknown error',
                severity: 'error',
            });
            handleClose();
        }
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="lg" fullWidth >
            <DialogTitle variant="h3" textAlign="center">New Partner</DialogTitle>
            <Stack padding="0 2rem 2rem" sx={{ maxWidth: '100%' }}>
                <Grid container spacing={1} padding=".25rem">
                    <Grid item xs={6}>
                        <InputLabel>Partner Name*</InputLabel>
                        <Input
                            id="partner-name"
                            type="text"
                            value={form.name}
                            onChange={(e) => handleFieldChange('name', e.target.value)}
                            placeholder="Enter partner name"
                            error={formError && !form.name.trim()}
                            data-testid="partner-modal-name"
                        />
                    </Grid>
                    <Grid item xs={6}>
                        <InputLabel>BPNL*</InputLabel>
                        <Input
                            id="partner-bpnl"
                            type="text"
                            value={form.bpnl}
                            onChange={(e) => handleFieldChange('bpnl', e.target.value)}
                            placeholder="Enter BPNL"
                            error={formError && !RE_BPNL.test(form.bpnl.trim())}
                            data-testid="partner-modal-bpnl"
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <InputLabel>EDC URL*</InputLabel>
                        <Input
                            id="partner-edc-url"
                            type="text"
                            value={form.edcUrl}
                            onChange={(e) => handleFieldChange('edcUrl', e.target.value)}
                            placeholder="Enter EDC URL"
                            error={formError && !isHttpUrl(form.edcUrl)}
                            data-testid="partner-modal-edc-url"
                        />
                    </Grid>
                </Grid>

                <Divider sx={{ my: 2 }} />
                <Box>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="h6">Addresses</Typography>
                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={<AddCircleOutline />}
                            onClick={handleAddAddress}
                            data-testid="partner-modal-add-address"
                        >
                            <Add></Add> Add address
                        </Button>
                    </Box>

                    {(!form.addresses || form.addresses.length === 0) && (
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>No addresses created</Typography>
                    )}

                    {form.addresses.map((address, index) => (
                        <Box key={index} sx={{ border: '1px solid #e0e0e0', borderRadius: '.5rem', padding: '0.75rem', mb: 1 }} >
                            <Box display="flex" justifyContent="space-between" alignItems="center">
                                <Typography variant="subtitle1">Address {index + 1}</Typography>
                                {form.addresses.length > 1 && (
                                    <IconButton
                                        size="small"
                                        onClick={() => handleRemoveAddress(index)}
                                        data-testid={`partner-modal-remove-address-${index}`}
                                    >
                                        <RemoveCircleOutline fontSize="small" />
                                    </IconButton>
                                )}
                            </Box>
                            <Grid container spacing={1} mt={0.5}>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>BPNA*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.bpna}
                                        onChange={(e) => handleAddressChange(index, 'bpna', e.target.value)}
                                        placeholder="Enter BPNA"
                                        error={formError && !RE_BPNA.test(address.bpna.trim())}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>Street*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.street}
                                        onChange={(e) => handleAddressChange(index, 'street', e.target.value)}
                                        placeholder="Street"
                                        error={formError && !address.street.trim()}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>Number*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.number}
                                        onChange={(e) => handleAddressChange(index, 'number', e.target.value)}
                                        placeholder="Number"
                                        error={formError && !RE_NUMERIC.test(address.number.trim())}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>ZIP code*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.zipCode}
                                        onChange={(e) => handleAddressChange(index, 'zipCode', e.target.value)}
                                        placeholder="ZIP code"
                                        error={formError && !RE_NUMERIC.test(address.number.trim())}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>City*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.city}
                                        onChange={(e) => handleAddressChange(index, 'city', e.target.value)}
                                        placeholder="City"
                                        error={formError && !address.city.trim()}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={4}>
                                    <InputLabel>Country*</InputLabel>
                                    <Input
                                        type="text"
                                        value={address.country}
                                        onChange={(e) => handleAddressChange(index, 'country', e.target.value)}
                                        placeholder="Country"
                                        error={formError && !address.country.trim()}
                                    />
                                </Grid>
                            </Grid>
                        </Box>
                    ))}
                </Box>

                <Divider sx={{ my: 2 }} />
                <Box>
                    <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="h6">Sites*</Typography>
                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={<AddCircleOutline />}
                            onClick={handleAddSite}
                            data-testid="partner-modal-add-site"
                        >
                            <Add></Add> Add site
                        </Button>
                    </Box>

                    {form.sites.map((site, siteIndex) => (
                        <Box key={siteIndex} sx={{ border: '1px solid #e0e0e0', borderRadius: '.5rem', padding: '0.75rem', mb: 1 }} >
                            {siteIndex > 0 && (
                                <Box display="flex" justifyContent="space-between" alignItems="center">
                                <Typography variant="subtitle1">Site {siteIndex + 1}</Typography>
                                <IconButton
                                    size="small"
                                    onClick={() => handleRemoveSite(siteIndex)}
                                    data-testid={`partner-modal-remove-site-${siteIndex}`}
                                >
                                    <RemoveCircleOutline fontSize="small" />
                                </IconButton>
                            </Box>
                            )}

                            <Grid container spacing={1} mt={0.5}>
                                <Grid item xs={12} sm={6}>
                                    <InputLabel>BPNS*</InputLabel>
                                    <Input
                                        type="text"
                                        value={site.bpns}
                                        onChange={(e) => handleSiteFieldChange(siteIndex, 'bpns', e.target.value)}
                                        placeholder="Enter BPNS"
                                        error={formError && !RE_BPNS.test(site.bpns.trim())}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <InputLabel>Site Name*</InputLabel>
                                    <Input
                                        type="text"
                                        value={site.name}
                                        onChange={(e) => handleSiteFieldChange(siteIndex, 'name', e.target.value)}
                                        placeholder="Enter site name"
                                    />
                                </Grid>
                            </Grid>
                            
                            <Box mt={1}>
                                <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5} >
                                    <Typography variant="subtitle2">Addresses*</Typography>
                                    <Button
                                        variant="text"
                                        size="small"
                                        startIcon={<AddCircleOutline />}
                                        onClick={() => handleAddSiteAddress(siteIndex)}
                                        data-testid={`partner-modal-add-site-address-${siteIndex}`}
                                    >
                                        Add address
                                    </Button>
                                </Box>

                                {site.addresses.map((address, addressIndex) => (
                                    <Box key={addressIndex} sx={{ border: '1px dashed #e0e0e0', borderRadius: '.5rem', padding: '0.5rem', mb: 0.75 }} >
                                        {addressIndex > 0 && (<Box display="flex" justifyContent="space-between" alignItems="center">
                                            <Typography variant="body2">Address {addressIndex + 1}</Typography>
                                            <IconButton
                                                size="small"
                                                onClick={() => handleRemoveSiteAddress(siteIndex, addressIndex)}
                                                data-testid={`partner-modal-remove-site-address-${siteIndex}-${addressIndex}`}
                                            >
                                                <RemoveCircleOutline fontSize="small" />
                                            </IconButton>
                                        </Box>
                                        )}
                                        <Grid container spacing={1} mt={0.5}>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>BPNA*</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.bpna}
                                                    onChange={(e) => handleSiteAddressChange(siteIndex, addressIndex, 'bpna', e.target.value)}
                                                    placeholder="Enter BPNA"
                                                    error={formError && !RE_BPNA.test(address.bpna.trim())}
                                                />
                                            </Grid>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>Street</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.street}
                                                    onChange={(e) => handleSiteAddressChange( siteIndex, addressIndex, 'street', e.target.value)}
                                                    placeholder="Street"
                                                />
                                            </Grid>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>Number</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.number}
                                                    onChange={(e) => handleSiteAddressChange(siteIndex, addressIndex, 'number', e.target.value)}
                                                    placeholder="Number"
                                                    error={formError && !RE_NUMERIC.test(address.number.trim())}
                                                />
                                            </Grid>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>ZIP code</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.zipCode}
                                                    onChange={(e) => handleSiteAddressChange(siteIndex, addressIndex, 'zipCode', e.target.value)}
                                                    placeholder="ZIP code"
                                                    error={formError && !RE_NUMERIC.test(address.number.trim())}
                                                />
                                            </Grid>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>City</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.city}
                                                    onChange={(e) => handleSiteAddressChange(siteIndex, addressIndex, 'city', e.target.value)}
                                                    placeholder="City"
                                                />
                                            </Grid>
                                            <Grid item xs={12} sm={4}>
                                                <InputLabel>Country</InputLabel>
                                                <Input
                                                    type="text"
                                                    value={address.country}
                                                    onChange={(e) => handleSiteAddressChange(siteIndex, addressIndex, 'country', e.target.value)}
                                                    placeholder="Country"
                                                />
                                            </Grid>
                                        </Grid>
                                    </Box>
                                ))}
                            </Box>
                        </Box>
                    ))}
                </Box>
                <Box
                    display="flex"
                    gap="1rem"
                    width="100%"
                    justifyContent="end"
                    marginTop="1rem"
                >
                    <Button
                        variant="outlined"
                        color="primary"
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleClose}
                    >
                        <Close /> Close
                    </Button>
                    <Button
                        variant="contained"
                        sx={{ display: 'flex', gap: '.25rem' }}
                        onClick={handleSaveClick}
                        data-testid="partner-modal-save"
                    >
                        <Send /> Save
                    </Button>
                </Box>
            </Stack>
        </Dialog>
    );
};