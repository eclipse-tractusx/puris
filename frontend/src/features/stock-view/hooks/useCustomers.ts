import { useFetch } from '../../../hooks/useFetch';
import { config } from '../../../models/constants/config';
import { Partner } from '../../../models/types/edc/partner';

export const useCustomers = (materialNumber: string) => {
  const { data: customers, isLoading: isLoadingCustomers } = useFetch<Partner>(
      `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_SUPPLIER}${materialNumber}`
  );
  return {
      customers,
      isLoadingCustomers,
  };
}
