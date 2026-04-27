import React, { useState, useEffect } from 'react';
import { Bot, RotateCcw, Clock } from 'lucide-react';
import ChatMessageList from '../../components/Chatbot/ChatMessageList';
import ChatInput from '../../components/Chatbot/ChatInput';
import ChatHistorySidebar from '../../components/Chatbot/ChatHistorySidebar';
import { askChatbot } from '../../api/chatbot';

const getInitialMessage = () => ({ 
    id: 'welcome', 
    sender: 'bot', 
    answer: '안녕하세요! 복지 정책 상담형 안내 도우미입니다.\n\n현재 겪고 계신 어려움이나 궁금하신 지원 분야를 편하게 말씀해주세요. (예: 생활이 너무 힘들어, 청년 주거 지원 등)',
    responseType: 'SMALLTALK',
    suggestedReplies: [
        { label: '청년 주거 지원', value: '청년 주거 지원 알려줘' },
        { label: '취업 지원금', value: '취업 지원금 알려줘' },
        { label: '긴급 생계 지원', value: '생활이 너무 힘들어' }
    ],
    timestamp: new Date().toISOString()
});

// Mock API for demonstration
const mockAskChatbot = async (text, sessionId) => {
    return new Promise(resolve => {
        setTimeout(() => {
            const data = {
                sessionId: sessionId || 'mock-session-123',
                answer: '',
                responseType: 'SMALLTALK',
                suggestedReplies: [],
                policies: [],
                references: [],
                matchedPolicyCount: 0
            };

            if (text.includes('죽고') || text.includes('자살') || text.includes('포기')) {
                data.responseType = 'SAFETY';
                data.answer = '';
                data.suggestedReplies = [
                    { label: '긴급복지 문의', value: '긴급복지 문의' },
                    { label: '생계 지원 문의', value: '생계 지원 문의' }
                ];
            } else if (text.includes('주식') || text.includes('투자') || text.includes('코인')) {
                data.responseType = 'OFF_TOPIC';
                data.answer = '저는 복지 정책 및 공공 서비스 안내를 도와드리는 도우미입니다. 원하시는 지원 분야를 선택하시거나 말씀해주세요.';
                data.suggestedReplies = [
                    { label: '청년 지원', value: '청년 지원 알려줘' },
                    { label: '주거 지원', value: '주거 지원 알려줘' },
                    { label: '취업 지원', value: '취업 지원 알려줘' }
                ];
            } else if (text.includes('가난') || text.includes('힘들어') || text.includes('월세')) {
                data.responseType = 'CLARIFICATION_NEEDED';
                data.answer = '생활이 많이 부담되실 수 있겠어요.\n맞는 지원 정책을 찾으려면 현재 연령대를 알려주세요.';
                data.suggestedReplies = [
                    { label: '청년', value: '청년' },
                    { label: '중장년', value: '중장년' },
                    { label: '노년', value: '노년' }
                ];
            } else if (text.includes('청년') || text.includes('주거')) {
                data.responseType = 'POLICY_SEARCH';
                data.answer = '청년 주거비 부담을 덜 수 있는 정책들을 찾았어요.\n신청 조건과 지원 내용을 함께 확인해보세요.';
                data.matchedPolicyCount = 3;
                data.policies = [
                    {
                        serviceId: 'p1',
                        serviceName: '청년 월세 특별지원',
                        purposeSummary: '청년층의 주거비 부담 경감',
                        target: '만 19세~34세 독립거주 무주택 청년',
                        supportContent: '월 최대 20만원씩 12개월 분할 지급',
                        applicationMethod: '복지로 온라인 신청 또는 관할 주민센터 방문',
                        applicationDeadline: '2024.12.31 까지',
                        detailUrl: 'https://www.bokjiro.go.kr',
                        orgName: '국토교통부',
                        departmentName: '주거복지지원과',
                        reason: '입력하신 "청년", "월세" 키워드에 적합한 주거 지원 정책입니다.'
                    },
                    {
                        serviceId: 'p2',
                        serviceName: '청년 전세자금 대출 이자 지원',
                        purposeSummary: '무주택 청년 전세자금 대출 이자 부담 완화',
                        target: '만 19세~39세 무주택 청년 (소득기준 충족자)',
                        supportContent: '전세보증금 대출 이자 연 최대 2% 지원',
                        applicationMethod: '관할 지자체 홈페이지 또는 방문 신청',
                        applicationDeadline: '상시',
                        detailUrl: 'https://www.bokjiro.go.kr',
                        orgName: '지방자치단체',
                        departmentName: '청년정책과',
                        reason: '"청년 주거" 관련하여 이자 부담을 줄일 수 있는 대안입니다.'
                    }
                ];
                data.suggestedReplies = [
                    { label: '청년 대상만 보기', value: '청년 대상만 보기' },
                    { label: '주거 지원만 다시 보기', value: '주거 지원만 다시 보기' },
                    { label: '신청 조건 더 보기', value: '신청 조건 더 보기' }
                ];
            } else {
                data.responseType = 'SMALLTALK';
                data.answer = '안녕하세요. 무엇을 도와드릴까요? 편하게 말씀해주시거나 아래 칩을 선택해주세요.';
                data.suggestedReplies = [
                    { label: '청년 주거 지원', value: '청년 주거 지원 알려줘' },
                    { label: '생활비 지원', value: '생활이 너무 힘들어' }
                ];
            }

            resolve({ resultCode: 'S-1', data });
        }, 1000);
    });
};

