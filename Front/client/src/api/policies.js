import axios from 'axios';
import { API_BASE_URL } from './index';

const policyApi = axios.create({
    baseURL: 'http://localhost:8080/api/policies', // 🚀 8080번 백엔드로 가라고 못 박아줍니다!
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

// 챗봇용 정책 검색
export const searchPolicies = async (keyword, size = 5) => {
    const response = await policyApi.get('/search', {
        params: { keyword, size }
    });
    return response.data;
};

// 챗봇용 정책 상세 검색 (serviceId 기반)
export const searchPolicyDetail = async (serviceId) => {
    const response = await policyApi.get(`/search/${serviceId}`);
    return response.data;
};