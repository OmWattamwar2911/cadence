import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

// In-memory token storage (primary) with sessionStorage fallback
let inMemoryToken = sessionStorage.getItem('wallet_token') || null;

export const setAuthToken = (token) => {
  inMemoryToken = token;
  if (token) {
    sessionStorage.setItem('wallet_token', token);
  } else {
    sessionStorage.removeItem('wallet_token');
  }
};

export const getAuthToken = () => inMemoryToken;

const apiBase = import.meta.env.VITE_API_BASE_URL || '';

const apiClient = axios.create({
  baseURL: `${apiBase}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT Bearer token & Request ID
apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (!config.headers['X-Request-Id']) {
      config.headers['X-Request-Id'] = uuidv4();
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: 401 -> Clear token and redirect to /login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      setAuthToken(null);
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
