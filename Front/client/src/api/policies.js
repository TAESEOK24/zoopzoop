import axios from 'axios';
import { API_BASE_URL, attachAuthInterceptors } from './index';

const policyApi = attachAuthInterceptors(axios.create({
    baseURL: `${API_BASE_URL}/api/policies`,
    headers: {
        'Content-Type': 'application/json',
    },
}));


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

export const fetchMyScrapIds = async () => {
    const response = await policyApi.get('/scraps/me/ids');
    return response.data;
};

export const fetchMyScraps = async ({ query = '', page = 0, size = 5 } = {}) => {
    const response = await policyApi.get('/scraps/me', {
        params: {
            query: query || undefined,
            page,
            size,
        },
    });
    return response.data;
};

export const addPolicyScrap = async (serviceId) => {
    const response = await policyApi.post(`/${serviceId}/scraps`);
    return response.data;
};

export const migrateLegacyScraps = async () => {
    let legacyIds = [];

    try {
        legacyIds = JSON.parse(localStorage.getItem('likedPolicyIds') || '[]');
    } catch {
        localStorage.removeItem('likedPolicyIds');
        return;
    }

    const uniqueIds = [...new Set(legacyIds)].filter(Boolean);
    if (uniqueIds.length === 0) {
        localStorage.removeItem('likedPolicyIds');
        return;
    }

    const results = await Promise.allSettled(uniqueIds.map((serviceId) => addPolicyScrap(serviceId)));
    const failedIds = uniqueIds.filter((_, index) => results[index].status === 'rejected');

    if (failedIds.length === 0) {
        localStorage.removeItem('likedPolicyIds');
        return;
    }

    localStorage.setItem('likedPolicyIds', JSON.stringify(failedIds));
};

export const removePolicyScrap = async (serviceId) => {
    const response = await policyApi.delete(`/${serviceId}/scraps`);
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
