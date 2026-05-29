import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    AlertTriangle,
    ArrowLeft,
    ArrowUpDown,
    BarChart3,
    CheckCircle,
    Eye,
    FileText,
    Search,
    Settings,
    ShieldAlert,
    Trash2,
    UserPlus,
    Users,
} from 'lucide-react';
import axiosInstance from '../../api/index';

const roleLabels = {
    ALL: '전체',
    USER: '일반 회원',
    ADMIN: '관리자',
};

const AdminPage = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('dashboard');
    const [loading, setLoading] = useState(true);

    const [stats, setStats] = useState({ totalUsers: 0, todayPosts: 0, totalPolicies: 0 });
    const [userList, setUserList] = useState([]);
    const [postList, setPostList] = useState([]);
    const [reportList, setReportList] = useState([]);

    const [userSearchTerm, setUserSearchTerm] = useState('');
    const [userRoleFilter, setUserRoleFilter] = useState('ALL');
    const [userSortConfig, setUserSortConfig] = useState({ key: 'id', direction: 'desc' });

    const [reportSearchTerm, setReportSearchTerm] = useState('');
    const [reportSortConfig, setReportSortConfig] = useState({ key: 'createdAt', direction: 'desc' });

    const [postSearchTerm, setPostSearchTerm] = useState('');
    const [postSortConfig, setPostSortConfig] = useState({ key: 'id', direction: 'desc' });

    useEffect(() => {
        const checkAdmin = async () => {
            try {
                const res = await axiosInstance.get('/api/users/me');
                if (res.data.data.role !== 'ADMIN') {
                    alert('관리자 전용 페이지입니다.');
                    navigate('/');
                    return;
                }

                setLoading(false);
                fetchDashboardStats();
            } catch (error) {
                navigate('/login');
            }
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
        try {
            const res = await axiosInstance.get('/api/admin/dashboard');
            setStats(res.data.data);
        } catch (e) {}
    };

    const fetchUsers = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/users');
            setUserList(res.data.data);
        } catch (e) {}
    };

    const fetchPosts = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/posts');
            setPostList(res.data.data);
        } catch (e) {}
    };

    const fetchReports = async () => {
        try {
            const res = await axiosInstance.get('/api/admin/reports');
            setReportList(res.data.data);
        } catch (e) {}
    };

    const handleDeleteUser = async (userId, userName) => {
        if (!window.confirm(`[${userName}] 회원을 강제 탈퇴시키겠습니까?`)) return;

        try {
            await axiosInstance.delete(`/api/admin/users/${userId}`);
            alert('회원 삭제가 완료되었습니다.');
            fetchUsers();
            fetchDashboardStats();
        } catch (e) {
            alert('회원 삭제에 실패했습니다.');
        }
    };

    const handleGrantAdmin = async (userId, userName) => {
        if (!window.confirm(`[${userName}] 회원에게 관리자 권한을 지급하시겠습니까?`)) return;

        try {
            const res = await axiosInstance.put(`/api/admin/users/${userId}/grant-admin`);
            const updatedUser = res.data.data;
            setUserList((users) => users.map((user) => (user.id === updatedUser.id ? updatedUser : user)));
            alert('관리자 권한을 지급했습니다.');
        } catch (e) {
            alert('관리자 권한 지급에 실패했습니다.');
        }
    };

    const handleDeletePost = async (postId, title) => {
        if (!window.confirm(`게시글 [${title}]을 삭제하시겠습니까?`)) return;

        try {
            await axiosInstance.delete(`/api/admin/posts/${postId}`);
            alert('게시글 삭제가 완료되었습니다.');
            fetchPosts();
        } catch (e) {
            alert('게시글 삭제에 실패했습니다.');
        }
    };

    const handleForceDeleteReportedItem = async (type, id, reportId) => {
        if (!window.confirm(`해당 ${type === 'POST' ? '게시글' : '댓글'}을 강제 삭제하시겠습니까?`)) return;

        try {
            if (type === 'POST') await axiosInstance.delete(`/api/admin/posts/${id}`);
            else await axiosInstance.delete(`/api/admin/comments/${id}`);

            await axiosInstance.put(`/api/admin/reports/${reportId}/resolve`);
            alert('삭제 및 신고 처리가 완료되었습니다.');
            fetchReports();
        } catch (e) {
            alert('신고 처리에 실패했습니다.');
        }
    };

    const handleResolveReport = async (reportId) => {
        if (!window.confirm('이상 없음으로 처리하시겠습니까?')) return;

        try {
            await axiosInstance.put(`/api/admin/reports/${reportId}/resolve`);
            alert('신고 처리가 완료되었습니다.');
            fetchReports();
        } catch (e) {
            alert('신고 처리에 실패했습니다.');
        }
    };

    const handleSyncPolicies = async () => {
        if (!window.confirm('정책 데이터 동기화를 시작하시겠습니까?')) return;

        try {
            await axiosInstance.post('/api/admin/policies/sync');
            alert('정책 동기화가 완료되었습니다.');
            fetchDashboardStats();
        } catch (e) {
            alert('정책 동기화에 실패했습니다.');
        }
    };

    const handleSort = (currentConfig, setConfig, key) => {
        const direction = currentConfig.key === key && currentConfig.direction === 'asc' ? 'desc' : 'asc';
        setConfig({ key, direction });
    };

    const sortItems = (items, sortConfig) => {
        return [...items].sort((a, b) => {
            const valA = a[sortConfig.key] ?? '';
            const valB = b[sortConfig.key] ?? '';

            if (valA < valB) return sortConfig.direction === 'asc' ? -1 : 1;
            if (valA > valB) return sortConfig.direction === 'asc' ? 1 : -1;
            return 0;
        });
    };

    const processedUsers = useMemo(() => {
        let items = [...userList];
        const term = userSearchTerm.trim().toLowerCase();

        if (userRoleFilter !== 'ALL') {
            items = items.filter((user) => user.role === userRoleFilter);
        }

        if (term) {
            items = items.filter((user) =>
                String(user.id).includes(term) ||
                (user.name ?? '').toLowerCase().includes(term) ||
                (user.email ?? '').toLowerCase().includes(term) ||
                (user.role ?? '').toLowerCase().includes(term)
            );
        }

        return sortItems(items, userSortConfig);
    }, [userList, userSearchTerm, userRoleFilter, userSortConfig]);

    const processedReports = useMemo(() => {
        let items = [...reportList];
        const term = reportSearchTerm.trim().toLowerCase();

        if (term) {
            items = items.filter((item) =>
                (item.reason ?? '').toLowerCase().includes(term) ||
                (item.reporter ?? '').toLowerCase().includes(term) ||
                (item.targetType === 'POST' ? '게시글' : '댓글').includes(term)
            );
        }

        return sortItems(items, reportSortConfig);
    }, [reportList, reportSearchTerm, reportSortConfig]);

    const processedPosts = useMemo(() => {
        let items = [...postList];
        const term = postSearchTerm.trim().toLowerCase();

        if (term) {
            items = items.filter((item) =>
                String(item.id).includes(term) ||
                (item.title ?? '').toLowerCase().includes(term) ||
                (item.authorName ?? '').toLowerCase().includes(term)
            );
        }

        return sortItems(items, postSortConfig);
    }, [postList, postSearchTerm, postSortConfig]);

    const getTitle = () => {
        if (activeTab === 'dashboard') return '대시보드';
        if (activeTab === 'users') return '회원 관리';
        if (activeTab === 'posts') return '커뮤니티 관리';
        if (activeTab === 'reports') return '신고 관리';
        if (activeTab === 'policies') return '정책 동기화';
        return '관리자';
    };

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-gray-50 text-indigo-600 font-bold animate-pulse">
                관리자 권한 확인 중...
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 flex">
            <aside className="w-64 bg-[#1E1B4B] text-white flex flex-col shadow-2xl z-10">
                <div className="p-6 border-b border-white/10">
                    <h1 className="text-2xl font-black flex items-center space-x-2">
                        <ShieldAlert className="w-7 h-7 text-indigo-400" />
                        <span>Zoop Admin</span>
                    </h1>
                </div>
                <nav className="flex-1 p-4 space-y-2">
                    <SidebarButton active={activeTab === 'dashboard'} onClick={() => setActiveTab('dashboard')} icon={<BarChart3 className="w-5 h-5" />} label="대시보드" />
                    <SidebarButton active={activeTab === 'users'} onClick={() => setActiveTab('users')} icon={<Users className="w-5 h-5" />} label="회원 관리" />
                    <SidebarButton active={activeTab === 'posts'} onClick={() => setActiveTab('posts')} icon={<FileText className="w-5 h-5" />} label="커뮤니티 관리" />
                    <SidebarButton active={activeTab === 'reports'} onClick={() => setActiveTab('reports')} icon={<AlertTriangle className="w-5 h-5" />} label="신고 관리" />
                    <SidebarButton active={activeTab === 'policies'} onClick={() => setActiveTab('policies')} icon={<Settings className="w-5 h-5" />} label="정책 동기화" />
                </nav>
                <div className="p-4 border-t border-white/10">
                    <button onClick={() => navigate('/')} className="flex items-center justify-center w-full py-3 text-sm font-bold text-indigo-200 hover:text-white bg-white/5 rounded-xl">
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        메인으로
                    </button>
                </div>
            </aside>

            <main className="flex-1 overflow-y-auto p-10">
                <div className="mb-8">
                    <h2 className="text-3xl font-black text-gray-900">{getTitle()}</h2>
                </div>

                {activeTab === 'dashboard' && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        <StatCard label="총 회원" value={`${stats.totalUsers}명`} />
                        <StatCard label="오늘 게시글" value={`${stats.todayPosts}개`} />
                        <StatCard label="총 정책" value={`${stats.totalPolicies}개`} />
                    </div>
                )}

                {activeTab === 'users' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                            <h3 className="font-bold text-gray-800">회원 목록 ({processedUsers.length})</h3>
                            <div className="flex flex-col gap-3 sm:flex-row">
                                <div className="relative">
                                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                                    <input
                                        type="text"
                                        placeholder="ID, 이름, 이메일 검색"
                                        value={userSearchTerm}
                                        onChange={(e) => setUserSearchTerm(e.target.value)}
                                        className="pl-9 pr-4 py-2 w-full sm:w-72 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                                    />
                                </div>
                                <select
                                    value={userRoleFilter}
                                    onChange={(e) => setUserRoleFilter(e.target.value)}
                                    className="px-4 py-2 border border-gray-200 rounded-xl text-sm font-bold text-gray-700 bg-white focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
                                >
                                    <option value="ALL">전체 권한</option>
                                    <option value="USER">일반 회원</option>
                                    <option value="ADMIN">관리자</option>
                                </select>
                            </div>
                        </div>
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black border-b border-gray-100">
                            <tr>
                                <SortableHeader label="ID" sortKey="id" onSort={(key) => handleSort(userSortConfig, setUserSortConfig, key)} />
                                <SortableHeader label="이름" sortKey="name" onSort={(key) => handleSort(userSortConfig, setUserSortConfig, key)} />
                                <SortableHeader label="이메일" sortKey="email" onSort={(key) => handleSort(userSortConfig, setUserSortConfig, key)} />
                                <SortableHeader label="권한" sortKey="role" onSort={(key) => handleSort(userSortConfig, setUserSortConfig, key)} />
                                <SortableHeader label="가입일" sortKey="createdAt" onSort={(key) => handleSort(userSortConfig, setUserSortConfig, key)} />
                                <th className="px-6 py-4 text-center">관리</th>
                            </tr>
                            </thead>
                            <tbody>
                            {processedUsers.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="py-12 text-center text-gray-400 font-medium">조회된 회원이 없습니다.</td>
                                </tr>
                            ) : processedUsers.map((user) => (
                                <tr key={user.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                    <td className="px-6 py-4 font-bold text-gray-900">#{user.id}</td>
                                    <td className="px-6 py-4 font-bold text-gray-800">{user.name}</td>
                                    <td className="px-6 py-4">{user.email}</td>
                                    <td className="px-6 py-4">
                                        <span className={`px-2 py-1 rounded text-xs font-bold ${user.role === 'ADMIN' ? 'bg-indigo-100 text-indigo-700' : 'bg-gray-100 text-gray-600'}`}>
                                            {roleLabels[user.role] ?? user.role}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4">{formatDate(user.createdAt)}</td>
                                    <td className="px-6 py-4">
                                        <div className="flex justify-center space-x-2">
                                            {user.role !== 'ADMIN' && (
                                                <button onClick={() => handleGrantAdmin(user.id, user.name)} className="p-2 text-indigo-500 hover:bg-indigo-50 rounded-lg transition-colors" title="관리자 지급">
                                                    <UserPlus className="w-4 h-4" />
                                                </button>
                                            )}
                                            {user.role !== 'ADMIN' && (
                                                <button onClick={() => handleDeleteUser(user.id, user.name)} className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="강제 탈퇴">
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {activeTab === 'posts' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <TableToolbar title={`커뮤니티 게시글 목록 (${processedPosts.length})`} value={postSearchTerm} onChange={setPostSearchTerm} placeholder="번호, 제목, 작성자 검색" />
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black border-b border-gray-100">
                            <tr>
                                <SortableHeader label="번호" sortKey="id" onSort={(key) => handleSort(postSortConfig, setPostSortConfig, key)} />
                                <SortableHeader label="제목" sortKey="title" onSort={(key) => handleSort(postSortConfig, setPostSortConfig, key)} />
                                <SortableHeader label="작성자" sortKey="authorName" onSort={(key) => handleSort(postSortConfig, setPostSortConfig, key)} />
                                <th className="px-6 py-4 text-center">관리</th>
                            </tr>
                            </thead>
                            <tbody>
                            {processedPosts.length === 0 ? (
                                <tr><td colSpan="4" className="py-12 text-center text-gray-400 font-medium">조회된 게시글이 없습니다.</td></tr>
                            ) : processedPosts.map((post) => (
                                <tr key={post.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                    <td className="px-6 py-4 font-bold text-gray-900">#{post.id}</td>
                                    <td className="px-6 py-4 text-gray-800 font-medium">{post.title}</td>
                                    <td className="px-6 py-4">{post.authorName}</td>
                                    <td className="px-6 py-4">
                                        <div className="flex justify-center space-x-2">
                                            <button onClick={() => navigate(`/community/post/${post.id}`)} className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors" title="원문 보기"><Eye className="w-4 h-4" /></button>
                                            <button onClick={() => handleDeletePost(post.id, post.title)} className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="강제 삭제"><Trash2 className="w-4 h-4" /></button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {activeTab === 'reports' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                        <TableToolbar title={`대기 중인 신고 목록 (${processedReports.length})`} value={reportSearchTerm} onChange={setReportSearchTerm} placeholder="분류, 사유, 신고자 검색" />
                        <table className="w-full text-left text-sm text-gray-500">
                            <thead className="bg-gray-50 text-xs text-gray-400 uppercase font-black border-b border-gray-100">
                            <tr>
                                <SortableHeader label="분류" sortKey="targetType" onSort={(key) => handleSort(reportSortConfig, setReportSortConfig, key)} />
                                <SortableHeader label="신고 사유" sortKey="reason" onSort={(key) => handleSort(reportSortConfig, setReportSortConfig, key)} />
                                <SortableHeader label="신고자" sortKey="reporter" onSort={(key) => handleSort(reportSortConfig, setReportSortConfig, key)} />
                                <SortableHeader label="일시" sortKey="createdAt" onSort={(key) => handleSort(reportSortConfig, setReportSortConfig, key)} />
                                <th className="px-6 py-4 text-center">조치</th>
                            </tr>
                            </thead>
                            <tbody>
                            {processedReports.length === 0 ? (
                                <tr><td colSpan="5" className="py-12 text-center text-gray-400 font-medium">조회된 신고 내역이 없습니다.</td></tr>
                            ) : processedReports.map((report) => (
                                <tr key={report.id} className="border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                                    <td className="px-6 py-4 font-bold">
                                        <span className={`px-2 py-1 rounded text-xs ${report.targetType === 'POST' ? 'bg-purple-100 text-purple-700' : 'bg-green-100 text-green-700'}`}>
                                            {report.targetType === 'POST' ? '게시글' : '댓글'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-gray-800 font-medium">{report.reason}</td>
                                    <td className="px-6 py-4">{report.reporter}</td>
                                    <td className="px-6 py-4">{report.createdAt}</td>
                                    <td className="px-6 py-4">
                                        <div className="flex justify-center space-x-2">
                                            <button onClick={() => navigate(`/community/post/${report.targetType === 'POST' ? report.targetId : ''}`)} className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition-colors" title="원문 보기"><Eye className="w-4 h-4" /></button>
                                            <button onClick={() => handleForceDeleteReportedItem(report.targetType, report.targetId, report.id)} className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="강제 삭제"><Trash2 className="w-4 h-4" /></button>
                                            <button onClick={() => handleResolveReport(report.id)} className="p-2 text-gray-400 hover:text-emerald-500 hover:bg-emerald-50 rounded-lg transition-colors" title="이상 없음 처리"><CheckCircle className="w-4 h-4" /></button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {activeTab === 'policies' && (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-12 text-center">
                        <h3 className="text-xl font-black mb-4">공공데이터 정책 동기화</h3>
                        <button onClick={handleSyncPolicies} className="px-8 py-4 bg-indigo-600 text-white font-bold rounded-2xl hover:bg-indigo-700 transition-colors">
                            지금 동기화 시작
                        </button>
                    </div>
                )}
            </main>
        </div>
    );
};

const formatDate = (value) => {
    if (!value) return '-';
    return String(value).replace('T', ' ').slice(0, 16);
};

const StatCard = ({ label, value }) => (
    <div className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
        <h4 className="text-sm font-bold text-gray-400 mb-2">{label}</h4>
        <div className="text-3xl font-black">{value}</div>
    </div>
);

const TableToolbar = ({ title, value, onChange, placeholder }) => (
    <div className="p-6 border-b border-gray-100 bg-gray-50/50 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h3 className="font-bold text-gray-800">{title}</h3>
        <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
                type="text"
                placeholder={placeholder}
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="pl-9 pr-4 py-2 w-full sm:w-72 border border-gray-200 rounded-xl text-sm focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
            />
        </div>
    </div>
);

const SortableHeader = ({ label, sortKey, onSort }) => (
    <th className="px-6 py-4 cursor-pointer hover:text-indigo-600 transition-colors" onClick={() => onSort(sortKey)}>
        <div className="flex items-center">
            {label}
            <ArrowUpDown className="w-3 h-3 ml-1" />
        </div>
    </th>
);

const SidebarButton = ({ active, onClick, icon, label }) => (
    <button onClick={onClick} className={`w-full flex items-center space-x-3 px-4 py-3.5 rounded-xl text-sm font-bold transition-all ${active ? 'bg-indigo-600 text-white shadow-lg' : 'text-indigo-200 hover:bg-white/10 hover:text-white'}`}>
        {icon}
        <span>{label}</span>
    </button>
);

export default AdminPage;
