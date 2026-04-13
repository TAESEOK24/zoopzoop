import React, { useEffect, useRef } from 'react';
import { Bot, User, ExternalLink } from 'lucide-react';
import PolicyCardList from './PolicyCardList';

/**
 * @typedef {Object} Message
 * @property {string} id
 * @property {'user'|'bot'|'system'} sender
 * @property {string} [text] - User message or System message
 * @property {string} [answer] - Bot answer
 * @property {Array} [policies]
 * @property {Array} [references]
 * @property {boolean} [isTyping]
 * @property {number} [matchedPolicyCount]
 */

/**
 * @param {{ messages: Message[] }} props 
 */
const ChatMessageList = ({ messages }) => {
    const endRef = useRef(null);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div className="flex-1 overflow-y-auto p-4 bg-slate-50/50 space-y-6">
            {messages.map((msg) => {
                const isBot = msg.sender === 'bot';
                const isSystem = msg.sender === 'system';

                if (isSystem) {
                    return (
                        <div key={msg.id} className="flex justify-center my-2">
                            <div className="bg-gray-200 text-gray-600 text-[10px] px-3 py-1 rounded-full text-center max-w-[80%]">
                                {msg.text}
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
                            {isBot && <span className="text-[10px] text-blue-600 mb-1 ml-1 font-medium">AI 정책비서</span>}
                            
                            <div className={`rounded-2xl px-4 py-3 shadow-sm text-sm leading-relaxed ${
                                msg.isTyping ? 'bg-gray-100 text-gray-500 rounded-tl-none italic' :
                                isBot ? 'bg-white border border-gray-200 rounded-tl-none' : 'bg-blue-600 text-white rounded-tr-none'
                            }`}>
                                {msg.isTyping && (
                                    <div className="flex space-x-1 py-1">
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                                    </div>
                                )}

                                {!msg.isTyping && !isBot && (
                                    <p className="whitespace-pre-wrap word-break">{msg.text}</p>
                                )}

                                {!msg.isTyping && isBot && (
                                    <div className="space-y-4">
                                        {/* 1. Answer */}
                                        {msg.answer && (
                                            <div className="whitespace-pre-wrap">{msg.answer}</div>
                                        )}

                                        {/* 2. Policies */}
                                        {msg.matchedPolicyCount !== undefined && <div className="mt-2 text-xs text-blue-600 font-semibold bg-blue-50 px-2 py-1 rounded inline-block">찾은 정책: {msg.matchedPolicyCount}건</div>}
                                        {msg.policies && msg.policies.length > 0 && (
                                            <PolicyCardList policies={msg.policies} />
                                        )}

                                        {/* 3. References */}
                                        {msg.references && msg.references.length > 0 && (
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
                        </div>
                    </div>
                );
            })}
            <div ref={endRef} />
        </div>
    );
};

export default ChatMessageList;
