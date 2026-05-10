import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, ChevronRight, ExternalLink, FileCheck, Heart, Mail, User } from 'lucide-react';
import axiosInstance from '../../api/index';

const likedPolicies = [
    { id: 1, title: '청년 월세 지원 사업', category: '주거', agency: '국토교통부', dDay: 'D-15' },
    { id: 2, title: 'K-Pass 교통비 환급', category: '교통', agency: '국토교통부', dDay: '상시' },
    { id: 3, title: '청년 희망 적금', category: '금융', agency: '금융위원회', dDay: 'D-5' },
];

const appliedPolicies = [
    { id: 101, title: '청년 내일 채움 공제', date: '2026.04.10', status: '심사 중', color: 'text-blue-600 bg-blue-50' },
    { id: 102, title: '경기도 청년 기본 소득', date: '2026.03.15', status: '지급 완료', color: 'text-green-600 bg-green-50' },
];

const MyPage = () => {
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('liked');

    useEffect(() => {
        const fetchMyInfo = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                alert('로그인이 필요합니다.');
                navigate('/login');
                return;
            }

            try {
                const response = await axiosInstance.get('/api/users/me');
                setUserInfo(response.data.data);
            } catch (error) {
                console.error('사용자 정보 조회 실패:', error);
                alert('사용자 정보를 불러오지 못했습니다.');
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };

        fetchMyInfo();
    }, [navigate]);

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-gray-50">
                <span className="text-xl font-bold text-blue-500">정보를 불러오는 중입니다...</span>
            </div>
        );
    }

    if (!userInfo) {
        return null;
    }

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="mx-auto max-w-4xl px-4">
                <div className="mb-10">
                    <h1 className="text-3xl font-black tracking-tight text-gray-900">마이페이지</h1>
                    <p className="mt-2 font-medium text-gray-500">계정 정보와 활동 내역을 확인하세요.</p>
                </div>

                <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
                    <div className="space-y-6 lg:col-span-1">
                        <ProfileCard userInfo={userInfo} />

                        <div className="rounded-3xl border border-gray-100 bg-white p-6 shadow-sm">
                            <h3 className="mb-4 text-sm font-black uppercase tracking-widest text-gray-400">계정 관리</h3>
                            <div className="space-y-1">
                                <MenuButton label="비밀번호 변경" />
                                <MenuButton label="알림 설정" onClick={() => navigate('/mypage/notifications')} />
                                <button className="w-full rounded-xl p-3 text-left text-sm font-bold text-red-400 transition-colors hover:bg-red-50">
                                    회원 탈퇴
                                </button>
                            </div>
                        </div>
                    </div>

                    <div className="space-y-6 lg:col-span-2">
                        <ActivityPanel activeTab={activeTab} setActiveTab={setActiveTab} />

                        <div className="relative overflow-hidden rounded-3xl bg-indigo-900 p-8 text-white">
                            <div className="relative z-10">
                                <h4 className="mb-2 text-xl font-bold">맞춤 정책 알림을 받아보세요</h4>
                                <p className="text-sm font-medium text-indigo-200">
                                    관심 알림을 설정하면 새 정책과 마감 임박 정책을 더 빠르게 확인할 수 있습니다.
                                </p>
                                <button
                                    onClick={() => navigate('/mypage/notifications')}
                                    className="mt-6 rounded-xl bg-white px-6 py-2 text-sm font-bold text-indigo-900 transition-colors hover:bg-indigo-50"
                                >
                                    알림 설정으로 이동
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const ProfileCard = ({ userInfo }) => (
    <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
        <div className="bg-blue-600 p-8 text-center text-white">
            <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-2xl border border-white/30 bg-white/20">
                <User className="h-10 w-10 text-white" />
            </div>
            <h2 className="text-xl font-bold">{userInfo.name}</h2>
            <p className="mt-1 text-sm text-blue-100 opacity-80">{userInfo.role === 'ADMIN' ? '관리자' : '일반 회원'}</p>
        </div>
        <div className="space-y-4 p-6">
            <div className="flex items-center space-x-3 text-sm">
                <Mail className="h-4 w-4 text-gray-400" />
                <span className="truncate text-gray-600">{userInfo.email}</span>
            </div>
            <div className="flex items-center space-x-3 text-sm">
                <Calendar className="h-4 w-4 text-gray-400" />
                <span className="text-gray-600">가입 회원</span>
            </div>
            <button className="mt-4 w-full rounded-xl bg-blue-50 py-2 text-sm font-bold text-blue-600 transition-colors hover:bg-blue-100">
                프로필 수정
            </button>
        </div>
    </div>
);

