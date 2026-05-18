import axios from 'axios';
import { API_BASE_URL, attachAuthInterceptors } from './index';

const chatbotApi = attachAuthInterceptors(axios.create({
    baseURL: `${API_BASE_URL}/api/chatbot`,
    headers: {
        'Content-Type': 'application/json',
    },
}));


/**
 * @typedef {Object} AskChatbotParams
 * @property {string} message - 사용자 질문 메시지
 * @property {string|null} [sessionId] - 세션 아이디 (이전 대화가 있으면 포함)
 */

/**
 * 챗봇에게 질문을 전송하고 응답을 받습니다.
 * @param {AskChatbotParams} params
 */
export const askChatbot = async ({ message, sessionId }) => {
    const payload = { message };
    if (sessionId) {
        payload.sessionId = sessionId;
    }
    
    const response = await chatbotApi.post('/ask', payload);
    return response.data;
};
