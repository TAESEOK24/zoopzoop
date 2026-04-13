import React from 'react';
import { X, MessageSquare, Clock } from 'lucide-react';

const ChatHistorySidebar = ({ isOpen, onClose, sessions, onSelectSession }) => {
    return (
        <>
            {/* Backdrop */}
            {isOpen && (
                <div 
                    className="absolute inset-0 bg-black bg-opacity-30 z-20 transition-opacity"
                    onClick={onClose}
                />
            )}

            {/* Sidebar */}
            <div 
                className={`absolute top-0 right-0 h-full w-64 bg-white shadow-xl z-30 transform transition-transform duration-300 ease-in-out flex flex-col ${
                    isOpen ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="flex justify-between items-center p-4 border-b border-gray-100">
                    <h3 className="font-bold flex items-center text-gray-800">
                        <Clock className="w-4 h-4 mr-2" /> 
                        이전 대화 내역
                    </h3>
                    <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded text-gray-500">
                        <X size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-2">
                    {sessions.length === 0 ? (
                        <div className="text-center text-sm text-gray-400 mt-10">
                            이전 대화 내역이 없습니다.
                        </div>
                    ) : (
                        <ul className="space-y-2">
                            {sessions.map((session) => (
                                <li key={session.id}>
                                    <button
                                        onClick={() => onSelectSession(session)}
                                        className="w-full text-left p-3 rounded hover:bg-gray-50 border border-transparent hover:border-gray-200 transition-colors group"
                                    >
                                        <div className="flex items-start">
                                            <MessageSquare className="w-4 h-4 mr-2 mt-1 text-gray-400 group-hover:text-blue-500 shrink-0" />
                                            <div className="flex-1 min-w-0">
                                                <p className="text-sm font-medium text-gray-700 truncate">
                                                    {session.title || '새로운 대화'}
                                                </p>
                                                <p className="text-xs text-gray-400 mt-1">
                                                    {new Date(session.timestamp).toLocaleString('ko-KR', {
                                                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                                                    })}
                                                </p>
                                            </div>
                                        </div>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        </>
    );
};

export default ChatHistorySidebar;
