import React, { useEffect, useState } from 'react';
import { Bot, Clock, RotateCcw } from 'lucide-react';
import ChatMessageList from '../../components/Chatbot/ChatMessageList';
import ChatInput from '../../components/Chatbot/ChatInput';
import ChatHistorySidebar from '../../components/Chatbot/ChatHistorySidebar';
import { askChatbot } from '../../api/chatbot';
import { getUserName, isAuthenticated } from '../../api/authSession';

const RESPONSE_TYPES = {
    POLICY_SEARCH: 'POLICY_SEARCH',
    CLARIFICATION_NEEDED: 'CLARIFICATION_NEEDED',
    SMALLTALK: 'SMALLTALK',
    OFF_TOPIC: 'OFF_TOPIC',
    SAFETY: 'SAFETY'
};

const LEGACY_CHAT_HISTORY_KEY = 'chat_sessions';

const getChatHistoryStorageKey = (userName) => (
    userName ? `chat_sessions:${encodeURIComponent(userName)}` : null
);



const getInitialMessage = () => ({
    id: 'welcome',
    sender: 'bot',
    answer: '안녕하세요. 복지 정책 상담형 안내 도우미입니다.\n\n현재 겪고 계신 어려움이나 궁금하신 지원 분야를 편하게 말씀해주세요. 예: 생활이 너무 힘들어, 청년 주거 지원 알려줘',
    responseType: RESPONSE_TYPES.SMALLTALK,
    suggestedReplies: [
        { label: '청년 주거 지원', value: '청년 주거 지원 알려줘' },
        { label: '취업 지원금', value: '취업 지원 정책 알려줘' },
        { label: '긴급 생계 지원', value: '생활이 너무 힘들어' }
    ],
    timestamp: new Date().toISOString()
});



