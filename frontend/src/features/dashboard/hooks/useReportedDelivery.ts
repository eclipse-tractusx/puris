import { useFetch } from '@hooks/useFetch';
import { config } from '@models/constants/config';
import { Delivery } from '@models/types/data/delivery';

export const useReportedDelivery = (materialNumber: string | null) => {
  const {data: reportedDeliveries, error: reportedDeliveriesError, isLoading: isLoadingReportedDeliveries, refresh: refreshDelivery } = useFetch<Delivery[]>(materialNumber ? `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DELIVERY}/reported?materialNumber=${materialNumber}` : undefined);
  return {
    reportedDeliveries,
    reportedDeliveriesError,
    isLoadingReportedDeliveries,
    refreshDelivery,
  };
}
