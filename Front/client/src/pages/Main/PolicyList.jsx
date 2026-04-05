import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchPolicies } from '../../api/policies';

const PolicyList = ({ query }) => {
    const [policies, setPolicies] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        totalElements: 0,
    });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let cancelled = false;

        const loadPolicies = async () => {
            setIsLoading(true);
            setError('');

            try {
                const result = await fetchPolicies({ query, page: 0, size: 6 });
                const data = result?.data;

                if (cancelled) {
                    return;
                }

                setPolicies(data?.items ?? []);
                setPageInfo({
                    totalElements: data?.totalElements ?? 0,
                });
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
    }, [query]);

    if (isLoading) {
        return <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">정책 목록을 불러오는 중입니다.</div>;
    }

    if (error) {
        return <div className="rounded-2xl border border-red-100 bg-red-50 p-8 text-center text-red-600 shadow-sm">{error}</div>;
    }

    if (policies.length === 0) {
        return <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center text-gray-500 shadow-sm">조건에 맞는 정책이 없습니다.</div>;
    }

    return (
        <div className="space-y-4">
            <div className="text-sm text-gray-500">총 {pageInfo.totalElements.toLocaleString()}건의 정책</div>
            <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {policies.map((policy) => (
                    <div key={policy.serviceId} className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
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