const ActivityPanel = ({ activeTab, setActiveTab }) => (
    <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
        <div className="flex border-b border-gray-100">
            <button
                onClick={() => setActiveTab('liked')}
                className={`flex flex-1 items-center justify-center space-x-2 py-4 text-sm font-bold transition-all ${
                    activeTab === 'liked' ? 'border-b-2 border-blue-600 bg-blue-50/30 text-blue-600' : 'text-gray-400 hover:bg-gray-50'
                }`}
            >
                <Heart className={`h-4 w-4 ${activeTab === 'liked' ? 'fill-blue-600' : ''}`} />
                <span>찜한 정책 ({likedPolicies.length})</span>
            </button>
            <button
                onClick={() => setActiveTab('applied')}
                className={`flex flex-1 items-center justify-center space-x-2 py-4 text-sm font-bold transition-all ${
                    activeTab === 'applied' ? 'border-b-2 border-blue-600 bg-blue-50/30 text-blue-600' : 'text-gray-400 hover:bg-gray-50'
                }`}
            >
                <FileCheck className="h-4 w-4" />
                <span>신청 현황 ({appliedPolicies.length})</span>
            </button>
        </div>

        <div className="p-6">
            {activeTab === 'liked' ? (
                <div className="space-y-4">
                    {likedPolicies.map((policy) => (
                        <div key={policy.id} className="group flex items-center justify-between rounded-2xl border border-transparent bg-gray-50 p-4 transition-all hover:border-blue-200 hover:bg-white">
                            <div>
                                <div className="mb-1 flex items-center space-x-2">
                                    <span className="rounded bg-blue-100 px-2 py-0.5 text-[10px] font-black uppercase text-blue-700">{policy.category}</span>
                                    <span className="text-[10px] font-bold text-red-500">{policy.dDay}</span>
                                </div>
                                <h4 className="font-bold text-gray-900">{policy.title}</h4>
                                <p className="mt-1 text-xs text-gray-400">{policy.agency}</p>
                            </div>
                            <button className="p-2 text-gray-300 group-hover:text-blue-500">
                                <ChevronRight className="h-5 w-5" />
                            </button>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="space-y-4">
                    {appliedPolicies.map((item) => (
                        <div key={item.id} className="flex items-center justify-between rounded-2xl border border-gray-100 p-4">
                            <div className="flex items-center space-x-4">
                                <div className={`flex h-12 w-12 items-center justify-center rounded-xl text-xs font-bold ${item.color}`}>
                                    {item.status.split(' ')[0]}
                                </div>
                                <div>
                                    <h4 className="font-bold text-gray-900">{item.title}</h4>
                                    <p className="mt-1 text-xs text-gray-400">신청일 {item.date}</p>
                                </div>
                            </div>
                            <button className="flex items-center space-x-1 text-xs font-bold text-gray-400 hover:text-blue-600">
                                <span>상세보기</span>
                                <ExternalLink className="h-3 w-3" />
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    </div>
);

const MenuButton = ({ label, onClick }) => (
    <button onClick={onClick} className="group flex w-full items-center justify-between rounded-xl p-3 transition-colors hover:bg-gray-50">
        <span className="text-sm font-bold text-gray-600 group-hover:text-blue-600">{label}</span>
        <ChevronRight className="h-4 w-4 text-gray-300 transition-all group-hover:translate-x-1 group-hover:text-blue-500" />
    </button>
);

export default MyPage;
