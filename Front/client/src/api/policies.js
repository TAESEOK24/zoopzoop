import axios from 'axios';
import { API_BASE_URL } from './index';

const policyApi = axios.create({
    baseURL: `${API_BASE_URL}/policies`,
    headers: {
        'Content-Type': 'application/json',
    },
});

export const fetchPolicies = async ({
    query = '',
    type = '',
    age = '',
    region = '',
    special = '',
    page = 0,
    size = 6,
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
