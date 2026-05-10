import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, CheckCheck, User } from 'lucide-react';
import {
    fetchRecentNotifications,
    fetchUnreadNotificationCount,
    markAllNotificationsRead,
    markNotificationRead,
} from '../../api/notifications';

const Header = () => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem('accessToken'));
    const [userName, setUserName] = useState(localStorage.getItem('userName'));
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [isNotificationOpen, setIsNotificationOpen] = useState(false);

    useEffect(() => {
        const handleStateChange = () => {
            const nextLoggedIn = !!localStorage.getItem('accessToken');
            setIsLoggedIn(nextLoggedIn);
            setUserName(localStorage.getItem('userName'));
            if (!nextLoggedIn) {
                setNotifications([]);
                setUnreadCount(0);
                setIsNotificationOpen(false);
            }
        };
        window.addEventListener('loginStateChange', handleStateChange);
        window.addEventListener('storage', handleStateChange);

        return () => {
            window.removeEventListener('loginStateChange', handleStateChange);
            window.removeEventListener('storage', handleStateChange);
        };
    }, []);

    useEffect(() => {
        if (!isLoggedIn) {
            return undefined;
        }

        let cancelled = false;

        const loadNotificationSummary = async () => {
            try {
                const [recentResult, countResult] = await Promise.all([
                    fetchRecentNotifications(5),
                    fetchUnreadNotificationCount(),
                ]);

                if (!cancelled) {
                    setNotifications(recentResult?.data?.items ?? []);
                    setUnreadCount(countResult?.data?.unreadCount ?? 0);
                }
            } catch {
                if (!cancelled) {
                    setNotifications([]);
                    setUnreadCount(0);
                }
            }
        };

        loadNotificationSummary();

        return () => {
            cancelled = true;
        };
    }, [isLoggedIn]);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('userName'); // 로그아웃 시 닉네임도 삭제
        window.dispatchEvent(new Event('loginStateChange'));
        navigate('/');
    };

    const handleNotificationClick = async (notification) => {
        try {
            if (!notification.read) {
                await markNotificationRead(notification.id);
                setUnreadCount((current) => Math.max(current - 1, 0));
                setNotifications((current) => current.map((item) => (
                    item.id === notification.id ? { ...item, read: true } : item
                )));
            }
        } catch {
            // Navigation still works even if the read state update fails.
        }

        setIsNotificationOpen(false);
        navigate(`/policies/${notification.serviceId}`);
    };

    const handleReadAll = async () => {
        try {
            await markAllNotificationsRead();
            setUnreadCount(0);
            setNotifications((current) => current.map((item) => ({ ...item, read: true })));
        } catch {
            return;
        }
    };

    return (
        <header className="flex items-center justify-between px-8 py-4 bg-white shadow-sm sticky top-0 z-40">
            <Link to="/" className="text-2xl font-bold text-blue-600 cursor-pointer">
                ZoopZoop
            </Link>

            <nav className="hidden md:flex space-x-8 text-lg font-medium">
                <Link to="/ai-chat" className="hover:text-blue-500 transition-colors">AI채팅</Link>
                <Link to="/community" className="hover:text-blue-500 transition-colors">커뮤니티</Link>
                <Link to="/policies" className="hover:text-blue-500 transition-colors">정책목록</Link>
                <Link to="/support" className="hover:text-blue-500 transition-colors">고객센터</Link>
            </nav>

            <div className="flex items-center space-x-4">
                {isLoggedIn ? (
                    <div className="flex items-center space-x-4">
                        <div className="relative">
                            <button
                                type="button"
                                onClick={() => setIsNotificationOpen((current) => !current)}
                                className="relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-100 bg-gray-50 text-gray-500 transition hover:border-blue-100 hover:bg-blue-50 hover:text-blue-600"
                                aria-label="알림"
                            >
                                <Bell className="h-5 w-5" />
                                {unreadCount > 0 && (
                                    <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-red-500 px-1.5 py-0.5 text-center text-[10px] font-bold leading-4 text-white">
                                        {unreadCount > 9 ? '9+' : unreadCount}
                                    </span>
                                )}
                            </button>

                            {isNotificationOpen && (
                                <div className="absolute right-0 top-12 w-80 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-xl">
                                    <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
                                        <span className="text-sm font-black text-gray-900">알림</span>
                                        <button
                                            type="button"
                                            onClick={handleReadAll}
                                            className="inline-flex items-center gap-1 text-xs font-bold text-blue-600 hover:text-blue-700"
                                        >
                                            <CheckCheck className="h-3.5 w-3.5" />
                                            모두 읽음
                                        </button>
                                    </div>
                                    <div className="max-h-96 overflow-y-auto">
                                        {notifications.length === 0 && (
                                            <div className="px-4 py-8 text-center text-sm font-semibold text-gray-400">
                                                아직 알림이 없습니다.
                                            </div>
                                        )}
                                        {notifications.map((notification) => (
                                            <button
                                                key={notification.id}
                                                type="button"
                                                onClick={() => handleNotificationClick(notification)}
                                                className="block w-full border-b border-gray-50 px-4 py-3 text-left transition hover:bg-blue-50"
                                            >
                                                <div className="flex items-start gap-2">
                                                    {!notification.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-500" />}
                                                    <div className="min-w-0">
                                                        <p className="text-sm font-bold text-gray-900">{notification.title}</p>
                                                        <p className="mt-1 line-clamp-2 text-xs leading-5 text-gray-500">{notification.message}</p>
                                                    </div>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                    <Link
                                        to="/mypage/notifications"
                                        onClick={() => setIsNotificationOpen(false)}
                                        className="block bg-gray-50 px-4 py-3 text-center text-xs font-bold text-gray-600 transition hover:bg-gray-100 hover:text-blue-600"
                                    >
                                        알림 설정으로 이동
                                    </Link>
                                </div>
                            )}
                        </div>

                        {/* 닉네임 표시 영역 */}
                        <div className="flex items-center text-sm font-medium text-gray-700 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-100">
                            <User className="w-4 h-4 mr-1.5 text-blue-500" />
                            <span className="font-bold text-blue-600">{userName}</span>님
                        </div>

                        {/* 마이페이지 이동 버튼 */}
                        <Link
                            to="/mypage"
                            className="px-4 py-2 text-sm font-bold text-blue-600 border border-blue-600 rounded-lg hover:bg-blue-50 transition-all shadow-sm"
                        >
                            마이페이지
                        </Link>

                        <button
                            onClick={handleLogout}
                            className="px-2 py-2 text-sm font-semibold text-gray-500 hover:text-red-500 transition-colors"
                        >
                            로그아웃
                        </button>
                    </div>
                ) : (
                    <>
                        <Link
                            to="/login"
                            className="px-4 py-2 text-sm font-semibold text-gray-600 hover:text-blue-600 flex items-center"
                        >
                            로그인
                        </Link>
                        <Link
                            to="/signup"
                            className="px-4 py-2 text-sm font-semibold text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors flex items-center shadow-md"
                        >
                            회원가입
                        </Link>
                    </>
                )}
            </div>
        </header>
    );
};

export default Header;
