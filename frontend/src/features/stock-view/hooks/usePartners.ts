import { useFetch } from '../../../hooks/useFetch';
import { config } from '../../../models/constants/config';
import { Partner } from '../../../models/types/edc/partner';

export const usePartners = (type: 'material' | 'product', materialNumber: string | null) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_CUSTOMER : config.app.ENDPOINT_SUPPLIER;
  const { data: partners, isLoading: isLoadingPartners } = useFetch<Partner[]>(
      `${config.app.BACKEND_BASE_URL}${endpoint}${materialNumber}`
  );
  return {
      partners,
      isLoadingPartners,
  };
}
