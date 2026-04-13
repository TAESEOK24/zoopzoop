import React, { useState, useRef, useEffect } from 'react';
import { Send } from 'lucide-react';

/**
 * @param {{ onSend: (msg: string) => void, disabled: boolean }} props 
 */
const ChatInput = ({ onSend, disabled }) => {
    const [text, setText] = useState('');
    const textareaRef = useRef(null);

    const SUGGESTIONS = [
        "청년 주거 지원 정책 알려줘",
        "대학생 장학금 관련 정보 찾아줘",
        "취업 준비생을 위한 지원금 있어?"
    ];

    const handleSubmit = (e) => {
        if (e) e.preventDefault();
        const trimmed = text.trim();
        if (!trimmed || disabled) return;
        
        onSend(trimmed);
        setText('');
        
        // Reset textarea height
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    const handleInput = (e) => {
        setText(e.target.value);
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
        }
    };

    const handleSuggestionClick = (suggestion) => {
        if (disabled) return;
        onSend(suggestion);
    };

    return (
        <div className="bg-white border-t border-gray-200 shrink-0 flex flex-col">
            {/* Suggestions */}
            <div className="flex overflow-x-auto p-2 gap-2 scrollbar-hide border-b border-gray-50">
                {SUGGESTIONS.map((sug, idx) => (
                    <button
                        key={idx}
                        onClick={() => handleSuggestionClick(sug)}
                        disabled={disabled}
                        className="whitespace-nowrap px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs rounded-full border border-blue-100 transition-colors disabled:opacity-50"
                        title={sug}
                    >
                        {sug}
                    </button>
                ))}
            </div>

            {/* Input Area */}
            <form onSubmit={handleSubmit} className="p-3 flex items-end gap-2 bg-gray-50">
                <textarea
                    ref={textareaRef}
                    value={text}
                    onChange={handleInput}
                    onKeyDown={handleKeyDown}
                    disabled={disabled}
                    placeholder="메시지를 입력하세요... (Shift+Enter로 줄바꿈)"
                    className="flex-1 max-h-[120px] min-h-[44px] resize-none border border-gray-300 rounded-2xl px-4 py-3 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-shadow bg-white disabled:bg-gray-100"
                    rows={1}
                />
                <button
                    type="submit"
                    disabled={!text.trim() || disabled}
                    className="w-11 h-11 shrink-0 flex items-center justify-center rounded-full bg-blue-600 text-white disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors hover:bg-blue-700 shadow-sm"
                    aria-label="전송"
                >
                    <Send size={18} className="translate-x-[1px]" />
                </button>
            </form>
        </div>
    );
};

export default ChatInput;
