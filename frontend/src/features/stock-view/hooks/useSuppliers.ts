import { useFetch } from '../../../hooks/useFetch';
import { config } from '../../../models/constants/config';
import { Partner } from '../../../models/types/edc/partner';

export const useSuppliers = (materialNumber: string) => {
    const { data: suppliers, isLoading: isLoadingSuppliers } = useFetch<Partner>(
        `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_SUPPLIER}${materialNumber}`
    );
    return {
        suppliers,
        isLoadingSuppliers,
    };
}
