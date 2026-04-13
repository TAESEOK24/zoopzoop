import axios from 'axios';
import { API_BASE_URL } from './index';

const policyApi = axios.create({
    baseURL: `${API_BASE_URL}/api/policies`,
    headers: {
        'Content-Type': 'application/json',
    },
});

policyApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

export const fetchPolicies = async ({
    query = '',
    type = '',
    age = '',
    region = '',
    special = '',
    page = 0,
    size = 6,
    sort = 'views',
} = {}) => {
    const response = await policyApi.get('', {
        params: {
            query: query || undefined,
            type: type || undefined,
            age: age || undefined,
            region: region || undefined,
            special: special || undefined,
            page,
            size,
            sort,
        },
    });

    return response.data;
};

export const fetchPolicyTypes = async ({ query = '', age = '', region = '', special = '' } = {}) => {
    const response = await policyApi.get('/types', {
        params: {
            query: query || undefined,
            age: age || undefined,
            region: region || undefined,
            special: special || undefined,
        },
    });

    return response.data;
};

export const fetchPolicyDetail = async (serviceId) => {
    const response = await policyApi.get(`/${serviceId}`);
    return response.data;
};

// Chatbot policy search
export const searchPolicies = async (keyword, size = 5) => {
    const response = await policyApi.get('/search', {
        params: { keyword, size },
    });
    return response.data;
};

// Chatbot policy detail search by serviceId
export const searchPolicyDetail = async (serviceId) => {
    const response = await policyApi.get(`/search/${serviceId}`);
    return response.data;
};
