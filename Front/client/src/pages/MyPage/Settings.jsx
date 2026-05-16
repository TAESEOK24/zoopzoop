import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, Settings as SettingsIcon, Shield, ArrowLeft, MapPin, Briefcase } from 'lucide-react';
import axiosInstance from '../../api/index';
import { getAccessToken, setAuthSession } from '../../api/authSession';

const Settings = () => {
    const navigate = useNavigate();
    const [userInfo, setUserInfo] = useState(null);
    const [loading, setLoading] = useState(true);

    const [profileForm, setProfileForm] = useState({
        name: '',
        email: '',
        profileImageUrl: '',
        age: '',
        gender: '',
        region: '',
        district: '',
        maritalStatus: '',
        householdSize: '',
        employmentStatus: '',
        income: '',
        incomeBracket: '' // 🚀 incomePercent 대신 incomeBracket으로 변경
    });

    const [pwData, setPwData] = useState({ currentPassword: '', newPassword: '' });

    useEffect(() => {
        const fetchUserData = async () => {
            const token = getAccessToken();
            if (!token) { navigate('/login'); return; }

            try {
                const userRes = await axiosInstance.get('/api/users/me');
                const data = userRes.data.data;
                setUserInfo(data);

                setProfileForm({
                    name: data.name || '',
                    email: data.email || '',
                    profileImageUrl: data.profileImageUrl || '',
                    age: data.age || '',
                    gender: data.gender || '',
                    region: data.region || '',
                    district: data.district || '',
                    maritalStatus: data.maritalStatus || '',
                    householdSize: data.householdSize || '',
                    employmentStatus: data.employmentStatus || '',
                    income: data.income || '',
                    incomeBracket: data.incomeBracket || '' // 🚀 서버에서 받은 소득 구간
                });
            } catch (error) {
                console.error(error);
                navigate('/login');
            } finally {
                setLoading(false);
            }
        };
        fetchUserData();
    }, [navigate]);

    const handleSaveAllChanges = async () => {
        try {
            // 백엔드 DTO에 맞게 전송 (incomeBracket은 서버가 무시/덮어쓰기 하므로 같이 보내도 무방합니다)
            await axiosInstance.put('/api/users/profile', profileForm);

            if (pwData.currentPassword && pwData.newPassword) {
                await axiosInstance.put('/api/users/password', pwData);
            }

            alert("모든 맞춤 정보가 성공적으로 저장되었습니다.");
            setAuthSession({ userName: profileForm.name });
            navigate('/mypage');
        } catch (error) {
            alert(error.response?.data?.message || "수정에 실패했습니다. 입력값을 확인해주세요.");
        }
    };

    if (loading) return <div className="flex min-h-screen items-center justify-center bg-gray-50"><span className="text-xl font-bold text-blue-500">정보를 불러오는 중입니다...</span></div>;

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="mx-auto max-w-4xl px-4">

                <div className="mb-8 flex justify-between items-end">
                    <div>
                        <h1 className="text-3xl font-black tracking-tight text-gray-900">내 정보 관리</h1>
                        <p className="mt-2 font-medium text-gray-500">맞춤형 정책 추천을 위한 상세 정보 및 보안을 설정합니다.</p>
                    </div>
                    <button onClick={() => navigate('/mypage')} className="flex items-center px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm font-bold text-gray-600 hover:text-blue-600 hover:border-blue-200 transition-colors shadow-sm">
                        <ArrowLeft className="w-4 h-4 mr-2" /> 돌아가기
                    </button>
                </div>

                <div className="bg-white rounded-3xl border border-gray-100 shadow-sm p-8 lg:p-10 animate-fade-in-up space-y-12">

                    {/* 1. 기본 정보 설정 */}
                    <section>
                        <div className="flex items-center space-x-2 mb-6 border-b pb-4">
                            <SettingsIcon className="w-6 h-6 text-blue-600" />
                            <h3 className="text-xl font-black text-gray-900">기본 정보 설정</h3>
                        </div>

                        <div className="flex flex-col md:flex-row md:items-center space-y-4 md:space-y-0 md:space-x-8 mb-6">
                            <div className="relative group mx-auto md:mx-0">
                                <div className="w-24 h-24 rounded-3xl bg-gray-50 flex items-center justify-center overflow-hidden border-2 border-dashed border-gray-200">
                                    {profileForm.profileImageUrl ?
                                        <img src={profileForm.profileImageUrl} alt="preview" className="w-full h-full object-cover" /> :
                                        <Camera className="w-8 h-8 text-gray-300" />
                                    }
                                </div>
                            </div>
                            <div className="flex-1 w-full">
                                <label className="block text-xs font-black text-gray-400 mb-2">프로필 이미지 주소</label>
                                <input type="text" value={profileForm.profileImageUrl} onChange={(e) => setProfileForm({...profileForm, profileImageUrl: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-blue-500 focus:outline-none transition-all" placeholder="이미지 URL을 입력하세요" />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">닉네임</label>
                                <input type="text" value={profileForm.name} onChange={(e) => setProfileForm({...profileForm, name: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 font-bold text-gray-800 focus:bg-white focus:border-blue-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">이메일 주소</label>
                                <input type="email" value={profileForm.email} onChange={(e) => setProfileForm({...profileForm, email: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 font-bold text-gray-800 focus:bg-white focus:border-blue-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">나이</label>
                                <input type="number" value={profileForm.age} onChange={(e) => setProfileForm({...profileForm, age: e.target.value})} placeholder="만 나이 입력" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-blue-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">성별</label>
                                <select value={profileForm.gender} onChange={(e) => setProfileForm({...profileForm, gender: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-blue-500 focus:outline-none transition-all">
                                    <option value="">선택해주세요</option>
                                    <option value="MALE">남성</option>
                                    <option value="FEMALE">여성</option>
                                </select>
                            </div>
                        </div>
                    </section>

                    {/* 2. 거주 및 가구 정보 */}
                    <section>
                        <div className="flex items-center space-x-2 mb-6 border-b pb-4">
                            <MapPin className="w-6 h-6 text-emerald-500" />
                            <h3 className="text-xl font-black text-gray-900">거주 및 가구 정보</h3>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">거주 지역 (시/도)</label>
                                <input type="text" value={profileForm.region} onChange={(e) => setProfileForm({...profileForm, region: e.target.value})} placeholder="예: 서울특별시, 경기도" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-emerald-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">거주 지역 (구/군)</label>
                                <input type="text" value={profileForm.district} onChange={(e) => setProfileForm({...profileForm, district: e.target.value})} placeholder="예: 강남구, 수원시" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-emerald-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">가구원 수</label>
                                <input type="number" value={profileForm.householdSize} onChange={(e) => setProfileForm({...profileForm, householdSize: e.target.value})} placeholder="본인 포함 가구원 수 (명)" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-emerald-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">결혼 여부</label>
                                <select value={profileForm.maritalStatus} onChange={(e) => setProfileForm({...profileForm, maritalStatus: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-emerald-500 focus:outline-none transition-all">
                                    <option value="">선택해주세요</option>
                                    <option value="SINGLE">미혼</option>
                                    <option value="MARRIED">기혼</option>
                                </select>
                            </div>
                        </div>
                    </section>

                    {/* 3. 경제 활동 정보 */}
                    <section>
                        <div className="flex items-center space-x-2 mb-6 border-b pb-4">
                            <Briefcase className="w-6 h-6 text-orange-500" />
                            <h3 className="text-xl font-black text-gray-900">경제 활동 정보</h3>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">고용 상태</label>
                                <select value={profileForm.employmentStatus} onChange={(e) => setProfileForm({...profileForm, employmentStatus: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-orange-500 focus:outline-none transition-all">
                                    <option value="">선택해주세요</option>
                                    <option value="EMPLOYED">재직중</option>
                                    <option value="UNEMPLOYED">구직중/무직</option>
                                    <option value="STUDENT">학생</option>
                                    <option value="FREELANCER">프리랜서</option>
                                    <option value="ENTREPRENEUR">개인사업자</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">연 소득 (만원 단위)</label>
                                <input type="number" value={profileForm.income} onChange={(e) => setProfileForm({...profileForm, income: e.target.value})} placeholder="예: 3000" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3 text-sm focus:bg-white focus:border-orange-500 focus:outline-none transition-all" />
                            </div>

                            {/* 🚀 소득 구간: 읽기 전용 UI로 변경 */}
                            <div>
                                <div className="flex items-center mb-2">
                                    <label className="block text-xs font-black text-gray-400">소득 구간</label>
                                    <span className="ml-2 px-1.5 py-0.5 bg-gray-200 text-[10px] font-bold text-gray-500 rounded-md">자동 계산</span>
                                </div>
                                <input
                                    type="text"
                                    readOnly
                                    value={profileForm.incomeBracket ? `${profileForm.incomeBracket} 구간` : ''}
                                    placeholder="저장 시 자동 계산"
                                    className="w-full bg-gray-100 border border-gray-200 text-gray-500 font-bold rounded-2xl px-4 py-3 text-sm cursor-not-allowed focus:outline-none"
                                />
                            </div>
                        </div>
                    </section>

                    {/* 4. 보안 및 비밀번호 변경 */}
                    <section>
                        <div className="flex items-center space-x-2 mb-6 border-b pb-4">
                            <Shield className="w-6 h-6 text-indigo-600" />
                            <h3 className="text-xl font-black text-gray-900">보안 및 비밀번호</h3>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">현재 비밀번호</label>
                                <input type="password" placeholder="현재 비밀번호 (변경 시에만 입력)" value={pwData.currentPassword} onChange={(e) => setPwData({...pwData, currentPassword: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3.5 text-sm focus:bg-white focus:border-indigo-500 focus:outline-none transition-all" />
                            </div>
                            <div>
                                <label className="block text-xs font-black text-gray-400 mb-2">새 비밀번호</label>
                                <input type="password" placeholder="변경할 새 비밀번호" value={pwData.newPassword} onChange={(e) => setPwData({...pwData, newPassword: e.target.value})} className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-4 py-3.5 text-sm focus:bg-white focus:border-indigo-500 focus:outline-none transition-all" />
                            </div>
                        </div>
                    </section>

                    <div className="pt-8 flex justify-end">
                        <button onClick={handleSaveAllChanges} className="bg-blue-600 hover:bg-blue-700 text-white font-black py-4 px-12 rounded-2xl shadow-xl shadow-blue-200 transition-all transform active:scale-95">
                            모든 맞춤정보 저장하기
                        </button>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default Settings;