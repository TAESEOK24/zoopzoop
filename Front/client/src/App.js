import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Footer from './components/layout/Footer';
import Header from './components/layout/Header';
import LoginPage from './pages/Login/index';
import MainPage from './pages/Main/index';
import PolicyDetailPage from './pages/Policy/Detail';
import PolicyPage from './pages/Policy/index';
import SignupPage from './pages/Signup/index';

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
                <Route path="/ai-chat" element={<EmptyPage title="AI 채팅" />} />
                <Route path="/community" element={<EmptyPage title="커뮤니티" />} />
                <Route path="/policies" element={<PolicyPage />} />
                <Route path="/policies/:serviceId" element={<PolicyDetailPage />} />
                <Route path="/qna" element={<EmptyPage title="질의응답" />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/signup" element={<SignupPage />} />
            </Routes>
            <Footer />
        </Router>
    );
}

export default App;
