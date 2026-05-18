import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, FileText, Settings, ShieldAlert, BarChart3, Trash2, ArrowLeft, RefreshCw } from 'lucide-react';
import axiosInstance from '../../api/index';

const AdminPage = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('dashboard');

    // 상태 관리
    const [loading, setLoading] = useState(true); // 보안 검증 로딩
    const [stats, setStats] = useState({ totalUsers: 0, todayPosts: 0, totalPolicies: 0 });
    const [userList, setUserList] = useState([]);
    const [postList, setPostList] = useState([]);
    const [isSyncing, setIsSyncing] = useState(false);

    // 🔒 1. 보안 강화: 관리자 권한 검증 (주석 해제 완료!)
    useEffect(() => {
        const checkAdmin = async () => {
            try {
                const res = await axiosInstance.get('/api/users/me');
                if (res.data.data.role !== 'ADMIN') {
                    alert('관리자만 접근할 수 있는 페이지입니다. 🚫');
                    navigate('/'); // 일반 유저면 메인으로 쫓아냄
                } else {
                    setLoading(false); // 관리자면 화면 렌더링 허용
                    fetchDashboardStats(); // 첫 화면 대시보드 통계 불러오기
                }
            } catch (error) {
                navigate('/login');
            }
        };
        checkAdmin();
    }, [navigate]);

    // 🔄 탭 변경 시 데이터 자동 로드
    useEffect(() => {
        if (activeTab === 'dashboard') fetchDashboardStats();
        if (activeTab === 'users') fetchUsers();
        if (activeTab === 'posts') fetchPosts();
    }, [activeTab]);

    // --- API 호출 함수들 ---
    const fetchDashboardStats = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/dashboard');
            setStats(res.data.data);
        } catch (error) { console.error("통계 로드 실패", error); }
    };

    const fetchUsers = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/users');
            setUserList(res.data.data);
        } catch (error) { console.error('유저 로드 실패', error); }
    };

    const fetchPosts = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/posts');
            setPostList(res.data.data);
        } catch (error) { console.error('게시글 로드 실패', error); }
    };

    // --- 액션(삭제/동기화) 함수들 ---
    const handleDeleteUser = async (userId, userName) => {
        if (!window.confirm(`[${userName}] 유저를 강제 탈퇴시키겠습니까?`)) return;
        try {
            await axiosInstance.delete(`/api/admin/users/${userId}`);
            alert('삭제되었습니다.');
            fetchUsers();
        } catch (error) { alert('삭제 실패!'); }
    };

    const handleDeletePost = async (postId, title) => {
        if (!window.confirm(`게시글 [${title}] 을(를) 삭제하시겠습니까?`)) return;
        try {
            await axiosInstance.delete(`/api/admin/posts/${postId}`);
            alert('게시글이 삭제되었습니다.');
            fetchPosts();
        } catch (error) { alert('삭제 실패!'); }
    };

    const handleSyncPolicies = async () => {
        if (!window.confirm('공공데이터포털에서 최신 정책 데이터를 가져오시겠습니까?\n이 작업은 몇 분 정도 소요될 수 있습니다.')) return;
        setIsSyncing(true);
        try {
            await axiosInstance.post('/api/admin/policies/sync');
            alert('성공적으로 최신 정책 데이터가 동기화되었습니다! ✅');
            fetchDashboardStats(); // 동기화 후 통계 업데이트
        } catch (error) {
            alert('동기화 중 오류가 발생했습니다.');
        } finally {
            setIsSyncing(false);
        }
    };

    // 화면 렌더링
    if (loading) return <div className="flex min-h-screen items-center justify-center bg-gray-50"><span className="text-xl font-bold text-indigo-600 animate-pulse">관리자 권한 확인 중... 🛡️</span></div>;

    return (
        <div className="min-h-screen bg-gray-50 flex">
            {/* 왼쪽 사이드바 */}
            <aside className="w-64 bg-[#1E1B4B] text-white flex flex-col shadow-2xl z-10">
                <div className="p-6 border-b border-white/10">
                    <h1 className="text-2xl font-black flex items-center space-x-2">
                        <ShieldAlert className="w-7 h-7 text-indigo-400" />
                        <span>Zoop Admin</span>
                    </h1>
                    <p className="text-indigo-200 text-xs mt-2 font-medium">시스템 통합 관리 대시보드</p>
                </div>

                <nav className="flex-1 p-4 space-y-2">
                    <SidebarButton active={activeTab === 'dashboard'} onClick={() => setActiveTab('dashboard')} icon={<BarChart3 className="w-5 h-5" />} label="대시보드 통계" />
                    <SidebarButton active={activeTab === 'users'} onClick={() => setActiveTab('users')} icon={<Users className="w-5 h-5" />} label="회원 관리" />
                    <SidebarButton active={activeTab === 'posts'} onClick={() => setActiveTab('posts')} icon={<FileText className="w-5 h-5" />} label="커뮤니티 관리" />
                    <SidebarButton active={activeTab === 'policies'} onClick={() => setActiveTab('policies')} icon={<Settings className="w-5 h-5" />} label="정책 데이터 동기화" />
                </nav>

                <div className="p-4 border-t border-white/10">
                    <button onClick={() => navigate('/')} className="flex items-center justify-center w-full py-3 text-sm font-bold text-indigo-200 hover:text-white bg-white/5 rounded-xl transition-colors">
                        <ArrowLeft className="w-4 h-4 mr-2" /> 홈페이지로 돌아가기
                    </button>
                </div>
            </aside>

            {/* 오른쪽 메인 콘텐츠 */}
            <main className="flex-1 overflow-y-auto p-10">
                <div className="mb-8">
                    <h2 className="text-3xl font-black text-gray-900">
                        {activeTab === 'dashboard' && '대시보드 개요'}
                        {activeTab === 'users' && '회원 관리 시스템'}
                        {activeTab === 'posts' && '커뮤니티 관리'}
                        {activeTab === 'policies' && '정책 데이터 관리'}
                    </h2>
                    <p className="text-gray-500 font-medium mt-1">시스템의 최신 상태를 확인하고 관리하세요.</p>
                </div>

                {/* 📊 탭 1: 대시보드 */}
                {activeTab === 'dashboard' && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-fade-in-up">
                        <StatCard label="총 가입자 수" value={`${stats.totalUsers}명`} trend="실시간 연동" />
                        <StatCard label="오늘 새 게시글" value={`${stats.todayPosts}개`} trend="실시간 연동" />
                        <StatCard label="등록된 정책 수" value={`${stats.totalPolicies}개`} trend="실시간 연동" />
                    </div>
                )}

                {/* 👥 탭 2: 회원 관리 (이전과 동일) */}
                {activeTab === 'users' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden animate-fade-in-up">
                        <div className="p-6 border-b border-gray-100 bg-gray-50/50">
                            <h3 className="font-bold text-gray-800">가입된 유저 목록</h3>
                        </div>
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black">
                            <tr><th className="px-6 py-4">ID</th><th className="px-6 py-4">이름</th><th className="px-6 py-4">이메일</th><th className="px-6 py-4">권한</th><th className="px-6 py-4 text-center">관리</th></tr>
                            </thead>
                            <tbody>
                            {userList.map((user) => (
                                <tr key={user.id} className="border-b border-gray-50">
                                    <td className="px-6 py-4">#{user.id}</td>
                                    <td className="px-6 py-4 font-bold">{user.name}</td>
                                    <td className="px-6 py-4">{user.email}</td>
                                    <td className="px-6 py-4">{user.role}</td>
                                    <td className="px-6 py-4 text-center">
                                        {user.role !== 'ADMIN' && (
                                            <button onClick={() => handleDeleteUser(user.id, user.name)} className="text-red-400 hover:text-red-600"><Trash2 className="w-5 h-5 inline" /></button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* 📝 탭 3: 커뮤니티 관리 (게시글 목록) */}
                {activeTab === 'posts' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden animate-fade-in-up">
                        <div className="p-6 border-b border-gray-100 bg-gray-50/50">
                            <h3 className="font-bold text-gray-800">전체 게시글 목록</h3>
                        </div>
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black">
                            <tr><th className="px-6 py-4">글 번호</th><th className="px-6 py-4">제목</th><th className="px-6 py-4">작성자</th><th className="px-6 py-4 text-center">강제 삭제</th></tr>
                            </thead>
                            <tbody>
                            {postList.map((post) => (
                                <tr key={post.id} className="border-b border-gray-50">
                                    <td className="px-6 py-4">#{post.id}</td>
                                    <td className="px-6 py-4 font-bold text-gray-800">{post.title}</td>
                                    <td className="px-6 py-4">{post.authorName}</td>
                                    <td className="px-6 py-4 text-center">
                                        <button onClick={() => handleDeletePost(post.id, post.title)} className="text-red-400 hover:text-red-600"><Trash2 className="w-5 h-5 inline" /></button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* ⚙️ 탭 4: 정책 데이터 동기화 */}
                {activeTab === 'policies' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-12 flex flex-col items-center justify-center text-center animate-fade-in-up">
                        <div className="w-20 h-20 bg-indigo-50 rounded-full flex items-center justify-center mb-6">
                            <RefreshCw className={`w-10 h-10 text-indigo-500 ${isSyncing ? 'animate-spin' : ''}`} />
                        </div>
                        <h3 className="text-xl font-black text-gray-900 mb-2">공공데이터포털 정책 동기화</h3>
                        <p className="text-gray-500 font-medium mb-8 max-w-md">
                            최신 청년 정책 데이터를 외부 API에서 가져와 시스템 데이터베이스에 저장하고 업데이트합니다.
                        </p>
                        <button
                            onClick={handleSyncPolicies}
                            disabled={isSyncing}
                            className={`px-8 py-4 rounded-2xl font-black text-white shadow-xl transition-all ${isSyncing ? 'bg-indigo-400 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700 hover:scale-105 shadow-indigo-200'}`}
                        >
                            {isSyncing ? '데이터를 긁어오는 중입니다...' : '지금 바로 데이터 동기화 시작'}
                        </button>
                    </div>
                )}
            </main>
        </div>
    );
};

// 미니 컴포넌트들
const SidebarButton = ({ active, onClick, icon, label }) => (
    <button onClick={onClick} className={`w-full flex items-center space-x-3 px-4 py-3.5 rounded-xl text-sm font-bold transition-all ${active ? 'bg-indigo-600 text-white shadow-lg' : 'text-indigo-200 hover:bg-white/10 hover:text-white'}`}>
        {icon}<span>{label}</span>
    </button>
);

const StatCard = ({ label, value, trend }) => (
    <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <h4 className="text-sm font-bold text-gray-400 mb-2">{label}</h4>
        <div className="text-3xl font-black text-gray-900 mb-1">{value}</div>
        <span className="text-xs font-bold text-emerald-500 bg-emerald-50 px-2 py-1 rounded-md">{trend}</span>
    </div>
);

export default AdminPage;