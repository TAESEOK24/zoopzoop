import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

// 컴포넌트 불러오기
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import MainPage from './pages/Main/index'; // 방금까지 만든 메인 페이지

import LoginPage from './pages/Login/index';
import SignupPage from './pages/Signup/index';

// 임시 빈 화면(페이지) 컴포넌트들
const EmptyPage = ({ title }) => (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <h1 className="text-3xl font-bold text-gray-500">{title} 페이지 (준비 중입니다 🚀)</h1>
    </div>
);

function App() {
    return (
        <Router>
            {/* Header를 라우터 안에 두면 모든 페이지에서 상단에 고정됩니다. */}
            <Header />

            {/* 페이지 이동 경로 설정 */}
            <Routes>
                <Route path="/" element={<MainPage />} />
                <Route path="/ai-chat" element={<EmptyPage title="AI 채팅" />} />
                <Route path="/community" element={<EmptyPage title="커뮤니티" />} />
                <Route path="/policies" element={<EmptyPage title="정책목록" />} />
                <Route path="/qna" element={<EmptyPage title="질의응답" />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/signup" element={<SignupPage />} />
            </Routes>

            {/* Footer도 모든 페이지 하단에 고정됩니다. */}
            <Footer />
        </Router>
    );
}

export default App;