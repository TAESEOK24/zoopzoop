import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, ChevronRight, Heart, Mail, User, MessageSquare, Bell, ShieldAlert } from 'lucide-react';
import axiosInstance from '../../api/index';
import { fetchMyScraps, migrateLegacyScraps } from '../../api/policies';

const MyPage = () => {
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState(null);
    const [likedPolicies, setLikedPolicies] = useState([]);
    const [likedPolicyTotal, setLikedPolicyTotal] = useState(0);
    const [myActivities, setMyActivities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('liked');

    useEffect(() => {
        const fetchAllData = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) { navigate('/login'); return; }

            try {
                const userRes = await axiosInstance.get('/api/users/me');
                setUserInfo(userRes.data.data);

                await migrateLegacyScraps();
                const scrapRes = await fetchMyScraps({ page: 0, size: 5 });
                setLikedPolicies(scrapRes?.data?.items ?? []);
                setLikedPolicyTotal(scrapRes?.data?.totalElements ?? 0);

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

    // 🚀 안전한 회원 탈퇴 로직 (특정 문구 입력)
    const handleDeleteAccount = async () => {
        const confirmText = window.prompt('정말로 탈퇴하시겠습니까?\n탈퇴를 원하시면 "탈퇴동의" 라고 입력해주세요.');

        // 취소를 누르거나 빈 칸인 경우 방어
        if (confirmText === null) return;

        // 정확한 텍스트를 입력하지 않은 경우
        if (confirmText !== '탈퇴동의') {
            alert('입력한 문구가 일치하지 않습니다. 탈퇴가 취소되었습니다.');
            return;
        }

        try {
            await axiosInstance.delete('/api/users/me');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userName');
            window.dispatchEvent(new Event('loginStateChange'));
            alert('회원 탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.');
            navigate('/');
        } catch (error) {
            alert('탈퇴 처리에 실패했습니다.');
        }
    };

    if (loading) return <div className="flex min-h-screen items-center justify-center bg-gray-50"><span className="text-xl font-bold text-blue-500">정보를 불러오는 중입니다...</span></div>;
    if (!userInfo) return null;

    const joinDate = userInfo.createdAt ? new Date(userInfo.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }) : '2026년 04월 27일';

    return (
        <div className="min-h-screen bg-gray-50 py-12 relative">
            <div className="mx-auto max-w-4xl px-4">
                <div className="mb-10">
                    <h1 className="text-3xl font-black tracking-tight text-gray-900">마이페이지</h1>
                    <p className="mt-2 font-medium text-gray-500">계정 정보와 활동 내역을 확인하세요.</p>
                </div>

                <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
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
                                <div className="flex items-center space-x-3 text-sm">
                                    <Calendar className="h-4 w-4 text-gray-400" />
                                    <span className="text-gray-600">{joinDate} 가입</span>
                                </div>
                            </div>
                        </div>

                        <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
                            <h3 className="mb-4 text-sm font-black uppercase text-gray-400">계정 설정</h3>
                            <div className="space-y-1">
                                <MenuButton label="내 정보 관리" onClick={() => navigate('/mypage/settings')} />
                                <MenuButton label="알림 설정" onClick={() => navigate('/mypage/notifications')} />

                                {/* 🚀 ADMIN 권한일 때만 보이는 관리자 메뉴 (알림 설정 바로 아래) */}
                                {userInfo.role === 'ADMIN' && (
                                    <MenuButton
                                        label="👑 관리자 대시보드"
                                        onClick={() => navigate('/admin')}
                                        className="bg-indigo-50 border border-indigo-100 mt-2"
                                        labelClassName="text-indigo-700 group-hover:text-indigo-800"
                                        iconClassName="text-indigo-400 group-hover:text-indigo-600"
                                    />
                                )}

                                <button onClick={handleDeleteAccount} className="w-full text-left p-4 mt-2 text-sm font-bold text-red-400 hover:text-red-500 hover:bg-red-50 rounded-2xl transition-colors">
                                    회원 탈퇴하기
                                </button>
                            </div>
                        </div>
                    </div>

                    <div className="space-y-6 lg:col-span-2">
                        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
                            <div className="flex border-b border-gray-100">
                                <TabButton active={activeTab === 'liked'} onClick={() => setActiveTab('liked')} icon={<Heart className={`h-4 w-4 ${activeTab === 'liked' ? 'fill-blue-600' : ''}`} />} label={`찜한 정책 (${likedPolicyTotal})`} />
                                <TabButton active={activeTab === 'community'} onClick={() => setActiveTab('community')} icon={<MessageSquare className="h-4 w-4" />} label={`나의 활동 (${myActivities.length})`} />
                            </div>

                            <div className="p-6 min-h-[300px]">
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
                                        {likedPolicies.length === 0 && <p className="text-center text-sm text-gray-400 py-10">찜한 정책이 없습니다.</p>}
                                    </div>
                                )}

                                {activeTab === 'community' && (
                                    <div className="space-y-4">
                                        {myActivities.length === 0 && <p className="text-center text-sm text-gray-400 py-10">활동 내역이 없습니다.</p>}
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
        </div>
    );
};

const TabButton = ({ active, onClick, icon, label }) => (
    <button onClick={onClick} className={`flex-1 flex items-center justify-center space-x-2 py-4 text-sm font-bold transition-all ${active ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/30' : 'text-gray-400 hover:bg-gray-50'}`}>
        {icon} <span>{label}</span>
    </button>
);

// 🚀 MenuButton을 조금 더 유연하게 업데이트하여 커스텀 스타일(관리자용)을 받을 수 있게 변경
const MenuButton = ({ label, onClick, className = '', labelClassName = 'text-gray-600', iconClassName = 'text-gray-300' }) => (
    <button onClick={onClick} className={`w-full flex items-center justify-between p-4 rounded-2xl hover:bg-gray-50 transition-colors group ${className}`}>
        <span className={`text-sm font-bold group-hover:text-blue-600 transition-colors ${labelClassName}`}>{label}</span>
        <ChevronRight className={`w-4 h-4 group-hover:translate-x-1 group-hover:text-blue-500 transition-all ${iconClassName}`} />
    </button>
);

export default MyPage;