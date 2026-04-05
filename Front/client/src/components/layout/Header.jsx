import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Header = () => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem('accessToken'));

    useEffect(() => {
        const handleStateChange = () => setIsLoggedIn(!!localStorage.getItem('accessToken'));
        window.addEventListener('loginStateChange', handleStateChange);
        // 다른 탭에서의 로그인/로그아웃 대응
        window.addEventListener('storage', handleStateChange);
        
        return () => {
            window.removeEventListener('loginStateChange', handleStateChange);
            window.removeEventListener('storage', handleStateChange);
        };
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        window.dispatchEvent(new Event('loginStateChange'));
        navigate('/');
    };

    return (
        <header className="flex items-center justify-between px-8 py-4 bg-white shadow-sm sticky top-0 z-40">
            {/* 로고 클릭 시 메인 화면("/")으로 이동 */}
            <Link to="/" className="text-2xl font-bold text-blue-600 cursor-pointer">
                ZoopZoop
            </Link>

            <nav className="hidden md:flex space-x-8 text-lg font-medium">
                <Link to="/ai-chat" className="hover:text-blue-500 transition-colors">AI채팅</Link>
                <Link to="/community" className="hover:text-blue-500 transition-colors">커뮤니티</Link>
                <Link to="/policies" className="hover:text-blue-500 transition-colors">정책목록</Link>
                <Link to="/qna" className="hover:text-blue-500 transition-colors">질의응답</Link>
            </nav>

            <div className="flex space-x-4">
                {isLoggedIn ? (
                    <button
                        onClick={handleLogout}
                        className="px-4 py-2 text-sm font-semibold text-gray-600 hover:text-blue-600 flex items-center transition-colors"
                    >
                        로그아웃
                    </button>
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
                            className="px-4 py-2 text-sm font-semibold text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors flex items-center"
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