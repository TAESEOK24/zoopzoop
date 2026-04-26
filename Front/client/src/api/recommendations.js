import axios from 'axios';
import { API_BASE_URL } from './index';

const recommendationApi = axios.create({
    baseURL: `${API_BASE_URL}/api/recommendations`,
    headers: {
        'Content-Type': 'application/json',
    },
});

recommendationApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export const fetchPersonalizedRecommendations = async (size = 6) => {
    const response = await recommendationApi.get('/personalized', {
        params: { size },
    });
    return response.data;
};
