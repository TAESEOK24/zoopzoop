import axios from 'axios';
import { API_BASE_URL, attachAuthInterceptors } from './index';

const recommendationApi = attachAuthInterceptors(axios.create({
    baseURL: `${API_BASE_URL}/api/recommendations`,
    headers: {
        'Content-Type': 'application/json',
    },
}));


export const fetchPersonalizedRecommendations = async (size = 6) => {
    const response = await recommendationApi.get('/personalized', {
        params: { size },
    });
    return response.data;
};
