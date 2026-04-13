import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Footer from './components/layout/Footer';
import Header from './components/layout/Header';
import AIChatPage from './pages/AIChat/index';
// 1️⃣ 커뮤니티 페이지 import 추가
import CommunityPage from './pages/Community/index';
import LoginPage from './pages/Login/index';
import MainPage from './pages/Main/index';
import PolicyDetailPage from './pages/Policy/Detail';
import PolicyPage from './pages/Policy/index';
import SignupPage from './pages/Signup/index';
import ChatbotWidget from './components/Chatbot/ChatbotWidget';
import CommunityWrite from './pages/Community/Write';
import PostDetail from './pages/Community/PostDetail';
import CommunityEdit from './pages/Community/CommunityEdit';

const EmptyPage = ({ title }) => (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <h1 className="text-3xl font-bold text-gray-500">{title} 페이지는 준비 중입니다.</h1>
    </div>
);

function App() {
    return (
        <Router>
            <Header />
            <Routes>
                <Route path="/" element={<MainPage />} />
                <Route path="/ai-chat" element={<AIChatPage />} />

                {/* 2️⃣ EmptyPage를 CommunityPage로 변경 */}
                <Route path="/community" element={<CommunityPage />} />

                <Route path="/policies" element={<PolicyPage />} />
                <Route path="/policies/:serviceId" element={<PolicyDetailPage />} />
                <Route path="/qna" element={<EmptyPage title="질의응답" />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/signup" element={<SignupPage />} />
                <Route path="/community/write" element={<CommunityWrite />} />
                <Route path="/community/post/:id" element={<PostDetail />} />
                <Route path="/community/edit/:id" element={<CommunityEdit />} />
            </Routes>
            <Footer />
            <ChatbotWidget />
        </Router>
    );
}

export default App;