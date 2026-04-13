import axios from 'axios';

// 기존에 정의된 변수들
export const API_BASE_URL = 'http://localhost:8080';
export const ENDPOINTS = {
    AUTH: '/api/auth',
    POLICIES: '/api/policies',
    COMMUNITY: '/api/community',
};

// --- 방법 1: 공통 인스턴스 생성 및 default 내보내기 추가 ---
const instance = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// (선택 사항) 토큰이 있다면 요청 헤더에 자동으로 포함시키는 인터셉터
instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token'); // 프로젝트의 토큰 저장 방식에 맞게 수정
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default instance; // 이 줄이 있어야 'import axios from ...' 형태의 사용이 가능합니다.