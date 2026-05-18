import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Search, Star, Trash2 } from 'lucide-react';
import { fetchMyScraps, migrateLegacyScraps, removePolicyScrap } from '../../api/policies';
import { getAccessToken } from '../../api/authSession';

const PAGE_SIZE = 12;

const parsePage = (value) => {
    const parsed = Number.parseInt(value ?? '1', 10);
    return Number.isNaN(parsed) || parsed < 1 ? 1 : parsed;
};

const buildPageNumbers = (currentPage, totalPages) => {
    if (totalPages <= 0) {
        return [];
    }

    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages - 1, start + 4);
    const adjustedStart = Math.max(0, end - 4);

    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
};

const ScrapsPage = () => {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const urlPage = parsePage(searchParams.get('page'));
    const urlQuery = searchParams.get('query') ?? '';
    const [queryInput, setQueryInput] = useState(urlQuery);
    const [policies, setPolicies] = useState([]);
    const [pageInfo, setPageInfo] = useState({ page: 0, totalElements: 0, totalPages: 0, hasNext: false });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const pageNumbers = useMemo(() => buildPageNumbers(pageInfo.page, pageInfo.totalPages), [pageInfo.page, pageInfo.totalPages]);

    useEffect(() => {
        setQueryInput(urlQuery);
    }, [urlQuery]);

    useEffect(() => {
        let cancelled = false;
        const token = getAccessToken();

        if (!token) {
            alert('로그인이 필요합니다.');
            navigate('/login');
            return undefined;
        }

        const loadScraps = async () => {
            setIsLoading(true);
            setError('');

            try {
                await migrateLegacyScraps();
                const result = await fetchMyScraps({
                    query: urlQuery,
                    page: urlPage - 1,
                    size: PAGE_SIZE,
                });

                if (cancelled) {
                    return;
                }

                const data = result?.data;
                setPolicies(data?.items ?? []);
                setPageInfo({
                    page: data?.page ?? 0,
                    totalElements: data?.totalElements ?? 0,
                    totalPages: data?.totalPages ?? 0,
                    hasNext: data?.hasNext ?? false,
                });
            } catch (err) {
                if (!cancelled) {
                    setError(err.response?.data?.message || '찜한 정책을 불러오지 못했습니다.');
                    setPolicies([]);
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };

        loadScraps();

        return () => {
            cancelled = true;
        };
    }, [navigate, urlPage, urlQuery]);

    const updateUrl = ({ page = 0, query = urlQuery }) => {
        const nextParams = new URLSearchParams();
        nextParams.set('page', String(page + 1));

        if (query) {
            nextParams.set('query', query);
        }

        setSearchParams(nextParams);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        updateUrl({ page: 0, query: queryInput.trim() });
    };

    const movePage = (nextPage) => {
        const boundedPage = Math.min(Math.max(nextPage, 0), Math.max(pageInfo.totalPages - 1, 0));
        updateUrl({ page: boundedPage, query: urlQuery });
    };

    const handleRemoveScrap = async (serviceId) => {
        try {
            await removePolicyScrap(serviceId);
            setPolicies((current) => current.filter((policy) => policy.serviceId !== serviceId));
            setPageInfo((current) => ({
                ...current,
                totalElements: Math.max(current.totalElements - 1, 0),
            }));
        } catch (err) {
            alert(err.response?.data?.message || '찜을 해제하지 못했습니다.');
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="mx-auto max-w-6xl px-4">
                <button
                    onClick={() => navigate('/mypage')}
                    className="mb-6 inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2 text-sm font-bold text-gray-600 transition-colors hover:border-blue-200 hover:text-blue-600"
                >
                    <ChevronLeft className="h-4 w-4" />
                    마이페이지로 돌아가기
                </button>

                <div className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
                    <div>
                        <div className="mb-3 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-yellow-50 text-yellow-500">
                            <Star className="h-6 w-6 fill-yellow-400" />
                        </div>
                        <h1 className="text-3xl font-black tracking-tight text-gray-900">찜한 정책</h1>
                        <p className="mt-2 text-sm font-medium text-gray-500">관심 있는 정책을 검색하고 정리하세요.</p>
                    </div>
                    <div className="rounded-2xl border border-gray-100 bg-white px-5 py-3 text-sm font-bold text-gray-600 shadow-sm">
                        총 {pageInfo.totalElements.toLocaleString()}개
                    </div>
                </div>

                <section className="mb-6 rounded-3xl border border-gray-100 bg-white p-5 shadow-sm">
                    <form onSubmit={handleSubmit} className="flex flex-col gap-3 md:flex-row">
                        <div className="relative flex-1">
                            <Search className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
                            <input
                                value={queryInput}
                                onChange={(event) => setQueryInput(event.target.value)}
                                placeholder="정책명, 기관, 부서로 검색"
                                className="h-12 w-full rounded-2xl border border-gray-200 bg-white pl-12 pr-4 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-50"
                            />
                        </div>
                        <button className="h-12 rounded-2xl bg-blue-600 px-6 text-sm font-bold text-white transition hover:bg-blue-700">
                            검색
                        </button>
                    </form>
                </section>

                {isLoading && <div className="rounded-3xl border border-gray-100 bg-white p-10 text-center text-gray-500 shadow-sm">찜한 정책을 불러오는 중입니다.</div>}
                {!isLoading && error && <div className="rounded-3xl border border-red-100 bg-red-50 p-10 text-center text-red-600 shadow-sm">{error}</div>}
                {!isLoading && !error && policies.length === 0 && <div className="rounded-3xl border border-gray-100 bg-white p-10 text-center text-gray-500 shadow-sm">조건에 맞는 찜한 정책이 없습니다.</div>}

                {!isLoading && !error && policies.length > 0 && (
                    <>
                        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
                            {policies.map((policy) => (
                                <article key={policy.serviceId} className="relative rounded-3xl border border-gray-100 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
                                    <button
                                        type="button"
                                        onClick={() => handleRemoveScrap(policy.serviceId)}
                                        className="absolute right-5 top-5 inline-flex h-9 w-9 items-center justify-center rounded-full text-gray-300 transition hover:bg-red-50 hover:text-red-500"
                                        aria-label="찜 해제"
                                    >
                                        <Trash2 className="h-5 w-5" />
                                    </button>
                                    <div className="pr-10">
                                        <div className="mb-3 flex flex-wrap gap-2">
                                            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-bold text-blue-600">{policy.serviceType || '정책'}</span>
                                            <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-500">{policy.applicationDeadline || '상시'}</span>
                                        </div>
                                        <h2 className="line-clamp-2 text-lg font-black leading-7 text-gray-900">{policy.serviceName}</h2>
                                        <p className="mt-3 h-16 overflow-hidden text-sm leading-6 text-gray-500">{policy.purposeSummary || '정책 설명이 아직 정리되지 않았습니다.'}</p>
                                        <div className="mt-4 space-y-1 text-sm text-gray-500">
                                            <p className="font-semibold text-gray-700">{policy.orgName || '기관 정보 없음'}</p>
                                            <p>{policy.departmentName || '부서 정보 없음'}</p>
                                        </div>
                                        <Link
                                            to={`/policies/${policy.serviceId}`}
                                            className="mt-5 inline-flex w-full justify-center rounded-2xl border border-blue-200 px-4 py-3 text-sm font-bold text-blue-700 transition hover:bg-blue-50"
                                        >
                                            자세히 보기
                                        </Link>
                                    </div>
                                </article>
                            ))}
                        </div>

                        <div className="mt-8 flex items-center justify-center gap-2">
                            <button
                                onClick={() => movePage(pageInfo.page - 1)}
                                disabled={pageInfo.page === 0}
                                className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-600 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                <ChevronLeft className="h-5 w-5" />
                            </button>
                            {pageNumbers.map((page) => (
                                <button
                                    key={page}
                                    onClick={() => movePage(page)}
                                    className={`h-11 min-w-11 rounded-full px-4 text-sm font-bold transition ${
                                        page === pageInfo.page
                                            ? 'bg-gray-900 text-white'
                                            : 'border border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
                                    }`}
                                >
                                    {page + 1}
                                </button>
                            ))}
                            <button
                                onClick={() => movePage(pageInfo.page + 1)}
                                disabled={!pageInfo.hasNext}
                                className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-600 transition hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                <ChevronRight className="h-5 w-5" />
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default ScrapsPage;
