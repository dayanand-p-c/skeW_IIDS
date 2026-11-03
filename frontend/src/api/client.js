import axios from 'axios';

let inMemoryToken = null;

export const setAuthToken = (token) => {
  inMemoryToken = token;
  if (token) {
    sessionStorage.setItem('dis_token', token);
  } else {
    sessionStorage.removeItem('dis_token');
  }
};

export const getAuthToken = () => {
  if (inMemoryToken) return inMemoryToken;
  const stored = sessionStorage.getItem('dis_token');
  if (stored) {
    inMemoryToken = stored;
  }
  return inMemoryToken;
};

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      setAuthToken(null);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
