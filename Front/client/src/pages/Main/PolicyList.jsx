import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Star } from 'lucide-react';
import { addPolicyScrap, fetchMyScrapIds, fetchPolicies, migrateLegacyScraps, removePolicyScrap } from '../../api/policies';
import { fetchPersonalizedRecommendations } from '../../api/recommendations';

const PolicyList = ({ query, isLoggedIn }) => {
    const [policies, setPolicies] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        totalElements: 0,
    });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [allowPersonalized, setAllowPersonalized] = useState(true);
    const [likedPolicyIds, setLikedPolicyIds] = useState([]);
    const usePersonalized = isLoggedIn && !query.trim() && allowPersonalized;

    useEffect(() => {
        if (!isLoggedIn || query.trim()) {
            setAllowPersonalized(true);
        }
    }, [isLoggedIn, query]);

    useEffect(() => {
        let cancelled = false;

        if (!isLoggedIn) {
            setLikedPolicyIds([]);
            return undefined;
        }

        const loadMyScrapIds = async () => {
            try {
                await migrateLegacyScraps();
                const result = await fetchMyScrapIds();

                if (!cancelled) {
                    setLikedPolicyIds(result?.data?.serviceIds ?? []);
                }
            } catch (err) {
                if (!cancelled && err.response?.status === 401) {
                    setLikedPolicyIds([]);
                }
            }
        };

        loadMyScrapIds();

        return () => {
            cancelled = true;
        };
    }, [isLoggedIn]);

    useEffect(() => {
        let cancelled = false;

        const loadDefaultPolicies = async () => {
            const result = await fetchPolicies({ query, page: 0, size: 6 });
            const data = result?.data;

            if (cancelled) {
                return;
            }

            setPolicies(data?.items ?? []);
            setPageInfo({
                totalElements: data?.totalElements ?? 0,
            });
        };

        const loadPolicies = async () => {
            setIsLoading(true);
            setError('');

            try {
                if (usePersonalized) {
                    try {
                        const result = await fetchPersonalizedRecommendations(6);
                        const data = result?.data;

                        if (cancelled) {
                            return;
                        }

                        setPolicies(data?.items ?? []);
                        setPageInfo({
                            totalElements: data?.items?.length ?? 0,
                        });
                        return;
                    } catch (err) {
                        if (err.response?.status === 401) {
                            localStorage.removeItem('accessToken');
                            window.dispatchEvent(new Event('loginStateChange'));
                            setAllowPersonalized(false);
                            await loadDefaultPolicies();
                            return;
                        }
                        throw err;
                    }
                }

                await loadDefaultPolicies();
            } catch (err) {
                if (cancelled) {
                    return;
                }

                setError(err.response?.data?.message || '정책 목록을 불러오지 못했습니다.');
                setPolicies([]);
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
    }, [query, usePersonalized]);

    if (isLoading) {
        return <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">정책 목록을 불러오는 중입니다.</div>;
    }

    if (error) {
        return <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center text-red-600 shadow-sm">{error}</div>;
    }

    if (policies.length === 0) {
        return <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">조건에 맞는 정책이 없습니다.</div>;
    }

    const togglePolicyLike = async (serviceId) => {
        if (!isLoggedIn) {
            alert('로그인이 필요합니다.');
            return;
        }

        const isLiked = likedPolicyIds.includes(serviceId);

        try {
            if (isLiked) {
                await removePolicyScrap(serviceId);
                setLikedPolicyIds((current) => current.filter((id) => id !== serviceId));
            } else {
                await addPolicyScrap(serviceId);
                setLikedPolicyIds((current) => current.includes(serviceId) ? current : [...current, serviceId]);
            }
        } catch (err) {
            alert(err.response?.data?.message || '찜 상태를 변경하지 못했습니다.');
        }
    };

    return (
        <div className="space-y-4">
            <div className="text-sm text-gray-500">
                {usePersonalized
                    ? `회원님의 최근 관심사를 바탕으로 ${pageInfo.totalElements.toLocaleString()}건을 추천합니다.`
                    : `총 ${pageInfo.totalElements.toLocaleString()}건의 정책`}
            </div>
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {policies.map((policy) => (
                    <div key={policy.serviceId} className="relative rounded-2xl border border-gray-100 bg-white p-6 pr-14 shadow-sm transition-shadow hover:shadow-md">
                        <button
                            type="button"
                            onClick={() => togglePolicyLike(policy.serviceId)}
                            aria-label={likedPolicyIds.includes(policy.serviceId) ? '찜 해제' : '찜하기'}
                            className="absolute right-5 top-5 inline-flex h-9 w-9 items-center justify-center rounded-full text-gray-300 transition hover:bg-yellow-50 hover:text-yellow-400"
                        >
                            <Star
                                className={`h-5 w-5 ${
                                    likedPolicyIds.includes(policy.serviceId)
                                        ? 'fill-yellow-400 text-yellow-400'
                                        : ''
                                }`}
                            />
                        </button>
                        <div className="mb-4 flex items-start justify-between gap-3">
                            <span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-bold text-blue-600">{policy.serviceType || '분류 미정'}</span>
                            <span className="text-right text-sm font-bold text-orange-500">{policy.applicationDeadline || '상시'}</span>
                        </div>
                        <h3 className="mb-3 line-clamp-2 text-xl font-bold leading-tight text-gray-900">{policy.serviceName}</h3>
                        <p className="mb-4 min-h-[60px] line-clamp-3 text-sm text-gray-600">{policy.purposeSummary || '상세 설명이 아직 정리되지 않았습니다.'}</p>
                        <div className="space-y-1 text-sm text-gray-500">
                            <p>{policy.orgName || '기관 정보 없음'}</p>
                            <p>{policy.departmentName || '부서 정보 없음'}</p>
                        </div>
                        <div className="mt-5">
                            <Link
                                to={`/policies/${policy.serviceId}`}
                                className="inline-flex rounded-xl border border-blue-200 px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50"
                            >
                                자세히 보기
                            </Link>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default PolicyList;
