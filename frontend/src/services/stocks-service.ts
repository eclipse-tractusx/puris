import { config } from '../models/constants/config';
import { MaterialStock, ProductStock } from '../models/types/data/stock';

export const postProductStocks =async (stock: ProductStock) => {
  return fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_PRODUCT_STOCKS, {
    method: 'POST',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
}

export const putProductStocks = async (stock: ProductStock) => {
  return fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_PRODUCT_STOCKS, {
    method: 'PUT',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
}

export const postMaterialStocks = async (stock: MaterialStock) => {
  return fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_MATERIAL_STOCKS, {
    method: 'POST',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
}

export const putMaterialStocks = async (stock: MaterialStock) => {
  return fetch(config.app.BACKEND_BASE_URL + config.app.ENDPOINT_MATERIAL_STOCKS, {
    method: 'PUT',
    body: JSON.stringify(stock),
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
}

export const refreshPartnerStocks = (type: 'material' | 'product', materialNumber: string | null) => {
  const endpoint = type === 'product' ? config.app.ENDPOINT_UPDATE_REPORTED_PRODUCT_STOCKS : config.app.ENDPOINT_UPDATE_REPORTED_MATERIAL_STOCKS;
  return fetch(`${config.app.BACKEND_BASE_URL}${endpoint}${materialNumber}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-API-KEY': config.app.BACKEND_API_KEY,
    },
  });
}
