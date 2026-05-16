import axios from 'axios';
import { clearAuthSession, getAccessToken, setAuthSession } from './authSession';

export const API_BASE_URL = 'http://localhost:8080';
export const ENDPOINTS = {
    AUTH: '/api/auth',
    POLICIES: '/api/policies',
    COMMUNITY: '/api/community',
};

let refreshPromise = null;

const refreshAccessToken = async () => {
    if (!refreshPromise) {
        refreshPromise = axios.post(`${API_BASE_URL}/api/auth/refresh`, null, {
            withCredentials: true,
        }).then((response) => {
            const authData = response.data?.data;
            setAuthSession(authData);
            return authData?.accessToken;
        }).finally(() => {
            refreshPromise = null;
        });
    }

    return refreshPromise;
};

export const logoutAPI = async () => {
    try {
        await axios.post(`${API_BASE_URL}/api/auth/logout`, null, {
            withCredentials: true,
        });
    } finally {
        clearAuthSession();
    }
};

export const attachAuthInterceptors = (axiosInstance) => {
    axiosInstance.interceptors.request.use((config) => {
        const token = getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        config.withCredentials = true;
        return config;
    });

    axiosInstance.interceptors.response.use(
        (response) => response,
        async (error) => {
            const originalRequest = error.config;
            const status = error.response?.status;
            const isAuthEndpoint =
                originalRequest?.url?.includes('/api/auth') ||
                originalRequest?.baseURL?.includes('/api/auth');

            if (status !== 401 || originalRequest?._retry || isAuthEndpoint) {
                return Promise.reject(error);
            }

            originalRequest._retry = true;

            try {
                const token = await refreshAccessToken();
                if (!token) {
                    throw new Error('Access token refresh response is invalid.');
                }
                originalRequest.headers.Authorization = `Bearer ${token}`;
                originalRequest.withCredentials = true;
                return axiosInstance(originalRequest);
            } catch (refreshError) {
                clearAuthSession();
                return Promise.reject(refreshError);
            }
        }
    );

    return axiosInstance;
};

export const createApiClient = (baseURL) => attachAuthInterceptors(axios.create({
    baseURL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
}));

const instance = createApiClient(API_BASE_URL);

export default instance;