const AIChatPage = () => {
    const [messages, setMessages] = useState([getInitialMessage()]);
    const [sessionId, setSessionId] = useState(null);
    const [localSessionId, setLocalSessionId] = useState(() => Date.now().toString());
    const [isLoading, setIsLoading] = useState(false);
    const [chatSessions, setChatSessions] = useState([]);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(() => isAuthenticated());
    const [chatHistoryKey, setChatHistoryKey] = useState(() => (
        isAuthenticated() ? getChatHistoryStorageKey(getUserName()) : null
    ));

    useEffect(() => {
        const syncLoginState = () => {
            const nextIsLoggedIn = isAuthenticated();
            setIsLoggedIn(nextIsLoggedIn);
            setChatHistoryKey(nextIsLoggedIn ? getChatHistoryStorageKey(getUserName()) : null);

            if (!nextIsLoggedIn) {
                setMessages([getInitialMessage()]);
                setSessionId(null);
                setLocalSessionId(Date.now().toString());
                setChatSessions([]);
                setIsSidebarOpen(false);
            }
        };

        localStorage.removeItem(LEGACY_CHAT_HISTORY_KEY);
        window.addEventListener('loginStateChange', syncLoginState);
        window.addEventListener('storage', syncLoginState);

        return () => {
            window.removeEventListener('loginStateChange', syncLoginState);
            window.removeEventListener('storage', syncLoginState);
        };
    }, []);

    useEffect(() => {
        localStorage.removeItem(LEGACY_CHAT_HISTORY_KEY);

        if (!isLoggedIn || !chatHistoryKey) {
            setChatSessions([]);
            setIsSidebarOpen(false);
            return;
        }

        const stored = localStorage.getItem(chatHistoryKey);
        if (!stored) {
            setChatSessions([]);
            return;
        }

        try {
            const parsedSessions = JSON.parse(stored);
            setChatSessions(Array.isArray(parsedSessions) ? parsedSessions : []);
        } catch (error) {
            console.error('Failed to parse chat sessions', error);
            setChatSessions([]);
        }
    }, [chatHistoryKey, isLoggedIn]);

    useEffect(() => {
        if (!isLoggedIn || !chatHistoryKey || messages.length <= 1) {
            return;
        }

        setChatSessions((previousSessions) => {
            const existingIndex = previousSessions.findIndex((session) => session.id === localSessionId);
            const persistedMessages = messages.filter((message) => !message.isTyping);
            const userMessages = persistedMessages.filter((message) => message.sender === 'user');
            if (userMessages.length === 0) {
                return previousSessions;
            }
            const title = userMessages.length > 0 ? userMessages[0].text : '새로운 대화';

            const nextSession = {
                id: localSessionId,
                backendSessionId: sessionId,
                title,
                timestamp: new Date().toISOString(),
                messages: persistedMessages
            };

            let updatedSessions;
            if (existingIndex >= 0) {
                updatedSessions = [...previousSessions];
                updatedSessions[existingIndex] = nextSession;
            } else {
                updatedSessions = [nextSession, ...previousSessions];
            }

            localStorage.setItem(chatHistoryKey, JSON.stringify(updatedSessions));
            return updatedSessions;
        });
    }, [chatHistoryKey, isLoggedIn, localSessionId, messages, sessionId]);

    const handleReset = () => {
        const shouldReset = window.confirm('새로운 대화를 시작할까요? 이전 대화는 히스토리에 저장됩니다.');
        if (!shouldReset) {
            return;
        }

        setMessages([getInitialMessage()]);
        setSessionId(null);
        setLocalSessionId(Date.now().toString());
        setIsSidebarOpen(false);
    };

    const handleSelectSession = (session) => {
        if (!isLoggedIn) {
            return;
        }

        setMessages(session.messages?.length ? session.messages : [getInitialMessage()]);
        setSessionId(session.backendSessionId);
        setLocalSessionId(session.id);
        setIsSidebarOpen(false);
    };

    const handleSend = async (text) => {
        if (!text.trim() || isLoading) {
            return;
        }

        const userMessageId = Date.now().toString();
        setMessages((previous) => [
            ...previous,
            {
                id: userMessageId,
                sender: 'user',
                text,
                timestamp: new Date().toISOString()
            }
        ]);

        const typingMessageId = (Date.now() + 1).toString();
        setMessages((previous) => [...previous, { id: typingMessageId, sender: 'bot', isTyping: true }]);
        setIsLoading(true);

        try {
            const response = await askChatbot({ message: text, sessionId });
            setMessages((previous) => previous.filter((message) => message.id !== typingMessageId));

            if (response?.resultCode !== 'S-1' || !response.data) {
                throw new Error(response?.message || 'Invalid response');
            }

            const {
                answer,
                matchedPolicyCount,
                policies,
                references,
                responseType,
                suggestedReplies
            } = response.data;

            if (response.data.sessionId) {
                setSessionId(response.data.sessionId);
            }

            const botMessageId = (Date.now() + 2).toString();
            setMessages((previous) => [
                ...previous,
                {
                    id: botMessageId,
                    sender: 'bot',
                    answer,
                    responseType: responseType || RESPONSE_TYPES.POLICY_SEARCH,
                    suggestedReplies: suggestedReplies || [],
                    policies: policies || [],
                    references: references || [],
                    matchedPolicyCount: matchedPolicyCount ?? 0,
                    timestamp: new Date().toISOString()
                }
            ]);
        } catch (error) {
            console.error('Chat API error:', error);
            setMessages((previous) => previous.filter((message) => message.id !== typingMessageId));

            const errorMessageId = (Date.now() + 2).toString();
            setMessages((previous) => [
                ...previous,
                {
                    id: errorMessageId,
                    sender: 'system',
                    text: '서버와 연결하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
                    timestamp: new Date().toISOString()
                }
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="relative flex min-h-[calc(100vh-140px)] flex-col overflow-hidden bg-gray-50 px-4 pb-4 pt-20 md:px-0">
            <div className="relative mx-auto flex w-full max-w-3xl flex-1 flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-lg">
                <div className="relative z-10 flex shrink-0 items-center justify-between bg-blue-600 p-4 text-center text-white shadow-md">
                    <div className="w-16">
                        {sessionId && (
                            <span className="whitespace-nowrap rounded-full bg-blue-700 px-2 py-1 text-[10px] opacity-80">
                                이전 질문 기준 추천
                            </span>
                        )}
                    </div>

                    <div className="flex flex-col items-center">
                        <h2 className="flex items-center justify-center text-lg font-bold">
                            <Bot className="mr-2" />
                            복지 정책 안내 챗봇
                        </h2>
                        <p className="mt-1 text-xs text-blue-100">상황에 맞는 복지 정책을 찾아드려요.</p>
                    </div>

                    <div className="flex w-16 justify-end gap-1">
                        <button
                            onClick={handleReset}
                            className="rounded p-2 text-white transition-colors hover:bg-blue-700"
                            title="새 대화 시작"
                            aria-label="새 대화 시작"
                        >
                            <RotateCcw size={18} />
                        </button>
                        {isLoggedIn && (
                            <button
                                onClick={() => setIsSidebarOpen(true)}
                                className="rounded p-2 text-white transition-colors hover:bg-blue-700"
                                title="대화 히스토리"
                                aria-label="대화 히스토리"
                            >
                                <Clock size={18} />
                            </button>
                        )}
                    </div>
                </div>

                <ChatMessageList messages={messages} onChipClick={handleSend} />
                <ChatInput onSend={handleSend} disabled={isLoading} />
            </div>

            {isLoggedIn && (
                <ChatHistorySidebar
                    isOpen={isSidebarOpen}
                    onClose={() => setIsSidebarOpen(false)}
                    sessions={chatSessions}
                    onSelectSession={handleSelectSession}
                />
            )}
        </div>
    );
};

export default AIChatPage;
