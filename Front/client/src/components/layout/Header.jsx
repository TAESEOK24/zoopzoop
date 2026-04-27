import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User } from 'lucide-react';

const Header = () => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem('accessToken'));
    const [userName, setUserName] = useState(localStorage.getItem('userName'));

    useEffect(() => {
        const handleStateChange = () => {
            setIsLoggedIn(!!localStorage.getItem('accessToken'));
            setUserName(localStorage.getItem('userName'));
        };
        window.addEventListener('loginStateChange', handleStateChange);
        window.addEventListener('storage', handleStateChange);

        return () => {
            window.removeEventListener('loginStateChange', handleStateChange);
            window.removeEventListener('storage', handleStateChange);
        };
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('userName'); // 로그아웃 시 닉네임도 삭제
        window.dispatchEvent(new Event('loginStateChange'));
        navigate('/');
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