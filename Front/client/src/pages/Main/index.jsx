import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import HeroSection from './HeroSection';
import CategoryGrid from './CategoryGrid';
import PolicyList from './PolicyList';
import { isAuthenticated } from '../../api/authSession';

const MainPage = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(isAuthenticated());
    const [query, setQuery] = useState('');

    useEffect(() => {
        const syncLoginState = () => setIsLoggedIn(isAuthenticated());
        window.addEventListener('loginStateChange', syncLoginState);
        window.addEventListener('storage', syncLoginState);

        return () => {
            window.removeEventListener('loginStateChange', syncLoginState);
            window.removeEventListener('storage', syncLoginState);
        };
    }, []);

    const handleSearch = (value) => {
        setQuery(value);
    };

    const handleAiStartClick = () => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }

        navigate('/ai-chat');
    };

    return (
        <div className="relative min-h-screen bg-gray-50">
            {showLoginModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
                    <div className="absolute inset-0" onClick={() => setShowLoginModal(false)} />
                    <div className="relative z-10 w-full max-w-md rounded-3xl bg-white p-8 text-center shadow-2xl">
                        <button
                            onClick={() => setShowLoginModal(false)}
                            className="absolute right-4 top-4 text-gray-400 hover:text-gray-700"
                        >
                            X
                        </button>
                        <div className="mb-4 text-4xl">AI</div>
                        <h2 className="mb-2 text-2xl font-bold text-gray-900">
                            로그인 후 맞춤형 추천을 확인할 수 있습니다
                        </h2>
                        <p className="mb-6 text-sm text-gray-600">
                            회원님의 검색과 조회 이력을 바탕으로 더 적합한 정책을 추천합니다.
                        </p>
                        <button className="w-full rounded-xl bg-blue-600 py-3 font-bold text-white hover:bg-blue-700">
                            로그인하고 추천 보기
                        </button>
                    </div>
                </div>
            )}

            <HeroSection onSearch={handleSearch} />

            <div className="container mx-auto max-w-5xl px-4 py-10">
                <section className="relative z-20 mb-16 -mt-6">
                    <div className="flex w-full flex-col items-center justify-between rounded-2xl border-2 border-blue-100 bg-white p-8 shadow-md md:flex-row">
                        <div className="mb-4 md:mb-0">
                            <h2 className="mb-2 flex items-center gap-2 text-2xl font-bold text-gray-900">
                                내 상황에 맞는 정책 찾기
                                <span className="rounded-full bg-blue-100 px-2 py-1 text-xs font-bold text-blue-700">
                                    회원 전용
                                </span>
                            </h2>
                            <p className="text-gray-600">
                                로그인한 회원의 최근 행동을 바탕으로 AI와 추천 로직이 정책 탐색을 돕습니다.
                            </p>
                        </div>
                        <button
                            onClick={handleAiStartClick}
                            className="whitespace-nowrap rounded-xl bg-gray-900 px-6 py-3 font-bold text-white transition-transform hover:scale-105 hover:bg-gray-800"
                        >
                            AI 정책 분석 시작
                        </button>
                    </div>
                </section>

                <section className="mb-16">
                    <h2 className="mb-6 text-2xl font-bold text-gray-800">분야별로 둘러보기</h2>
                    <CategoryGrid />
                </section>

                <section>
                    <div className="mb-6 flex items-center justify-between">
                        <h2 className="text-2xl font-bold text-gray-800">
                            {isLoggedIn && !query.trim() ? '맞춤 추천 정책' : '지금 가장 유용한 정책'}
                        </h2>
                        <button className="font-semibold text-blue-600 hover:underline">전체보기 &gt;</button>
                    </div>
                    <PolicyList query={query} isLoggedIn={isLoggedIn} />
                </section>
            </div>
        </div>
    );
};

export default MainPage;
