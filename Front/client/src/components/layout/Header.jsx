import React from 'react';
import { Link } from 'react-router-dom';

const Header = () => {
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
                {localStorage.getItem('accessToken') ? (
                    <button
                        onClick={() => {
                            localStorage.removeItem('accessToken');
                            window.location.href = '/';
                        }}
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