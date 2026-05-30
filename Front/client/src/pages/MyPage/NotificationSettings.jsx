import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BadgePlus, Bell, ChevronLeft, Clock, Save, Sparkles } from 'lucide-react';
import { fetchNotificationSettings, updateNotificationSettings } from '../../api/notifications';
import { getAccessToken } from '../../api/authSession';

const DEFAULT_SETTINGS = {
    deadlineSoon: true,
    newPolicy: true,
    recommendedPolicy: true,
};

const NotificationSettings = () => {
    const navigate = useNavigate();
    const [settings, setSettings] = useState(DEFAULT_SETTINGS);
    const [savedMessage, setSavedMessage] = useState('');
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            navigate('/login');
            return;
        }

        const loadSettings = async () => {
            try {
                const result = await fetchNotificationSettings();

                if (!cancelled) {
                    setSettings({
                        ...DEFAULT_SETTINGS,
                        ...(result?.data ?? {}),
                    });
                }
            } catch (error) {
                if (!cancelled) {
                    alert(error.response?.data?.message || '알림 설정을 불러오지 못했습니다.');
                    navigate('/mypage');
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };

        loadSettings();

        return () => {
            cancelled = true;
        };
    }, [navigate]);

    const handleToggle = (key) => {
        setSettings((current) => ({
            ...current,
            [key]: !current[key],
        }));
        setSavedMessage('');
    };

    const handleSave = async () => {
        try {
            const result = await updateNotificationSettings({
                deadlineSoon: settings.deadlineSoon,
                newPolicy: settings.newPolicy,
                recommendedPolicy: settings.recommendedPolicy,
            });

            setSettings({
                ...DEFAULT_SETTINGS,
                ...(result?.data ?? {}),
            });
            setSavedMessage('알림 설정이 저장되었습니다.');
        } catch (error) {
            setSavedMessage('');
            alert(error.response?.data?.message || '알림 설정을 저장하지 못했습니다.');
        }
    };

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-gray-50">
                <span className="text-xl font-bold text-blue-500">알림 설정을 불러오는 중입니다...</span>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="mx-auto max-w-4xl px-4">
                <button
                    onClick={() => navigate('/mypage')}
                    className="mb-6 inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-bold text-gray-600 transition-colors hover:border-blue-200 hover:text-blue-600"
                >
                    <ChevronLeft className="h-4 w-4" />
                    마이페이지로 돌아가기
                </button>

                <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
                    <div className="border-b border-gray-100 p-8">
                        <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
                            <div>
                                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
                                    <Bell className="h-6 w-6" />
                                </div>
                                <h1 className="text-3xl font-black tracking-tight text-gray-900">알림 설정</h1>
                                <p className="mt-2 text-sm font-medium text-gray-500">
                                    받고 싶은 정책 알림 종류를 선택하세요.
                                </p>
                            </div>
                            <button
                                onClick={handleSave}
                                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-700"
                            >
                                <Save className="h-4 w-4" />
                                저장
                            </button>
                        </div>

                        {savedMessage && (
                            <p className="mt-5 rounded-xl bg-green-50 px-4 py-3 text-sm font-bold text-green-700">
                                {savedMessage}
                            </p>
                        )}
                    </div>

                    <div className="space-y-8 p-8">
                        <section>
                            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-gray-400">알림 종류</h2>
                            <div className="space-y-3">
                                <SettingRow
                                    icon={Clock}
                                    title="신청 마감 임박 정책"
                                    description="마감일이 가까운 정책을 놓치지 않도록 알려줍니다."
                                    checked={settings.deadlineSoon}
                                    onToggle={() => handleToggle('deadlineSoon')}
                                />
                                <SettingRow
                                    icon={BadgePlus}
                                    title="신규 등록 정책"
                                    description="새로 등록된 정책을 빠르게 확인할 수 있습니다."
                                    checked={settings.newPolicy}
                                    onToggle={() => handleToggle('newPolicy')}
                                />
                                <SettingRow
                                    icon={Sparkles}
                                    title="개인 맞춤 추천 정책"
                                    description="최근 조회와 검색 이력을 바탕으로 맞춤 정책을 추천합니다."
                                    checked={settings.recommendedPolicy}
                                    onToggle={() => handleToggle('recommendedPolicy')}
                                />
                            </div>
                        </section>
                    </div>
                </div>
            </div>
        </div>
    );
};

const SettingRow = ({ icon: Icon, title, description, checked, onToggle }) => (
    <div className="flex items-center justify-between gap-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
        <div className="flex min-w-0 items-start gap-3">
            <div className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                <Icon className="h-5 w-5" />
            </div>
            <div className="min-w-0">
                <p className="font-bold text-gray-900">{title}</p>
                <p className="mt-1 text-sm leading-5 text-gray-500">{description}</p>
            </div>
        </div>
        <button
            type="button"
            onClick={onToggle}
            aria-pressed={checked}
            className={`relative h-7 w-12 shrink-0 rounded-full transition-colors ${checked ? 'bg-blue-600' : 'bg-gray-200'}`}
        >
            <span className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow-sm transition-all ${checked ? 'left-6' : 'left-1'}`} />
        </button>
    </div>
);

export default NotificationSettings;
