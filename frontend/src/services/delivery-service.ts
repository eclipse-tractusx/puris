import { config } from '@models/constants/config';
import { Delivery } from '@models/types/data/delivery';
import { UUID } from 'crypto';

export const postDelivery = async (delivery: Partial<Delivery>) => {
  const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_DELIVERY, {
    method: 'POST',
    body: JSON.stringify(delivery),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
  return res.json();
}

export const deleteDelivery = async (id: UUID) => {
  const res = await fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_DELIVERY + `/${id}`, {
    method: 'DELETE',
    headers: {
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
}

export const requestReportedDeliveries = async (materialNumber: string | null) => {
  const res = await fetch(`${config.app.BACKEND_BASE_URL}${config.app.ENDPOINT_DELIVERY}/reported/refresh?ownMaterialNumber=${materialNumber}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
  if(res.status >= 400) {
    const error = await res.json();
    throw error;
  }
  return res.json();
}
