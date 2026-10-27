import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || '/api/v1'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (token && !['/auth/login', '/auth/register'].includes(config.url || '')) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !['/auth/login', '/auth/register'].includes(error.config?.url || '')) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      const basePath = import.meta.env.BASE_URL.replace(/\/$/, '');
      if (window.location.pathname !== `${basePath}/login`) {
        sessionStorage.setItem('loginReturnTo', JSON.stringify({ pathname: window.location.pathname.slice(basePath.length) || '/', search: window.location.search }));
        window.location.replace(`${basePath}/login`)
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
