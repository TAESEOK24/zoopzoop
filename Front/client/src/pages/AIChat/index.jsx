import React, { useState } from 'react';
import { Bot, RotateCcw } from 'lucide-react';
import ChatMessageList from '../../components/Chatbot/ChatMessageList';
import ChatInput from '../../components/Chatbot/ChatInput';
import { askChatbot } from '../../api/chatbot';

const INITIAL_MESSAGE = { 
    id: 'welcome', 
    sender: 'bot', 
    answer: '안녕하세요! 정책 검색 AI 챗봇입니다.\n\n궁금하신 정책 키워드를 채팅창에 입력해보세요.\n(예: 청년 주거 지원 정책 알려줘, 장학금 지원 등)' 
};

const AIChatPage = () => {
    const [messages, setMessages] = useState([INITIAL_MESSAGE]);
    const [sessionId, setSessionId] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleReset = () => {
        if (window.confirm('새로운 대화를 시작하시겠습니까?')) {
            setMessages([INITIAL_MESSAGE]);
            setSessionId(null);
        }
    };

    const handleSend = async (text) => {
        if (!text.trim() || isLoading) return;

        // 1. Add User Message
        const userMsgId = Date.now().toString();
        setMessages(prev => [...prev, { id: userMsgId, sender: 'user', text }]);
        
        // 2. Add Loading Message
        const tempBotMsgId = (Date.now() + 1).toString();
        setMessages(prev => [...prev, { id: tempBotMsgId, sender: 'bot', isTyping: true }]);
        setIsLoading(true);

        try {
            const res = await askChatbot({ message: text, sessionId });

            // 3. Remove Loading Message
            setMessages(prev => prev.filter(m => m.id !== tempBotMsgId));

            if (res && res.resultCode === 'S-1' && res.data) {
                const { answer, policies, references, matchedPolicyCount } = res.data;
                const newSessionId = res.data.sessionId;

                // 세션 아이디 저장 (다음 요청부터 사용)
                if (newSessionId) {
                    setSessionId(newSessionId);
                }

                const botMsgId = (Date.now() + 2).toString();
                
                // 프론트 렌더링 규칙에 맞춰 그대로 상태에 저장.
                // data.answer, policies, references 등을 분리 유지합니다.
                setMessages(prev => [...prev, {
                    id: botMsgId,
                    sender: 'bot',
                    answer: answer, // answer는 무조건 표시
                    policies: policies || [], // policies가 비어있어도 answer를 덮어쓰지 않음
                    references: references || [],
                    matchedPolicyCount: matchedPolicyCount !== undefined ? matchedPolicyCount : 0
                }]);
            } else {
                throw new Error(res?.message || 'Invalid Response');
            }
        } catch (error) {
            console.error('Chat API Error:', error);
            setMessages(prev => prev.filter(m => m.id !== tempBotMsgId));
            
            const errorMsgId = (Date.now() + 2).toString();
            setMessages(prev => [...prev, {
                id: errorMsgId,
                sender: 'system',
                text: '서버와 연결하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col min-h-[calc(100vh-140px)] bg-gray-50 pt-20 pb-4 px-4 md:px-0">
            <div className="max-w-3xl w-full mx-auto flex flex-col flex-1 bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden">
                {/* Header */}
                <div className="bg-blue-600 text-white p-4 text-center shrink-0 shadow-md z-10 relative flex items-center justify-center">
                    <div className="flex flex-col items-center">
                        <h2 className="text-lg font-bold flex items-center justify-center">
                            <Bot className="mr-2" /> AI 정책 비서
                        </h2>
                        <p className="text-xs text-blue-100 mt-1">나에게 딱 맞는 정책을 쉽고 빠르게 찾아보세요</p>
                    </div>
                    {/* Reset Button */}
                    <button 
                        onClick={handleReset}
                        className="absolute right-4 top-1/2 -translate-y-1/2 p-2 hover:bg-blue-700 rounded transition-colors text-white"
                        title="새 대화 시작"
                        aria-label="새 대화 시작"
                    >
                        <RotateCcw size={20} />
                    </button>
                </div>

                {/* Chat Message List */}
                <ChatMessageList messages={messages} />

                {/* Chat Input */}
                <ChatInput onSend={handleSend} disabled={isLoading} />
            </div>
        </div>
    );
};

export default AIChatPage;
