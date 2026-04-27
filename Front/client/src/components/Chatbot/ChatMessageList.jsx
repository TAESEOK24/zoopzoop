import React, { useEffect, useRef } from 'react';
import { Bot, ExternalLink, Volume2, Copy } from 'lucide-react';
import PolicyCardList from './PolicyCardList';
import SuggestedReplyChips from './SuggestedReplyChips';
import SafetyNotice from './SafetyNotice';/**
 * @typedef {Object} Message
 * @property {string} id
 * @property {'user'|'bot'|'system'} sender
 * @property {string} [text] - User message or System message
 * @property {string} [answer] - Bot answer
 * @property {Array} [policies]
 * @property {Array} [references]
 * @property {boolean} [isTyping]
 * @property {number} [matchedPolicyCount]
 * @property {string} [responseType]
 * @property {Array} [suggestedReplies]
 * @property {string} [timestamp]
 */

/**
 * @param {{ messages: Message[], onChipClick: (value: string) => void }} props 
 */
const ChatMessageList = ({ messages, onChipClick }) => {
    const endRef = useRef(null);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleTTS = (text) => {
        if (!text) return;
        if ('speechSynthesis' in window) {
            window.speechSynthesis.cancel(); // Stop playing anything else
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'ko-KR';
            window.speechSynthesis.speak(utterance);
        } else {
            alert('이 브라우저에서는 음성 기능을 지원하지 않습니다.');
        }
    };

    const handleCopy = (text) => {
        if (!text) return;
        navigator.clipboard.writeText(text).then(() => {
            alert('클립보드에 복사되었습니다.');
        }).catch(err => {
            console.error('Clipboard error:', err);
            alert('복사에 실패했습니다.');
        });
    };

    const formatTime = (isoString) => {
        if (!isoString) return '';
        try {
            return new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
        } catch(e) {
            return '';
        }
    };

    return (
        <div className="flex-1 overflow-y-auto p-4 bg-slate-50/50 space-y-6">
            {messages.map((msg, index) => {
                const isBot = msg.sender === 'bot';
                const isSystem = msg.sender === 'system';
                const isLastMessage = index === messages.length - 1;

                if (isSystem) {
                    return (
                        <div key={msg.id} className="flex justify-center my-2">
                            <div className="bg-gray-200 text-gray-600 text-[10px] px-3 py-1 rounded-full text-center max-w-[80%] flex flex-col items-center">
                                <span>{msg.text}</span>
                                {msg.timestamp && <span className="text-[9px] mt-0.5 opacity-70">{formatTime(msg.timestamp)}</span>}
                            </div>
                        </div>
                    );
                }

                return (
                    <div key={msg.id} className={`flex w-full ${isBot ? 'justify-start' : 'justify-end'}`}>
                        {isBot && (
                            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center mr-2 flex-shrink-0 shadow-sm border border-blue-200 mt-1">
                                <Bot size={16} className="text-blue-600" />
                            </div>
                        )}
                        
                        <div className={`flex flex-col max-w-[85%]`}>
                            {!isBot && <span className="text-[10px] text-gray-500 mb-1 ml-auto mr-1">나</span>}
                            {isBot && <span className="text-[10px] text-blue-600 mb-1 ml-1 font-bold">AI 퓨봇 비서</span>}
                            
                            <div className={`rounded-2xl px-4 py-3 shadow-sm text-sm leading-relaxed ${
                                msg.isTyping ? 'bg-gray-100 text-gray-500 rounded-tl-none italic' :
                                isBot ? 'bg-white border border-gray-200 rounded-tl-none text-gray-800' : 'bg-blue-600 text-white rounded-tr-none'
                            }`}>
                                {msg.isTyping && (
                                    <div className="flex space-x-1 py-1">
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                                    </div>
                                )}

                                {!msg.isTyping && !isBot && (
                                    <p className="whitespace-pre-wrap break-words">{msg.text}</p>
                                )}

                                {!msg.isTyping && isBot && (
                                    <div className="space-y-4">
                                        {/* 1. Answer */}
                                        {msg.answer && (
                                            <div className="whitespace-pre-wrap leading-relaxed">{msg.answer}</div>
                                        )}

                                        {/* 2. Safety Notice */}
                                        {msg.responseType === 'SAFETY' && (
                                            <SafetyNotice />
                                        )}

                                        {/* 3. Policies */}
                                        {msg.responseType === 'POLICY_SEARCH' && msg.policies && msg.policies.length > 0 && (
                                            <>
                                                {msg.matchedPolicyCount !== undefined && <div className="mt-2 text-xs text-blue-600 font-semibold bg-blue-50 px-2 py-1 rounded inline-block">찾은 정책: {msg.matchedPolicyCount}건</div>}
                                                <PolicyCardList policies={msg.policies} />
                                            </>
                                        )}

                                        {/* 4. References */}
                                        {msg.responseType === 'POLICY_SEARCH' && msg.references && msg.references.length > 0 && (
                                            <div className="pt-2 border-t border-gray-100">
                                                <p className="text-xs font-semibold text-gray-700 mb-2">🔗 관련 링크</p>
                                                <ul className="space-y-1">
                                                    {msg.references.map((ref, idx) => (
                                                        <li key={idx}>
                                                            <a 
                                                                href={ref.detailUrl} 
                                                                target="_blank" 
                                                                rel="noreferrer"
                                                                className="text-xs text-blue-500 hover:text-blue-700 underline flex items-center"
                                                            >
                                                                {ref.serviceName || '참고 링크'} <ExternalLink size={10} className="ml-1" />
                                                            </a>
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>

                            {/* Suggested Reply Chips - Only show on last message if exists */}
                            {!msg.isTyping && isBot && isLastMessage && msg.suggestedReplies && msg.suggestedReplies.length > 0 && (
                                <SuggestedReplyChips 
                                    chips={msg.suggestedReplies} 
                                    onChipClick={onChipClick} 
                                    size={msg.responseType === 'CLARIFICATION_NEEDED' ? 'large' : 'default'}
                                />
                            )}

                            {/* Timestamp Component / Button Actions Area */}
                            {msg.timestamp && !msg.isTyping && (
                                <div className={`flex items-center mt-1.5 space-x-3 text-[11px] text-gray-400 ${isBot ? 'justify-start ml-2' : 'justify-end mr-2'}`}>
                                    <span>{formatTime(msg.timestamp)}</span>
                                    
                                    {isBot && (
                                        <div className="flex items-center space-x-2 border-l border-gray-300 pl-3">
                                            <button 
                                                onClick={() => handleTTS(msg.answer || msg.text)} 
                                                className="hover:text-gray-700 flex items-center transition-colors"
                                                title="음성으로 듣기"
                                            >
                                                <Volume2 size={13} className="mr-1" /> 음성
                                            </button>
                                            <button 
                                                onClick={() => handleCopy(msg.answer || msg.text)} 
                                                className="hover:text-gray-700 flex items-center transition-colors"
                                                title="텍스트 복사"
                                            >
                                                <Copy size={13} className="mr-1" /> 복사
                                            </button>
                                        </div>
                                    )}
                                </div>
                            )}

                        </div>
                    </div>
                );
            })}
            <div ref={endRef} />
        </div>
    );
};

export default ChatMessageList;
