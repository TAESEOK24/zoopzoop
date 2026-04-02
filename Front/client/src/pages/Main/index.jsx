import React, { useState } from 'react';

import Header from '../../components/layout/Header';
import HeroSection from '../Main/HeroSection';
import CategoryGrid from '../Main/CategoryGrid';
import PolicyList from '../Main/PolicyList';

const MainPage = () => {
    // ... (나머지 코드는 이전과 완전히 동일합니다)
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    const handleSearch = (query) => {
        console.log("검색어:", query);
    };

    const handleAiStartClick = () => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
        } else {
            alert('AI 혜택 분석 페이지로 이동합니다!');
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 relative">
            {/* 로그인 모달 */}
            {showLoginModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-60">
                    <div className="absolute inset-0" onClick={() => setShowLoginModal(false)}></div>
                    <div className="relative bg-white rounded-3xl shadow-2xl w-full max-w-md p-8 z-10 text-center">
                        <button onClick={() => setShowLoginModal(false)} className="absolute top-4 right-4 text-gray-400 hover:text-gray-700">✕</button>
                        <div className="text-4xl mb-4">🎁</div>
                        <h2 className="text-2xl font-bold text-gray-900 mb-2">딱 맞는 혜택, 로그인하고 받아가세요</h2>
                        <p className="text-gray-600 mb-6 text-sm">AI가 회원님의 정보를 분석해 숨은 지원금을 찾아드립니다.</p>
                        <button className="w-full py-3 bg-blue-600 text-white font-bold rounded-xl mb-3 hover:bg-blue-700">가입하고 내 혜택 확인하기</button>
                    </div>
                </div>
            )}


            {/* 상단 비주얼 및 검색창 */}
            <HeroSection onSearch={handleSearch} />

            <div className="container mx-auto px-4 py-10 max-w-5xl">

                {/* 신규: AI 혜택 찾기 배너 */}
                <section className="mb-16 -mt-6 relative z-20">
                    <div className="w-full bg-white border-2 border-blue-100 rounded-2xl p-8 shadow-md flex flex-col md:flex-row items-center justify-between">
                        <div className="mb-4 md:mb-0">
                            <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2 mb-2">
                                ✨ 내 연령대와 상황에 딱 맞는 혜택 찾기
                                <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-full">회원 전용</span>
                            </h2>
                            <p className="text-gray-600">가입된 프로필 정보를 기반으로 AI가 1분 만에 숨은 혜택을 매칭해 드립니다.</p>
                        </div>
                        <button
                            onClick={handleAiStartClick}
                            className="px-6 py-3 bg-gray-900 text-white font-bold rounded-xl hover:bg-gray-800 transition-transform hover:scale-105 whitespace-nowrap"
                        >
                            🚀 AI 혜택 분석 시작
                        </button>
                    </div>
                </section>

                {/* 카테고리 퀵 메뉴 */}
                <section className="mb-16">
                    <h2 className="text-2xl font-bold mb-6 text-gray-800">분야별로 줍줍 🍎</h2>
                    <CategoryGrid />
                </section>

                {/* 인기 정책 리스트 */}
                <section>
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">지금 가장 핫한 혜택 🔥</h2>
                        <button className="text-blue-600 font-semibold hover:underline">전체보기 &gt;</button>
                    </div>
                    <PolicyList />
                </section>
            </div>
        </div>
    );
};

export default MainPage;