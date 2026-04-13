import React, { useState, useEffect } from 'react';
import { MessageCircle, X, RotateCcw, AlertCircle } from 'lucide-react';
import ChatMessageList from './ChatMessageList';
import ChatInput from './ChatInput';
import { askChatbot } from '../../api/chatbot';

const INITIAL_MESSAGE = {
    id: 'welcome',
    sender: 'bot',
    answer: '안녕하세요! 무엇을 도와드릴까요?\n궁금한 정책이나 지원사업을 물어보세요.'
};

const ChatbotWidget = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([INITIAL_MESSAGE]);
    const [sessionId, setSessionId] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    // Prevent body scroll when chatbot is open on mobile
    useEffect(() => {
        if (isOpen) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = '';
        }
        return () => {
            document.body.style.overflow = '';
        };
    }, [isOpen]);

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

                // 세션 아이디 저장
                if (newSessionId) {
                    setSessionId(newSessionId);
                }

                const botMsgId = (Date.now() + 2).toString();
                
                // 정책이 0건이어도 answer가 같이 옵니다. (matchedPolicyCount 상관없이 표시)
                setMessages(prev => [...prev, {
                    id: botMsgId,
                    sender: 'bot',
                    answer: answer,
                    policies: policies || [],
                    references: references || [],
                    matchedPolicyCount: matchedPolicyCount || 0
                }]);
            } else {
                // API 에러 응답 처리 방어코드
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
        <div className="fixed bottom-6 right-6 z-50">
            {/* Floating Button */}
            {!isOpen && (
                <button
                    onClick={() => setIsOpen(true)}
                    className="w-14 h-14 bg-blue-600 rounded-full shadow-lg flex items-center justify-center text-white hover:bg-blue-700 transition-transform transform hover:scale-105"
                    aria-label="챗봇 열기"
                >
                    <MessageCircle size={28} />
                </button>
            )}

            {/* Chat Panel */}
            {isOpen && (
                <div className="fixed inset-0 sm:inset-auto sm:bottom-6 sm:right-6 sm:w-[400px] sm:h-[650px] bg-white sm:rounded-2xl shadow-2xl flex flex-col overflow-hidden border border-gray-200 z-50 transition-all duration-300 transform scale-100 origin-bottom-right">
                    
                    {/* Header */}
                    <div className="bg-blue-600 text-white p-4 flex items-center justify-between shrink-0 shadow-sm z-10">
                        <div className="flex items-center space-x-2">
                            <MessageCircle size={20} />
                            <h2 className="text-base font-bold">AI 정책 비서</h2>
                        </div>
                        <div className="flex items-center space-x-2">
                            <button 
                                onClick={handleReset}
                                className="p-1 hover:bg-blue-700 rounded transition-colors"
                                title="새 대화 시작"
                                aria-label="새 대화 시작"
                            >
                                <RotateCcw size={18} />
                            </button>
                            <button 
                                onClick={() => setIsOpen(false)}
                                className="p-1 hover:bg-blue-700 rounded transition-colors"
                                aria-label="챗봇 닫기"
                            >
                                <X size={20} />
                            </button>
                        </div>
                    </div>

                    {/* Chat Messages */}
                    <ChatMessageList messages={messages} />

                    {/* Chat Input */}
                    <ChatInput onSend={handleSend} disabled={isLoading} />
                </div>
            )}
        </div>
    );
};

export default ChatbotWidget;
