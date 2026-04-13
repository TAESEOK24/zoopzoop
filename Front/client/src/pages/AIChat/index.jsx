import React, { useState, useEffect } from 'react';
import { Bot, RotateCcw, Clock } from 'lucide-react';
import ChatMessageList from '../../components/Chatbot/ChatMessageList';
import ChatInput from '../../components/Chatbot/ChatInput';
import ChatHistorySidebar from '../../components/Chatbot/ChatHistorySidebar';
import { askChatbot } from '../../api/chatbot';

const getInitialMessage = () => ({ 
    id: 'welcome', 
    sender: 'bot', 
    answer: '안녕하세요! 정책 검색 AI 챗봇입니다.\n\n궁금하신 정책 키워드를 채팅창에 입력해보세요.\n(예: 청년 주거 지원 정책 알려줘, 장학금 지원 등)',
    timestamp: new Date().toISOString()
});

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
            const res = await askChatbot({ message: text, sessionId });

            setMessages(prev => prev.filter(m => m.id !== tempBotMsgId));

            if (res && res.resultCode === 'S-1' && res.data) {
                const { answer, policies, references, matchedPolicyCount } = res.data;
                const newSessionId = res.data.sessionId;

                if (newSessionId) {
                    setSessionId(newSessionId);
                }

                const botMsgId = (Date.now() + 2).toString();
                
                setMessages(prev => [...prev, {
                    id: botMsgId,
                    sender: 'bot',
                    answer: answer,
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
                    <div className="w-16"></div> {/* Spacer for center alignment */}
                    <div className="flex flex-col items-center">
                        <h2 className="text-lg font-bold flex items-center justify-center">
                            <Bot className="mr-2" /> AI 정책 비서
                        </h2>
                        <p className="text-xs text-blue-100 mt-1">나에게 딱 맞는 정책을 쉽고 빠르게 찾아보세요</p>
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
                <ChatMessageList messages={messages} />

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
