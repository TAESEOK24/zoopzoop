import React, { useState, useEffect, useRef } from 'react';
import { Bot, User, ChevronDown, ChevronUp, Send } from 'lucide-react';
import { searchPolicies, searchPolicyDetail } from '../../api/policies';

const PolicyCard = ({ item }) => {
    const [detail, setDetail] = useState(null);
    const [expanded, setExpanded] = useState(false);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [errorMsg, setErrorMsg] = useState(null);

    const handleDetailClick = async () => {
        if (expanded) {
            setExpanded(false);
            return;
        }
        if (detail) {
            setExpanded(true);
            return;
        }

        setLoadingDetail(true);
        setErrorMsg(null);
        try {
            const res = await searchPolicyDetail(item.serviceId);
            if (res && res.resultCode === 'S-1' && res.data) {
                setDetail(res.data);
                setExpanded(true);
            } else {
                setErrorMsg('상세 정보를 불러올 수 없습니다.');
            }
        } catch (e) {
            console.error(e);
            setErrorMsg('상세 정보를 불러오는 중 오류가 발생했습니다.');
        } finally {
            setLoadingDetail(false);
        }
    };

    return (
        <div className="bg-white border border-gray-200 rounded-lg p-4 mb-3 shadow-sm text-sm w-full">
            <h4 className="font-bold text-blue-700 mb-1 leading-snug">{item.serviceName}</h4>
            <p className="text-gray-700 mb-3 text-xs leading-relaxed line-clamp-2">{item.purposeSummary}</p>
            
            <div className="flex flex-col gap-2">
                <div className="text-gray-500 text-xs flex justify-between items-center">
                    <span className="font-medium bg-gray-100 px-2 py-1 rounded text-[10px]">
                        {item.orgName} {item.departmentName ? `> ${item.departmentName}` : ''}
                    </span>
                    <button 
                        onClick={handleDetailClick}
                        className="flex items-center text-blue-600 hover:text-blue-800 focus:outline-none transition-colors border border-blue-200 hover:border-blue-400 px-2 py-1 rounded"
                        aria-label="상세보기"
                        aria-expanded={expanded}
                    >
                        {loadingDetail ? '로딩중...' : expanded ? <><ChevronUp size={14} className="mr-1"/>접기</> : <><ChevronDown size={14} className="mr-1"/>상세보기</>}
                    </button>
                </div>
                {errorMsg && <p className="text-red-500 text-xs mt-1">{errorMsg}</p>}
                
                {expanded && detail && (
                    <div className="mt-3 pt-3 border-t border-gray-100 text-gray-700 text-xs space-y-2 bg-gray-50 p-3 rounded-md">
                        {detail.target && <p><strong>지원대상:</strong> {detail.target}</p>}
                        {detail.supportContent && <p><strong>지원내용:</strong> {detail.supportContent}</p>}
                        {detail.applicationMethod && <p><strong>신청방법:</strong> {detail.applicationMethod}</p>}
                        {detail.applicationDeadline && <p><strong>신청기한:</strong> {detail.applicationDeadline}</p>}
                        {detail.contactInfo && <p><strong>문의처:</strong> {detail.contactInfo} {detail.contactNumber ? `(${detail.contactNumber})` : ''}</p>}
                        {detail.detailUrl && (
                            <a href={detail.detailUrl} target="_blank" rel="noreferrer" className="text-blue-600 underline inline-block mt-2 font-medium hover:text-blue-800">
                                공식 홈페이지 상세 링크
                            </a>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

const ChatMessage = ({ msg }) => {
    const isBot = msg.sender === 'bot';
    
    return (
        <div className={`flex w-full mb-6 ${isBot ? 'justify-start' : 'justify-end'}`}>
            {isBot && (
                <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center mr-3 flex-shrink-0 shadow-sm border border-blue-200">
                    <Bot size={20} className="text-blue-600" />
                </div>
            )}
            
            <div className={`flex flex-col max-w-[85%] md:max-w-[75%]`}>
                {!isBot && <span className="text-xs text-gray-500 mb-1 ml-auto mr-1">나</span>}
                {isBot && <span className="text-xs text-blue-600 mb-1 ml-1 font-medium">정책 지킴이 AI</span>}
                
                <div className={`rounded-2xl px-4 py-3 shadow-sm ${
                    msg.isTyping ? 'bg-gray-100 text-gray-500 rounded-tl-none italic' :
                    isBot ? 'bg-white border border-gray-200 rounded-tl-none' : 'bg-blue-600 text-white rounded-tr-none'
                }`}>
                    {msg.type === 'text' && (
                        <p className="whitespace-pre-wrap text-sm leading-relaxed">{msg.text}</p>
                    )}
                    
                    {msg.type === 'policy-list' && (
                        <div className="w-full">
                            <p className="mb-4 text-sm font-semibold text-gray-800 border-b pb-2">📋 추천 정책 목록</p>
                            {msg.data && msg.data.length > 0 ? (
                                <div className="space-y-3">
                                    {msg.data.map(item => (
                                        <PolicyCard key={item.serviceId} item={item} />
                                    ))}
                                </div>
                            ) : (
                                <p className="text-sm text-gray-600">해당 키워드에 대한 정책을 찾을 수 없습니다.</p>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

const AIChatPage = () => {
    const [messages, setMessages] = useState([
        { 
            id: 'welcome', 
            sender: 'bot', 
            type: 'text', 
            text: '안녕하세요! 정책 검색 AI 챗봇입니다.\n\n궁금하신 정책 키워드를 채팅창에 입력해보세요.\n입력하시는 동안 실시간으로 관련 정책을 추천해드립니다. (예: 청년, 지원금, 일자리 등)' 
        }
    ]);
    const [inputValue, setInputValue] = useState('');
    const [previewList, setPreviewList] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState(null);
    
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, previewList, isLoading, errorMsg]);

    // Live search debounce
    useEffect(() => {
        const fetchPreview = async () => {
            const trimmed = inputValue.trim();
            if (trimmed.length < 2) {
                setPreviewList(null);
                setErrorMsg(null);
                setIsLoading(false);
                return;
            }

            setIsLoading(true);
            setErrorMsg(null);
            
            try {
                const res = await searchPolicies(trimmed, 5);
                if (res && res.resultCode === 'S-1' && res.data) {
                    setPreviewList(res.data);
                } else {
                    setPreviewList([]);
                }
            } catch (error) {
                console.error(error);
                setErrorMsg('정책 정보를 검색하는 중 오류가 발생했습니다.');
                setPreviewList(null);
            } finally {
                setIsLoading(false);
            }
        };

        const timer = setTimeout(() => {
            fetchPreview();
        }, 500);

        return () => clearTimeout(timer);
    }, [inputValue]);

    const handleSubmit = (e) => {
        e.preventDefault();
        const trimmed = inputValue.trim();
        if (!trimmed) return;

        // Apply current state to chat history
        const newUserMsg = { id: Date.now().toString(), sender: 'user', type: 'text', text: trimmed };
        
        let newBotMsg = null;
        if (previewList && previewList.length > 0) {
            newBotMsg = { id: (Date.now() + 1).toString(), sender: 'bot', type: 'policy-list', data: previewList };
        } else if (previewList && previewList.length === 0) {
            newBotMsg = { id: (Date.now() + 1).toString(), sender: 'bot', type: 'text', text: `'${trimmed}'에 대한 검색 결과가 없습니다.` };
        } else if (errorMsg) {
            newBotMsg = { id: (Date.now() + 1).toString(), sender: 'bot', type: 'text', text: errorMsg };
        } else if (trimmed.length < 2) {
            newBotMsg = { id: (Date.now() + 1).toString(), sender: 'bot', type: 'text', text: '검색어는 최소 2글자 이상 입력해주세요.' };
        }

        setMessages(prev => {
            const next = [...prev, newUserMsg];
            if (newBotMsg) next.push(newBotMsg);
            return next;
        });

        setInputValue('');
        setPreviewList(null);
        setErrorMsg(null);
    };

    return (
        <div className="flex flex-col min-h-[calc(100vh-140px)] bg-gray-50 pt-20 pb-4 px-4 md:px-0">
            <div className="max-w-3xl w-full mx-auto flex flex-col flex-1 bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden">
                {/* Header */}
                <div className="bg-blue-600 text-white p-4 text-center shrink-0 shadow-md z-10">
                    <h2 className="text-lg font-bold flex items-center justify-center">
                        <Bot className="mr-2" /> AI 정책 비서
                    </h2>
                    <p className="text-xs text-blue-100 mt-1">나에게 딱 맞는 정책을 쉽고 빠르게 찾아보세요</p>
                </div>

                {/* Chat Message List */}
                <div className="flex-1 overflow-y-auto p-4 md:p-6 bg-slate-50/50">
                    {messages.map(msg => (
                        <ChatMessage key={msg.id} msg={msg} />
                    ))}
                    
                    {/* Live Preview section based on user input */}
                    {isLoading && (
                        <ChatMessage msg={{ sender: 'bot', type: 'text', text: '정책을 검색하고 있습니다...', isTyping: true }} />
                    )}
                    {!isLoading && errorMsg && (
                        <ChatMessage msg={{ sender: 'bot', type: 'text', text: errorMsg }} />
                    )}
                    {!isLoading && inputValue.trim().length >= 2 && previewList !== null && (
                        <ChatMessage msg={{ 
                            sender: 'bot', 
                            type: previewList.length > 0 ? 'policy-list' : 'text', 
                            text: previewList.length === 0 ? `'${inputValue.trim()}'에 대한 검색 결과가 없습니다.` : undefined,
                            data: previewList 
                        }} />
                    )}
                    
                    <div ref={messagesEndRef} />
                </div>

                {/* Input Area */}
                <form 
                    onSubmit={handleSubmit}
                    className="p-4 bg-white border-t border-gray-200 shrink-0"
                >
                    <div className="relative flex items-center">
                        <input
                            type="text"
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
                            placeholder="찾고 싶은 정책의 키워드를 입력하세요... (최소 2글자)"
                            className="flex-1 border border-gray-300 rounded-full pl-5 pr-14 py-3 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-shadow transition-colors bg-gray-50 focus:bg-white"
                            aria-label="정책 키워드 입력"
                        />
                        <button
                            type="submit"
                            disabled={!inputValue.trim()}
                            className="absolute right-2 top-1/2 -translate-y-1/2 w-10 h-10 flex items-center justify-center rounded-full bg-blue-600 text-white disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors hover:bg-blue-700"
                            aria-label="메시지 전송"
                        >
                            <Send size={18} className="translate-x-[1px] translate-y-[1px]" />
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default AIChatPage;
