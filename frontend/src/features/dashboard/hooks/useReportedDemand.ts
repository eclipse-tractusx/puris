import { useFetch } from '@hooks/useFetch';
import { config } from '@models/constants/config';
import { Demand } from '@models/types/data/demand';

export const useReportedDemand = (materialNumber: string | null) => {
  const {data: reportedDemands, error: reportedDemandsError, isLoading: isLoadingReportedDemands, refresh: refreshReportedDemands } = useFetch<Demand[]>(materialNumber ? `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DEMAND}/reported?ownMaterialNumber=${materialNumber}` : undefined);
  return {
    reportedDemands,
    reportedDemandsError,
    isLoadingReportedDemands,
    refreshReportedDemands,
  };
}
