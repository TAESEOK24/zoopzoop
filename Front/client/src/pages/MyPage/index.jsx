import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Shield, Calendar, ChevronRight, Heart, FileCheck, ExternalLink } from 'lucide-react';
import axiosInstance from '../../api/index';

const MyPage = () => {
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('liked'); // 'liked' 또는 'applied' 상태 관리

    // 🚀 임시 데이터: 내가 찜한 정책
    const likedPolicies = [
        { id: 1, title: "청년 월세 지원 사업", category: "주거", agency: "국토교통부", dDay: "D-15" },
        { id: 2, title: "K-Pass 교통비 환급", category: "교통", agency: "국토교통부", dDay: "상시" },
        { id: 3, title: "청년 도약 계좌", category: "금융", agency: "금융위원회", dDay: "D-5" },
    ];

    // 🚀 임시 데이터: 내가 신청한 정책
    const appliedPolicies = [
        { id: 101, title: "청년 내일 채움 공제", date: "2026.04.10", status: "심사 중", color: "text-blue-600 bg-blue-50" },
        { id: 102, title: "경기도 청년 기본 소득", date: "2026.03.15", status: "지급 완료", color: "text-green-600 bg-green-50" },
    ];

    useEffect(() => {
        const fetchMyInfo = async () => {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                alert("로그인 후 이용해주세요.");
                navigate('/login');
                return;
            }
            try {
                const response = await axiosInstance.get('/api/users/me');
                setUserInfo(response.data.data);
            } catch (error) {
                console.error("내 정보 불러오기 실패:", error);
                alert("유저 정보를 불러오는데 실패했습니다.");
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };
        fetchMyInfo();
    }, [navigate]);

    if (loading) return <div className="min-h-screen flex justify-center items-center bg-gray-50"><span className="text-xl font-bold text-blue-500">정보를 불러오는 중입니다... 🚀</span></div>;
    if (!userInfo) return null;

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="max-w-4xl mx-auto px-4">
                <div className="mb-10">
                    <h1 className="text-3xl font-black text-gray-900 tracking-tight">마이페이지</h1>
                    <p className="text-gray-500 mt-2 font-medium">내 계정 정보와 활동 내역을 한눈에 확인하세요.</p>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* 왼쪽: 프로필 및 기본 정보 (1컬럼) */}
                    <div className="lg:col-span-1 space-y-6">
                        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                            <div className="bg-blue-600 p-8 text-center text-white">
                                <div className="w-20 h-20 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-4 backdrop-blur-md border border-white/30">
                                    <User className="w-10 h-10 text-white" />
                                </div>
                                <h2 className="text-xl font-bold">{userInfo.name}님</h2>
                                <p className="text-blue-100 text-sm mt-1 opacity-80">{userInfo.role === 'ADMIN' ? '관리자' : '일반 회원'}</p>
                            </div>
                            <div className="p-6 space-y-4">
                                <div className="flex items-center space-x-3 text-sm">
                                    <Mail className="w-4 h-4 text-gray-400" />
                                    <span className="text-gray-600 truncate">{userInfo.email}</span>
                                </div>
                                <div className="flex items-center space-x-3 text-sm">
                                    <Calendar className="w-4 h-4 text-gray-400" />
                                    <span className="text-gray-600">2026.04.27 가입</span>
                                </div>
                                <button className="w-full mt-4 py-2 text-sm font-bold text-blue-600 bg-blue-50 rounded-xl hover:bg-blue-100 transition-colors">
                                    프로필 수정
                                </button>
                            </div>
                        </div>

                        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100">
                            <h3 className="text-sm font-black text-gray-400 uppercase tracking-widest mb-4">계정 관리</h3>
                            <div className="space-y-1">
                                <MenuButton label="비밀번호 변경" />
                                <MenuButton label="알림 설정" />
                                <button className="w-full text-left p-3 text-sm text-red-400 font-bold hover:bg-red-50 rounded-xl transition-colors">회원 탈퇴</button>
                            </div>
                        </div>
                    </div>

                    {/* 오른쪽: 활동 내역 (2컬럼) */}
                    <div className="lg:col-span-2 space-y-6">
                        <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
                            {/* 탭 헤더 */}
                            <div className="flex border-b border-gray-100">
                                <button
                                    onClick={() => setActiveTab('liked')}
                                    className={`flex-1 py-4 text-sm font-bold flex items-center justify-center space-x-2 transition-all ${activeTab === 'liked' ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/30' : 'text-gray-400 hover:bg-gray-50'}`}
                                >
                                    <Heart className={`w-4 h-4 ${activeTab === 'liked' ? 'fill-blue-600' : ''}`} />
                                    <span>찜한 정책 ({likedPolicies.length})</span>
                                </button>
                                <button
                                    onClick={() => setActiveTab('applied')}
                                    className={`flex-1 py-4 text-sm font-bold flex items-center justify-center space-x-2 transition-all ${activeTab === 'applied' ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50/30' : 'text-gray-400 hover:bg-gray-50'}`}
                                >
                                    <FileCheck className="w-4 h-4" />
                                    <span>신청 현황 ({appliedPolicies.length})</span>
                                </button>
                            </div>

                            {/* 탭 콘텐츠 */}
                            <div className="p-6">
                                {activeTab === 'liked' ? (
                                    <div className="space-y-4">
                                        {likedPolicies.map(policy => (
                                            <div key={policy.id} className="group p-4 bg-gray-50 rounded-2xl border border-transparent hover:border-blue-200 hover:bg-white transition-all flex justify-between items-center">
                                                <div>
                                                    <div className="flex items-center space-x-2 mb-1">
                                                        <span className="px-2 py-0.5 bg-blue-100 text-blue-700 text-[10px] font-black rounded uppercase">{policy.category}</span>
                                                        <span className="text-[10px] text-red-500 font-bold">{policy.dDay}</span>
                                                    </div>
                                                    <h4 className="text-gray-900 font-bold">{policy.title}</h4>
                                                    <p className="text-xs text-gray-400 mt-1">{policy.agency}</p>
                                                </div>
                                                <button className="p-2 text-gray-300 group-hover:text-blue-500">
                                                    <ChevronRight className="w-5 h-5" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="space-y-4">
                                        {appliedPolicies.map(item => (
                                            <div key={item.id} className="p-4 border border-gray-100 rounded-2xl flex justify-between items-center">
                                                <div className="flex items-center space-x-4">
                                                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center font-bold text-xs ${item.color}`}>
                                                        {item.status.split(' ')[0]}
                                                    </div>
                                                    <div>
                                                        <h4 className="text-gray-900 font-bold">{item.title}</h4>
                                                        <p className="text-xs text-gray-400 mt-1">신청일: {item.date}</p>
                                                    </div>
                                                </div>
                                                <button className="flex items-center space-x-1 text-xs font-bold text-gray-400 hover:text-blue-600">
                                                    <span>상세보기</span>
                                                    <ExternalLink className="w-3 h-3" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 홍보 배너/팁 영역 */}
                        <div className="bg-indigo-900 rounded-3xl p-8 text-white relative overflow-hidden group">
                            <div className="relative z-10">
                                <h4 className="text-xl font-bold mb-2">맞춤형 정책 알림을 받아보세요! 🔔</h4>
                                <p className="text-indigo-200 text-sm font-medium opacity-90">
                                    관심 카테고리를 설정하면 새로운 정책이 등록될 때<br/>가장 먼저 알려드립니다.
                                </p>
                                <button className="mt-6 px-6 py-2 bg-white text-indigo-900 font-bold rounded-xl text-sm hover:bg-indigo-50 transition-colors">
                                    알림 설정하러 가기
                                </button>
                            </div>
                            <div className="absolute -right-10 -bottom-10 w-40 h-40 bg-white/10 rounded-full blur-3xl group-hover:scale-150 transition-transform duration-700"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const MenuButton = ({ label }) => (
    <button className="w-full flex items-center justify-between p-3 rounded-xl hover:bg-gray-50 transition-colors group">
        <span className="text-gray-600 text-sm font-bold group-hover:text-blue-600">{label}</span>
        <ChevronRight className="w-4 h-4 text-gray-300 group-hover:text-blue-500 group-hover:translate-x-1 transition-all" />
    </button>
);

export default MyPage;