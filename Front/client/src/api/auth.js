import axios from 'axios';
import { API_BASE_URL } from './index';

// 인증 관련 API 요청을 처리하는 인스턴스
const authApi = axios.create({
    baseURL: `${API_BASE_URL}/auth`,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 로그인 요청
export const loginAPI = async (email, password) => {
    try {
        const response = await authApi.post('/login', { email, password });
        return response.data;
    } catch (error) {
        throw error;
    }
};

// 회원가입 요청
export const signupAPI = async (email, password, name) => {
    try {
        const response = await authApi.post('/signup', { email, password, name });
        return response.data;
    } catch (error) {
        throw error;
    }
};
