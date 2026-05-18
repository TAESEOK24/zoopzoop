import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, FileText, Settings, ShieldAlert, BarChart3, Trash2, ArrowLeft, AlertTriangle, CheckCircle, Eye, Search, ArrowUpDown } from 'lucide-react';
import axiosInstance from '../../api/index';

const AdminPage = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('dashboard');
    const [loading, setLoading] = useState(true);

    const [stats, setStats] = useState({ totalUsers: 0, todayPosts: 0, totalPolicies: 0 });
    const [userList, setUserList] = useState([]);
    const [postList, setPostList] = useState([]);
    const [reportList, setReportList] = useState([]);

    // 🚀 신고 탭 전용: 검색 및 정렬 상태 관리
    const [reportSearchTerm, setReportSearchTerm] = useState('');
    const [reportSortConfig, setReportSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

    // 🚀 게시글 탭 전용: 검색 및 정렬 상태 관리
    const [postSearchTerm, setPostSearchTerm] = useState('');
    const [postSortConfig, setPostSortConfig] = useState({ key: 'id', direction: 'desc' });

    useEffect(() => {
        const checkAdmin = async () => {
            try {
                const res = await axiosInstance.get('/api/users/me');
                if (res.data.data.role !== 'ADMIN') {
                    alert('관리자 전용 페이지입니다.');
                    navigate('/');
                } else {
                    setLoading(false);
                    fetchDashboardStats();
                }
            } catch (error) { navigate('/login'); }
        };
        checkAdmin();
    }, [navigate]);

    useEffect(() => {
        if (activeTab === 'dashboard') fetchDashboardStats();
        if (activeTab === 'users') fetchUsers();
        if (activeTab === 'posts') fetchPosts();
        if (activeTab === 'reports') fetchReports();
    }, [activeTab]);

    const fetchDashboardStats = async () => {
        try { const res = await axiosInstance.get('/api/admin/dashboard'); setStats(res.data.data); } catch (e) {}
    };
    const fetchUsers = async () => {
        try { const res = await axiosInstance.get('/api/admin/users'); setUserList(res.data.data); } catch (e) {}
    };
    const fetchPosts = async () => {
        try { const res = await axiosInstance.get('/api/admin/posts'); setPostList(res.data.data); } catch (e) {}
    };
    const fetchReports = async () => {
        try { const res = await axiosInstance.get('/api/admin/reports'); setReportList(res.data.data); } catch (e) {}
    };

    // --- 액션 핸들러 ---
    const handleDeleteUser = async (userId, userName) => {
        if (!window.confirm(`[${userName}] 유저를 강제 탈퇴시키겠습니까?`)) return;
        try { await axiosInstance.delete(`/api/admin/users/${userId}`); alert('삭제 완료'); fetchUsers(); } catch (e) {}
    };

    const handleDeletePost = async (postId, title) => {
        if (!window.confirm(`게시글 [${title}] 삭제하시겠습니까?`)) return;
        try { await axiosInstance.delete(`/api/admin/posts/${postId}`); alert('삭제 완료'); fetchPosts(); } catch (e) {}
    };

    const handleForceDeleteReportedItem = async (type, id, reportId) => {
        if (!window.confirm(`해당 ${type === 'POST' ? '게시글' : '댓글'}을 강제 삭제하시겠습니까?`)) return;
        try {
            if (type === 'POST') await axiosInstance.delete(`/api/admin/posts/${id}`);
            else await axiosInstance.delete(`/api/admin/comments/${id}`);
            await axiosInstance.put(`/api/admin/reports/${reportId}/resolve`);
            alert('삭제 및 처리 완료!'); fetchReports();
        } catch (e) { alert('처리 실패'); }
    };

    const handleResolveReport = async (reportId) => {
        if (!window.confirm('이상 없음으로 처리하시겠습니까?')) return;
        try { await axiosInstance.put(`/api/admin/reports/${reportId}/resolve`); alert('완료!'); fetchReports(); } catch (e) {}
    };

    const handleSyncPolicies = async () => {
        if (!window.confirm('정책 동기화를 시작할까요?')) return;
        try { await axiosInstance.post('/api/admin/policies/sync'); alert('동기화 완료!'); fetchDashboardStats(); } catch (e) {}
    };

    // --- 정렬 핸들러 ---
    const handleReportSort = (key) => {
        let direction = 'asc';
        if (reportSortConfig.key === key && reportSortConfig.direction === 'asc') direction = 'desc';
        setReportSortConfig({ key, direction });
    };

    const handlePostSort = (key) => {
        let direction = 'asc';
        if (postSortConfig.key === key && postSortConfig.direction === 'asc') direction = 'desc';
        setPostSortConfig({ key, direction });
    };

    // --- 필터 및 정렬 연산 (useMemo) ---
    const processedReports = useMemo(() => {
        let sortableItems = [...reportList];
        if (reportSearchTerm) {
            const term = reportSearchTerm.toLowerCase();
            sortableItems = sortableItems.filter(item =>
                item.reason.toLowerCase().includes(term) ||
                item.reporter.toLowerCase().includes(term) ||
                (item.targetType === 'POST' ? '게시글' : '댓글').includes(term)
            );
        }
        sortableItems.sort((a, b) => {
            const valA = a[reportSortConfig.key];
            const valB = b[reportSortConfig.key];
            if (valA < valB) return reportSortConfig.direction === 'asc' ? -1 : 1;
            if (valA > valB) return reportSortConfig.direction === 'asc' ? 1 : -1;
            return 0;
        });
        return sortableItems;
    }, [reportList, reportSearchTerm, reportSortConfig]);

    const processedPosts = useMemo(() => {
        let sortableItems = [...postList];
        if (postSearchTerm) {
            const term = postSearchTerm.toLowerCase();
            sortableItems = sortableItems.filter(item =>
                item.title.toLowerCase().includes(term) ||
                item.authorName.toLowerCase().includes(term) ||
                String(item.id).includes(term)
            );
        }
        sortableItems.sort((a, b) => {
            const valA = a[postSortConfig.key];
            const valB = b[postSortConfig.key];
            if (valA < valB) return postSortConfig.direction === 'asc' ? -1 : 1;
            if (valA > valB) return postSortConfig.direction === 'asc' ? 1 : -1;
            return 0;
        });
        return sortableItems;
    }, [postList, postSearchTerm, postSortConfig]);

    // 화면 렌더링
    if (loading) return <div className="flex min-h-screen items-center justify-center bg-gray-50 text-indigo-600 font-bold animate-pulse">관리자 확인 중... 🛡️</div>;

    return (
        <div className="min-h-screen bg-gray-50 flex">
            <aside className="w-64 bg-[#1E1B4B] text-white flex flex-col shadow-2xl z-10">
                <div className="p-6 border-b border-white/10"><h1 className="text-2xl font-black flex items-center space-x-2"><ShieldAlert className="w-7 h-7 text-indigo-400" /><span>Zoop Admin</span></h1></div>
                <nav className="flex-1 p-4 space-y-2">
                    <SidebarButton active={activeTab === 'dashboard'} onClick={() => setActiveTab('dashboard')} icon={<BarChart3 className="w-5 h-5" />} label="대시보드" />
                    <SidebarButton active={activeTab === 'users'} onClick={() => setActiveTab('users')} icon={<Users className="w-5 h-5" />} label="회원 관리" />
                    <SidebarButton active={activeTab === 'posts'} onClick={() => setActiveTab('posts')} icon={<FileText className="w-5 h-5" />} label="커뮤니티 관리" />
                    <SidebarButton active={activeTab === 'reports'} onClick={() => setActiveTab('reports')} icon={<AlertTriangle className="w-5 h-5" />} label="신고 관리 (모니터링)" />
                    <SidebarButton active={activeTab === 'policies'} onClick={() => setActiveTab('policies')} icon={<Settings className="w-5 h-5" />} label="정책 동기화" />
                </nav>
                <div className="p-4 border-t border-white/10"><button onClick={() => navigate('/')} className="flex items-center justify-center w-full py-3 text-sm font-bold text-indigo-200 hover:text-white bg-white/5 rounded-xl"><ArrowLeft className="w-4 h-4 mr-2" /> 메인으로</button></div>
            </aside>

            <main className="flex-1 overflow-y-auto p-10">
                <div className="mb-8"><h2 className="text-3xl font-black text-gray-900">{activeTab === 'dashboard' ? '대시보드' : activeTab === 'reports' ? '신고 관리' : activeTab === 'posts' ? '커뮤니티 관리' : activeTab}</h2></div>

                {/* 대시보드 탭 */}
                {activeTab === 'dashboard' && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100"><h4 className="text-sm font-bold text-gray-400 mb-2">총 유저</h4><div className="text-3xl font-black">{stats.totalUsers}명</div></div>
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100"><h4 className="text-sm font-bold text-gray-400 mb-2">오늘 게시글</h4><div className="text-3xl font-black">{stats.todayPosts}개</div></div>
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100"><h4 className="text-sm font-bold text-gray-400 mb-2">총 정책</h4><div className="text-3xl font-black">{stats.totalPolicies}개</div></div>
                    </div>
                )}

                {/* 신고 관리 탭 */}
                {activeTab === 'reports' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex justify-between items-center">
                            <h3 className="font-bold text-gray-800">대기 중인 신고 목록 ({processedReports.length})</h3>
                            <div className="relative">
                                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                                <input
                                    type="text" placeholder="분류, 사유, 신고자 검색" value={reportSearchTerm} onChange={(e) => setReportSearchTerm(e.target.value)}
                                    className="pl-9 pr-4 py-2 w-64 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                                />
                            </div>
                        </div>
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleReportSort('targetType')}><div className="flex items-center">분류 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleReportSort('reason')}><div className="flex items-center">신고사유 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleReportSort('reporter')}><div className="flex items-center">신고자 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handleReportSort('createdAt')}><div className="flex items-center">일시 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 text-center">조치</th>
                            </tr>
                            </thead>
                            <tbody>
                            {processedReports.length === 0 ? <tr><td colSpan="5" className="py-12 text-center text-gray-400 font-medium">조회된 신고 내역이 없습니다.</td></tr> :
                                processedReports.map(report => (
                                    <tr key={report.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                        <td className="px-6 py-4 font-bold"><span className={`px-2 py-1 rounded text-xs ${report.targetType === 'POST' ? 'bg-purple-100 text-purple-700' : 'bg-green-100 text-green-700'}`}>{report.targetType === 'POST' ? '게시글' : '댓글'}</span></td>
                                        <td className="px-6 py-4 text-gray-800 font-medium">{report.reason}</td>
                                        <td className="px-6 py-4">{report.reporter}</td>
                                        <td className="px-6 py-4">{report.createdAt}</td>
                                        <td className="px-6 py-4 flex justify-center space-x-2">
                                            <button onClick={() => navigate(`/community/post/${report.targetType === 'POST' ? report.targetId : ''}`)} className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors" title="원문 보기"><Eye className="w-4 h-4"/></button>
                                            <button onClick={() => handleForceDeleteReportedItem(report.targetType, report.targetId, report.id)} className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="강제 삭제"><Trash2 className="w-4 h-4"/></button>
                                            <button onClick={() => handleResolveReport(report.id)} className="p-2 text-gray-400 hover:text-emerald-500 hover:bg-emerald-50 rounded-lg transition-colors" title="이상없음 처리"><CheckCircle className="w-4 h-4"/></button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* 🚀 커뮤니티(게시글) 관리 탭 (검색/정렬/원문보기 추가) */}
                {activeTab === 'posts' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex justify-between items-center">
                            <h3 className="font-bold text-gray-800">커뮤니티 게시글 목록 ({processedPosts.length})</h3>
                            <div className="relative">
                                <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                                <input
                                    type="text" placeholder="제목, 작성자 검색" value={postSearchTerm} onChange={(e) => setPostSearchTerm(e.target.value)}
                                    className="pl-9 pr-4 py-2 w-64 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                                />
                            </div>
                        </div>
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handlePostSort('id')}><div className="flex items-center">번호 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handlePostSort('title')}><div className="flex items-center">제목 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => handlePostSort('authorName')}><div className="flex items-center">작성자 <ArrowUpDown className="w-3 h-3 ml-1" /></div></th>
                                <th className="px-6 py-4 text-center">관리 액션</th>
                            </tr>
                            </thead>
                            <tbody>
                            {processedPosts.length === 0 ? <tr><td colSpan="4" className="py-12 text-center text-gray-400 font-medium">조회된 게시글이 없습니다.</td></tr> :
                                processedPosts.map((post) => (
                                    <tr key={post.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                        <td className="px-6 py-4 font-bold text-gray-900">#{post.id}</td>
                                        <td className="px-6 py-4 text-gray-800 font-medium">{post.title}</td>
                                        <td className="px-6 py-4">{post.authorName}</td>
                                        <td className="px-6 py-4 text-center flex justify-center space-x-2">
                                            <button onClick={() => navigate(`/community/post/${post.id}`)} className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors" title="원문 보기"><Eye className="w-4 h-4"/></button>
                                            <button onClick={() => handleDeletePost(post.id, post.title)} className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="강제 삭제"><Trash2 className="w-4 h-4"/></button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* 유저 관리 탭 */}
                {activeTab === 'users' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black"><tr><th className="px-6 py-4">ID</th><th className="px-6 py-4">이름</th><th className="px-6 py-4">이메일</th><th className="px-6 py-4 text-center">관리</th></tr></thead>
                            <tbody>
                            {userList.map((user) => (
                                <tr key={user.id} className="border-b border-gray-50"><td className="px-6 py-4">#{user.id}</td><td className="px-6 py-4 font-bold">{user.name}</td><td className="px-6 py-4">{user.email}</td>
                                    <td className="px-6 py-4 text-center">{user.role !== 'ADMIN' && <button onClick={() => handleDeleteUser(user.id, user.name)} className="text-red-400 hover:text-red-600"><Trash2 className="w-5 h-5 inline" /></button>}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* 정책 동기화 탭 */}
                {activeTab === 'policies' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-12 text-center">
                        <h3 className="text-xl font-black mb-4">공공데이터 동기화</h3>
                        <button onClick={handleSyncPolicies} className="px-8 py-4 bg-indigo-600 text-white font-bold rounded-2xl">지금 동기화 시작</button>
                    </div>
                )}
            </main>
        </div>
    );
};

const SidebarButton = ({ active, onClick, icon, label }) => (
    <button onClick={onClick} className={`w-full flex items-center space-x-3 px-4 py-3.5 rounded-xl text-sm font-bold transition-all ${active ? 'bg-indigo-600 text-white shadow-lg' : 'text-indigo-200 hover:bg-white/10 hover:text-white'}`}>
        {icon}<span>{label}</span>
    </button>
);

export default AdminPage;