const AIChatPage = () => {
    const [messages, setMessages] = useState([getInitialMessage()]);
    const [sessionId, setSessionId] = useState(null);
    const [localSessionId, setLocalSessionId] = useState(() => Date.now().toString());
    const [isLoading, setIsLoading] = useState(false);
    
    const [chatSessions, setChatSessions] = useState([]);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    // Load history on mount
    useEffect(() => {
        const stored = localStorage.getItem('chat_sessions');
        if (stored) {
            try {
                setChatSessions(JSON.parse(stored));
            } catch (e) {
                console.error('Failed to parse chat sessions', e);
            }
        }
    }, []);

    // Save session when messages update
    useEffect(() => {
        if (messages.length <= 1) return; // don't save just the welcome message
        
        setChatSessions(prev => {
            const existingIdx = prev.findIndex(s => s.id === localSessionId);
            const userMessages = messages.filter(m => m.sender === 'user');
            const title = userMessages.length > 0 ? userMessages[0].text : '새로운 대화';
            
            const newSession = {
                id: localSessionId,
                backendSessionId: sessionId,
                title,
                timestamp: new Date().toISOString(),
                messages
            };

            let updated;
            if (existingIdx >= 0) {
                updated = [...prev];
                updated[existingIdx] = newSession;
            } else {
                updated = [newSession, ...prev];
            }
            
            localStorage.setItem('chat_sessions', JSON.stringify(updated));
            return updated;
        });
    }, [messages, sessionId, localSessionId]);

    const handleReset = () => {
        if (window.confirm('새로운 대화를 시작하시겠습니까? (이전 대화는 히스토리로 보존됩니다)')) {
            setMessages([getInitialMessage()]);
            setSessionId(null);
            setLocalSessionId(Date.now().toString());
            setIsSidebarOpen(false);
        }
    };

    const handleSelectSession = (session) => {
        setMessages(session.messages);
        setSessionId(session.backendSessionId);
        setLocalSessionId(session.id);
        setIsSidebarOpen(false);
    };

    const handleSend = async (text) => {
        if (!text.trim() || isLoading) return;

        const userMsgId = Date.now().toString();
        setMessages(prev => [...prev, { 
            id: userMsgId, 
            sender: 'user', 
            text,
            timestamp: new Date().toISOString()
        }]);
        
        const tempBotMsgId = (Date.now() + 1).toString();
        setMessages(prev => [...prev, { id: tempBotMsgId, sender: 'bot', isTyping: true }]);
        setIsLoading(true);

        try {
            // Use mock API for demonstration instead of real askChatbot
            // const res = await askChatbot({ message: text, sessionId });
            const res = await mockAskChatbot(text, sessionId);

            setMessages(prev => prev.filter(m => m.id !== tempBotMsgId));

            if (res && res.resultCode === 'S-1' && res.data) {
                const { answer, policies, references, matchedPolicyCount, responseType, suggestedReplies } = res.data;
                const newSessionId = res.data.sessionId;

                if (newSessionId) {
                    setSessionId(newSessionId);
                }

                const botMsgId = (Date.now() + 2).toString();
                
                setMessages(prev => [...prev, {
                    id: botMsgId,
                    sender: 'bot',
                    answer: answer,
                    responseType: responseType || 'POLICY_SEARCH',
                    suggestedReplies: suggestedReplies || [],
                    policies: policies || [],
                    references: references || [],
                    matchedPolicyCount: matchedPolicyCount !== undefined ? matchedPolicyCount : 0,
                    timestamp: new Date().toISOString()
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
                text: '서버와 연결하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
                timestamp: new Date().toISOString()
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col min-h-[calc(100vh-140px)] bg-gray-50 pt-20 pb-4 px-4 md:px-0 relative overflow-hidden">
            <div className="max-w-3xl w-full mx-auto flex flex-col flex-1 bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden relative">
                {/* Header - Reverted to Blue Theme */}
                <div className="bg-blue-600 text-white p-4 text-center shrink-0 shadow-md z-10 relative flex items-center justify-between">
                    <div className="w-16">
                        {sessionId && (
                            <span className="text-[10px] bg-blue-700 px-2 py-1 rounded-full opacity-80 whitespace-nowrap">
                                이전 질문 기준 추천
                            </span>
                        )}
                    </div>
                    <div className="flex flex-col items-center">
                        <h2 className="text-lg font-bold flex items-center justify-center">
                            <Bot className="mr-2" /> 복지 정책 안내 챗봇
                        </h2>
                        <p className="text-xs text-blue-100 mt-1">상황에 맞는 복지 정책을 찾아드려요</p>
                    </div>
                    {/* Buttons */}
                    <div className="w-16 flex justify-end gap-1">
                        <button 
                            onClick={handleReset}
                            className="p-2 hover:bg-blue-700 rounded transition-colors text-white"
                            title="새 대화 시작"
                            aria-label="새 대화 시작"
                        >
                            <RotateCcw size={18} />
                        </button>
                        <button 
                            onClick={() => setIsSidebarOpen(true)}
                            className="p-2 hover:bg-blue-700 rounded transition-colors text-white"
                            title="대화 히스토리"
                            aria-label="대화 히스토리"
                        >
                            <Clock size={18} />
                        </button>
                    </div>
                </div>

                {/* Chat Message List */}
                <ChatMessageList messages={messages} onChipClick={handleSend} />

                {/* Chat Input */}
                <ChatInput onSend={handleSend} disabled={isLoading} />
            </div>

            {/* Sidebar Overlay - Moved to avoid blocking pointer events */}
            <ChatHistorySidebar 
                isOpen={isSidebarOpen} 
                onClose={() => setIsSidebarOpen(false)} 
                sessions={chatSessions}
                onSelectSession={handleSelectSession}
            />
        </div>
    );
};

export default AIChatPage;
