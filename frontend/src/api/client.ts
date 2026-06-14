import axios from 'axios';

const getBaseUrl = () => {
  const savedUrl = localStorage.getItem('ROOTCAUSE_API_URL');
  if (savedUrl) return savedUrl;
  return import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
};

const api = axios.create({
  baseURL: getBaseUrl(),
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Update endpoint configuration and attach API Key dynamically
api.interceptors.request.use((config) => {
  const savedUrl = localStorage.getItem('ROOTCAUSE_API_URL');
  if (savedUrl) {
    config.baseURL = savedUrl;
  }
  
  const savedKey = localStorage.getItem('ROOTCAUSE_API_KEY');
  const apiKey = savedKey || import.meta.env.VITE_API_KEY;
  if (apiKey) {
    config.headers['X-API-Key'] = apiKey;
  }
  return config;
});

// Normalize errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ??
      error.response?.data?.error ??
      error.message ??
      'An unexpected error occurred';
    return Promise.reject(new Error(message));
  }
);

export default api;
