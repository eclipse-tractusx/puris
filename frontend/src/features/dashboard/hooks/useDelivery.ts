import { useFetch } from '@hooks/useFetch';
import { config } from '@models/constants/config';
import { Delivery } from '@models/types/data/delivery';
import { BPNS } from '@models/types/edc/bpn';

export const useDelivery = (materialNumber: string | null, site: BPNS | null) => {
    const {
        data: deliveries,
        error: deliveriesError,
        isLoading: isLoadingDeliverys,
        refresh: refreshDelivery,
    } = useFetch<Delivery[]>(
        materialNumber && site
            ? `${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DELIVERY}?materialNumber=${materialNumber}&site=${site}`
            : undefined
    );
    return {
        deliveries,
        deliveriesError,
        isLoadingDeliverys,
        refreshDelivery,
    };
};
