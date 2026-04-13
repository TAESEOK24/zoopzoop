import axios from 'axios';
import { API_BASE_URL } from './index';

// 인증 관련 API 요청을 처리하는 인스턴스
const authApi = axios.create({
    // 🚀 [핵심 수정] baseURL에 /api 를 명시적으로 추가했습니다.
    baseURL: `${API_BASE_URL}/api/auth`,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 로그인 요청
export const loginAPI = async (email, password) => {
    try {
        console.log(`[API Request] POST ${authApi.defaults.baseURL}/login`, { email, password });
        const response = await authApi.post('/login', { email, password });
        console.log(`[API Response] POST /login 성공:`, response.data);
        return response.data;
    } catch (error) {
        console.error(`[API Error] POST /login 에러:`, error);
        throw error;
    }
};

// 회원가입 요청
export const signupAPI = async (email, password, name) => {
    try {
        console.log(`[API Request] POST ${authApi.defaults.baseURL}/signup`, { email, name });
        const response = await authApi.post('/signup', { email, password, name });
        console.log(`[API Response] POST /signup 성공:`, response.data);
        return response.data;
    } catch (error) {
        console.error(`[API Error] POST /signup 에러:`, error);
        throw error;
    }
};