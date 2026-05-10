import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import MainPage from './pages/Main';
import LoginPage from './pages/Login';
import SignupPage from './pages/Signup';
import CommunityPage from './pages/Community';
import PostDetail from './pages/Community/PostDetail';
import CommunityWrite from './pages/Community/Write';
import CommunityEdit from './pages/Community/CommunityEdit';
import AIChatPage from './pages/AIChat';
import PolicyPage from './pages/Policy';
import PolicyDetail from './pages/Policy/Detail';
import MyPage from './pages/MyPage';
import NotificationSettings from './pages/MyPage/NotificationSettings';
import ScrapsPage from './pages/MyPage/Scraps';

function App() {
    return (
        <Router>
            <div className="flex flex-col min-h-screen">
                <Header />
                <main className="flex-grow">
                    <Routes>
                        <Route path="/" element={<MainPage />} />
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/signup" element={<SignupPage />} />
                        <Route path="/community" element={<CommunityPage />} />
                        <Route path="/community/post/:id" element={<PostDetail />} />
                        <Route path="/community/write" element={<CommunityWrite />} />
                        <Route path="/community/edit/:id" element={<CommunityEdit />} />
                        <Route path="/ai-chat" element={<AIChatPage />} />
                        <Route path="/policies" element={<PolicyPage />} />
                        <Route path="/policies/:serviceId" element={<PolicyDetail />} />
                        <Route path="/policy/:serviceId" element={<PolicyDetail />} />
                        <Route path="/mypage" element={<MyPage />} />
                        <Route path="/mypage/notifications" element={<NotificationSettings />} />
                        <Route path="/mypage/scraps" element={<ScrapsPage />} />
                    </Routes>
                </main>
                <Footer />
            </div>
        </Router>
    );
}

export default App;
