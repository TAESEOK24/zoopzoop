import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { fetchPolicies } from '../../api/policies';
import { fetchProfileBasedRecommendations } from '../../api/recommendations';

const ProfileRecommendationList = ({ isLoggedIn }) => {
    const navigate = useNavigate();
    const [policies, setPolicies] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [displayMode, setDisplayMode] = useState(isLoggedIn ? 'profile' : 'latest');

    useEffect(() => {
        let cancelled = false;

        const loadLatestPolicies = async () => {
            const result = await fetchPolicies({ page: 0, size: 3, sort: 'latest' });
            return result?.data?.items ?? [];
        };

        const loadPolicies = async () => {
            setIsLoading(true);
            setError('');
            setDisplayMode(isLoggedIn ? 'profile' : 'latest');

            try {
                if (!isLoggedIn) {
                    const latestItems = await loadLatestPolicies();
                    if (!cancelled) {
                        setDisplayMode('latest');
                        setPolicies(latestItems);
                    }
                    return;
                }

                const result = await fetchProfileBasedRecommendations(3);
                const data = result?.data;

                if (cancelled) {
                    return;
                }

                if (data?.profileReady && data?.items?.length > 0) {
                    setDisplayMode('profile');
                    setPolicies(data.items);
                    return;
                }

                const latestItems = await loadLatestPolicies();
                if (!cancelled) {
                    setDisplayMode(data?.profileReady ? 'profileFallback' : 'profileMissing');
                    setPolicies(latestItems);
                }
            } catch (err) {
                if (!cancelled && err.response?.status === 401) {
                    localStorage.removeItem('accessToken');
                    window.dispatchEvent(new Event('loginStateChange'));
                    const latestItems = await loadLatestPolicies();
                    if (!cancelled) {
                        setDisplayMode('latest');
                        setPolicies(latestItems);
                    }
                    return;
                }

                if (!cancelled) {
                    setError(err.response?.data?.message || '추천 정책을 불러오지 못했습니다.');
                    setPolicies([]);
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };

        loadPolicies();

        return () => {
            cancelled = true;
        };
    }, [isLoggedIn]);

    const title = displayMode === 'profile' ? '내 정보 기반 추천 정책' : '새로 등록된 정책';
    const description = {
        profile: '마이페이지에 저장된 나이, 지역, 소득 구간을 바탕으로 추천했어요.',
        profileFallback: '조건에 딱 맞는 정책이 부족해 최근 등록 정책을 함께 보여드려요.',
        profileMissing: '마이페이지 정보를 더 입력하면 더 정확한 추천을 받을 수 있어요.',
        latest: '로그인하면 내 정보에 맞는 정책을 추천받을 수 있어요.',
    }[displayMode];

    return (
        <section className="mb-16">
            <div className="mb-6 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                <div>
                    <h2 className="text-2xl font-bold text-gray-800">{title}</h2>
                    <p className="mt-2 text-sm font-medium text-gray-500">{description}</p>
                </div>

                {displayMode === 'profileMissing' && (
                    <button
                        type="button"
                        onClick={() => navigate('/mypage/settings')}
                        className="inline-flex items-center gap-2 rounded-xl border border-emerald-200 bg-white px-4 py-2 text-sm font-bold text-emerald-700 transition hover:bg-emerald-50"
                    >
                        내 정보 입력하기
                        <ArrowRight className="h-4 w-4" />
                    </button>
                )}
            </div>

            {isLoading && (
                <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">
                    추천 정책을 불러오는 중입니다.
                </div>
            )}

            {!isLoading && error && (
                <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center text-red-600 shadow-sm">
                    {error}
                </div>
            )}

            {!isLoading && !error && policies.length === 0 && (
                <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">
                    표시할 정책이 없습니다.
                </div>
            )}

            {!isLoading && !error && policies.length > 0 && (
                <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
                    {policies.map((policy) => (
                        <article key={policy.serviceId} className="flex min-h-[260px] flex-col rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
                            <div className="mb-3 flex flex-wrap gap-2">
                                <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-600">
                                    {policy.serviceType || '정책'}
                                </span>
                                <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-500">
                                    {policy.applicationDeadline || '상시'}
                                </span>
                            </div>

                            <h3 className="line-clamp-2 text-lg font-black leading-7 text-gray-900">{policy.serviceName}</h3>
                            <p className="mt-3 line-clamp-3 text-sm leading-6 text-gray-500">
                                {policy.purposeSummary || '정책 설명이 아직 정리되지 않았습니다.'}
                            </p>
                            <div className="mt-4 space-y-1 text-sm text-gray-500">
                                <p className="font-semibold text-gray-700">{policy.orgName || '기관 정보 없음'}</p>
                                <p>{policy.departmentName || '부서 정보 없음'}</p>
                            </div>
                            <Link
                                to={`/policies/${policy.serviceId}`}
                                className="mt-auto inline-flex items-center justify-center rounded-xl border border-blue-200 px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50"
                            >
                                자세히 보기
                            </Link>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
};

export default ProfileRecommendationList;
