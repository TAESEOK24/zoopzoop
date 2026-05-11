import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, ChevronRight, ExternalLink, FileCheck, Heart, Mail, User, X, MessageSquare, Shield, Bell, Camera } from 'lucide-react';
import axiosInstance from '../../api/index';
import { fetchMyScraps, migrateLegacyScraps } from '../../api/policies';

const appliedPolicies = [
    { id: 101, title: '청년 내일 채움 공제', date: '2026.04.10', status: '심사 중', color: 'text-blue-600 bg-blue-50' },
    { id: 102, title: '경기도 청년 기본 소득', date: '2026.03.15', status: '지급 완료', color: 'text-green-600 bg-green-50' },
];

const MyPage = () => {
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState(null);
    const [likedPolicies, setLikedPolicies] = useState([]);
    const [likedPolicyTotal, setLikedPolicyTotal] = useState(0);
    const [myActivities, setMyActivities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('liked'); // 기본 탭: 찜한 정책

    // 모달 상태 관리
    const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
    const [profileForm, setProfileForm] = useState({ name: '', email: '', profileImageUrl: '' });
    const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
    const [pwData, setPwData] = useState({ currentPassword: '', newPassword: '' });

    useEffect(() => {
        const fetchAllData = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) { navigate('/login'); return; }

            try {
                // 1. 내 정보 불러오기
                const userRes = await axiosInstance.get('/api/users/me');
                const data = userRes.data.data;
                setUserInfo(data);
                setProfileForm({ name: data.name, email: data.email, profileImageUrl: data.profileImageUrl || '' });

                // 2. 찜한 정책
                await migrateLegacyScraps();
                const scrapRes = await fetchMyScraps({ page: 0, size: 5 });
                setLikedPolicies(scrapRes?.data?.items ?? []);
                setLikedPolicyTotal(scrapRes?.data?.totalElements ?? 0);

                // 3. 내가 쓴 글/댓글
                const [postsRes, commentsRes] = await Promise.all([
                    axiosInstance.get('/api/community/my-posts'),
                    axiosInstance.get('/api/community/my-comments')
                ]);
                const combined = [
                    ...(postsRes.data.data || []).map(p => ({ ...p, type: '게시글', realId: p.id })),
                    ...(commentsRes.data.data || []).map(c => ({ ...c, type: '댓글', title: c.content, realId: c.postId }))
                ].sort((a, b) => b.date.localeCompare(a.date));
                setMyActivities(combined);
            } catch (error) { console.error(error); } finally { setLoading(false); }
        };
        fetchAllData();
    }, [navigate]);

    const handleUpdateProfile = async () => {
        try {
            await axiosInstance.put('/api/users/profile', profileForm);
            alert("프로필 정보가 업데이트되었습니다.");
            localStorage.setItem('userName', profileForm.name);
            window.location.reload();
        } catch (error) { alert("업데이트 실패"); }
    };

    const handleChangePassword = async () => {
        try {
            await axiosInstance.put('/api/users/password', pwData);
            alert("비밀번호가 변경되었습니다.");
            setIsPasswordModalOpen(false);
        } catch (error) { alert("현재 비밀번호가 올바르지 않습니다."); }
    };

    const handleDeleteAccount = async () => {
        if (!window.confirm('정말로 탈퇴하시겠습니까?')) return;
        try {
            await axiosInstance.delete('/api/users/me');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userName');
            window.dispatchEvent(new Event('loginStateChange'));
            alert('회원 탈퇴가 완료되었습니다.');
            navigate('/');
        } catch (error) { alert('탈퇴 실패'); }
    };

    if (loading) return <div className="flex min-h-screen items-center justify-center bg-gray-50"><span className="text-xl font-bold text-blue-500">정보를 불러오는 중입니다...</span></div>;
    if (!userInfo) return null;

    // 가입일 포맷팅
    const joinDate = userInfo.createdAt ? new Date(userInfo.createdAt).toLocaleDateString() : '2026.04.27';

    return (
        <div className="min-h-screen bg-gray-50 py-12 relative">
            <div className="mx-auto max-w-4xl px-4">
                <div className="mb-10">
                    <h1 className="text-3xl font-black tracking-tight text-gray-900">마이페이지</h1>
                    <p className="mt-2 font-medium text-gray-500">계정 정보와 활동 내역을 확인하세요.</p>
                </div>

                <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
                    {/* 왼쪽: 프로필 및 계정 관리 */}
                    <div className="space-y-6 lg:col-span-1">
                        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
                            <div className="bg-blue-600 p-8 text-center text-white">
                                <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-2xl border border-white/30 bg-white/20 overflow-hidden">
                                    {userInfo.profileImageUrl ?
                                        <img src={userInfo.profileImageUrl} alt="profile" className="w-full h-full object-cover" /> :
                                        <User className="h-10 w-10 text-white" />
                                    }
                                </div>
                                <h2 className="text-xl font-bold">{userInfo.name}님</h2>
                                <p className="mt-1 text-sm text-blue-100 opacity-80">{userInfo.role === 'ADMIN' ? '관리자' : '일반 회원'}</p>
                            </div>
                            <div className="space-y-4 p-6">
                                <div className="flex items-center space-x-3 text-sm">
                                    <Mail className="h-4 w-4 text-gray-400" />
                                    <span className="truncate text-gray-600">{userInfo.email}</span>
                                </div>
                                {/* 🚀 가입일 추가됨 */}
                                <div className="flex items-center space-x-3 text-sm">
                                    <Calendar className="h-4 w-4 text-gray-400" />
                                    <span className="text-gray-600">{joinDate} 가입</span>
                                </div>
                                <button onClick={() => setIsProfileModalOpen(true)} className="mt-4 w-full rounded-xl bg-blue-50 py-2 text-sm font-bold text-blue-600 hover:bg-blue-100 transition-colors">
                                    프로필 수정
                                </button>
                            </div>
                        </div>

                        <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
                            <h3 className="mb-4 text-sm font-black uppercase tracking-widest text-gray-400">계정 관리</h3>
                            <div className="space-y-1">
                                <MenuButton label="비밀번호 변경" onClick={() => setIsPasswordModalOpen(true)} />
                                <MenuButton label="알림 설정" onClick={() => navigate('/mypage/notifications')} />
                                <button onClick={handleDeleteAccount} className="w-full rounded-xl p-3 text-left text-sm font-bold text-red-400 hover:bg-red-50 transition-colors mt-2">회원 탈퇴</button>
                            </div>
                        </div>
                    </div>

                    {/* 오른쪽: 🚀 3개의 활동 탭 및 배너 */}
                    <div className="space-y-6 lg:col-span-2">
                        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
                            <div className="flex border-b border-gray-100">
                                <TabButton active={activeTab === 'liked'} onClick={() => setActiveTab('liked')} icon={<Heart className={`h-4 w-4 ${activeTab === 'liked' ? 'fill-blue-600' : ''}`} />} label={`찜한 정책 (${likedPolicyTotal})`} />
                                <TabButton active={activeTab === 'applied'} onClick={() => setActiveTab('applied')} icon={<FileCheck className="h-4 w-4" />} label="신청 현황" />
                                <TabButton active={activeTab === 'community'} onClick={() => setActiveTab('community')} icon={<MessageSquare className="h-4 w-4" />} label={`나의 활동 (${myActivities.length})`} />
                            </div>

                            <div className="p-6">
                                {activeTab === 'liked' && (
                                    <div className="space-y-4">
                                        {likedPolicies.map((p) => (
                                            <div key={p.serviceId} onClick={() => navigate(`/policies/${p.serviceId}`)} className="group flex items-center justify-between rounded-2xl bg-gray-50 p-4 hover:border-blue-200 hover:bg-white border border-transparent cursor-pointer transition-all">
                                                <div>
                                                    <span className="rounded bg-blue-100 px-2 py-0.5 text-[10px] font-black text-blue-700">{p.serviceType || '정책'}</span>
                                                    <h4 className="font-bold text-gray-900 mt-1">{p.serviceName}</h4>
                                                </div>
                                                <ChevronRight className="h-5 w-5 text-gray-300 group-hover:text-blue-500" />
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {activeTab === 'applied' && (
                                    <div className="space-y-4">
                                        {appliedPolicies.map((item) => (
                                            <div key={item.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-2xl border border-transparent">
                                                <div className="flex items-center space-x-4">
                                                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center font-bold text-xs ${item.color}`}>{item.status.split(' ')[0]}</div>
                                                    <div>
                                                        <h4 className="font-bold text-gray-900">{item.title}</h4>
                                                        <p className="text-xs text-gray-400 mt-1">신청일: {item.date}</p>
                                                    </div>
                                                </div>
                                                <ExternalLink className="h-4 w-4 text-gray-300" />
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {activeTab === 'community' && (
                                    <div className="space-y-4">
                                        {myActivities.length === 0 && <p className="text-center text-sm text-gray-400 py-6">활동 내역이 없습니다.</p>}
                                        {myActivities.map(a => (
                                            <div key={`${a.type}-${a.id}`} onClick={() => navigate(`/community/post/${a.realId}`)} className="group flex items-center justify-between rounded-2xl border border-transparent bg-gray-50 p-4 transition-all hover:border-blue-200 hover:bg-white cursor-pointer">
                                                <div className="flex flex-col">
                                                    <div className="flex items-center mb-1">
                                                        <span className={`text-[10px] font-black px-2 py-0.5 rounded mr-2 ${a.type === '게시글' ? 'bg-purple-100 text-purple-700' : 'bg-green-100 text-green-700'}`}>{a.type}</span>
                                                        <span className="text-[10px] text-gray-400 font-medium">{a.date}</span>
                                                    </div>
                                                    <h4 className="font-bold text-gray-900 line-clamp-1">{a.title}</h4>
                                                </div>
                                                <ChevronRight className="h-5 w-5 text-gray-300 group-hover:text-blue-500" />
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 추천 알림 배너 */}
                        <div className="bg-indigo-900 rounded-3xl p-8 text-white relative overflow-hidden group shadow-lg">
                            <div className="relative z-10">
                                <h4 className="text-xl font-bold mb-2">맞춤형 정책 알림을 받아보세요! 🔔</h4>
                                <p className="text-indigo-200 text-sm font-medium opacity-90">관심 정책의 새로운 소식을 가장 먼저 알려드립니다.</p>
                                <button onClick={() => navigate('/mypage/notifications')} className="mt-6 px-6 py-2 bg-white text-indigo-900 font-bold rounded-xl text-sm hover:bg-indigo-50 transition-colors">알림 설정하기</button>
                            </div>
                            <div className="absolute -right-10 -bottom-10 w-40 h-40 bg-white/10 rounded-full blur-3xl group-hover:scale-150 transition-all duration-700"></div>
                        </div>
                    </div>
                </div>
            </div>

            {/* 통합 수정 모달 */}
            {isProfileModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm px-4">
                    <div className="bg-white rounded-3xl p-8 w-full max-w-md shadow-2xl relative">
                        <button onClick={() => setIsProfileModalOpen(false)} className="absolute top-6 right-6 text-gray-400 hover:text-gray-600"><X /></button>
                        <h3 className="text-2xl font-black mb-8 text-gray-900">프로필 정보 수정</h3>
                        <div className="space-y-5">
                            <div>
                                <label className="block text-xs font-black text-gray-400 uppercase mb-2">프로필 이미지 URL</label>
                                <input type="text" value={profileForm.profileImageUrl} onChange={(e) => setProfileForm({...profileForm, profileImageUrl: e.target.value})} className="w-full bg-gray-50 border rounded-xl px-4 py-3 text-sm focus:outline-blue-500" placeholder="https://..." />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 uppercase mb-2">닉네임</label>
                                <input type="text" value={profileForm.name} onChange={(e) => setProfileForm({...profileForm, name: e.target.value})} className="w-full bg-gray-50 border rounded-xl px-4 py-3 text-sm focus:outline-blue-500" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 uppercase mb-2">이메일 주소</label>
                                <input type="email" value={profileForm.email} onChange={(e) => setProfileForm({...profileForm, email: e.target.value})} className="w-full bg-gray-50 border rounded-xl px-4 py-3 text-sm focus:outline-blue-500" />
                            </div>
                        </div>
                        <button onClick={handleUpdateProfile} className="w-full mt-10 bg-blue-600 text-white font-bold py-4 rounded-2xl hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all">수정 내용 저장</button>
                    </div>
                </div>
            )}

            {/* 비밀번호 모달 */}
            {isPasswordModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm px-4">
                    <div className="bg-white rounded-3xl p-8 w-full max-w-sm shadow-2xl relative">
                        <button onClick={() => setIsPasswordModalOpen(false)} className="absolute top-5 right-5 text-gray-400 hover:text-gray-600"><X /></button>
                        <h3 className="text-xl font-bold mb-6">비밀번호 변경</h3>
                        <div className="space-y-4 mb-6">
                            <input type="password" placeholder="현재 비밀번호" value={pwData.currentPassword} onChange={(e) => setPwData({...pwData, currentPassword: e.target.value})} className="w-full bg-gray-50 border rounded-xl px-4 py-3 focus:outline-blue-500" />
                            <input type="password" placeholder="새 비밀번호" value={pwData.newPassword} onChange={(e) => setPwData({...pwData, newPassword: e.target.value})} className="w-full bg-gray-50 border rounded-xl px-4 py-3 focus:outline-blue-500" />
                        </div>
                        <button onClick={handleChangePassword} className="w-full bg-blue-600 text-white font-bold py-3 rounded-xl hover:bg-blue-700 transition-colors">변경 실행</button>
                    </div>
                </div>
            )}
        </div>
    );
};

const TabButton = ({ active, onClick, icon, label }) => (
    <button onClick={onClick} className={`flex-1 flex items-center justify-center space-x-2 py-4 text-sm font-bold transition-all ${active ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/30' : 'text-gray-400 hover:bg-gray-50'}`}>
        {icon} <span>{label}</span>
    </button>
);

const MenuButton = ({ label, onClick }) => (
    <button onClick={onClick} className="w-full flex items-center justify-between p-3.5 rounded-xl hover:bg-gray-50 transition-colors group">
        <span className="text-gray-600 text-sm font-bold group-hover:text-blue-600">{label}</span>
        <ChevronRight className="w-4 h-4 text-gray-300 group-hover:text-blue-500 group-hover:translate-x-1 transition-all" />
    </button>
);

export default MyPage;