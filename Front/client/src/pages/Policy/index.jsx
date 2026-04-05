import React, { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, RefreshCcw, Search, X } from 'lucide-react';
import { fetchPolicies, fetchPolicyTypes } from '../../api/policies';

const PAGE_SIZE = 24;
const sidebarMenus = ['정책 통합검색', '지원유형별 정책', '조건별 탐색', '상세 정보 보기'];

const regionHierarchy = {
    서울: [],
    부산: [],
    대구: [],
    인천: [],
    광주: [],
    대전: [],
    울산: [],
    세종: [],
    경기: ['수원', '성남', '고양', '용인', '부천', '안산', '안양', '남양주', '화성', '평택', '의정부', '시흥', '파주', '광명', '김포', '군포', '광주', '이천', '양주', '오산', '구리', '안성', '포천', '의왕', '하남', '여주', '양평', '동두천', '과천', '가평', '연천'],
    강원: ['춘천', '원주', '강릉', '동해', '태백', '속초', '삼척', '홍천', '횡성', '영월', '평창', '정선', '철원', '화천', '양구', '인제', '고성', '양양'],
    충북: ['청주', '충주', '제천', '보은', '옥천', '영동', '증평', '진천', '괴산', '음성', '단양'],
    충남: ['천안', '공주', '보령', '아산', '서산', '논산', '계룡', '당진', '금산', '부여', '서천', '청양', '홍성', '예산', '태안'],
    전북: ['전주', '군산', '익산', '정읍', '남원', '김제', '완주', '진안', '무주', '장수', '임실', '순창', '고창', '부안'],
    전남: ['목포', '여수', '순천', '나주', '광양', '담양', '곡성', '구례', '고흥', '보성', '화순', '장흥', '강진', '해남', '영암', '무안', '함평', '영광', '장성', '완도', '진도', '신안'],
    경북: ['포항', '경주', '김천', '안동', '구미', '영주', '영천', '상주', '문경', '경산', '의성', '청송', '영양', '영덕', '청도', '고령', '성주', '칠곡', '예천', '봉화', '울진', '울릉'],
    경남: ['창원', '진주', '통영', '사천', '김해', '밀양', '거제', '양산', '의령', '함안', '창녕', '고성', '남해', '하동', '산청', '함양', '거창', '합천'],
    제주: ['제주', '서귀포'],
};

const topLevelRegions = Object.keys(regionHierarchy);

const specialOptions = [
    { code: 'ja0201', label: '중위소득 0~50%' },
    { code: 'ja0202', label: '중위소득 51~75%' },
    { code: 'ja0203', label: '중위소득 76~100%' },
    { code: 'ja0204', label: '중위소득 101~200%' },
    { code: 'ja0205', label: '중위소득 200% 초과' },
    { code: 'ja0317', label: '초등학생' },
    { code: 'ja0318', label: '중학생' },
    { code: 'ja0319', label: '고등학생' },
    { code: 'ja0320', label: '대학생/대학원생' },
    { code: 'ja0403', label: '한부모/조손가정' },
    { code: 'ja0401', label: '다문화가족' },
    { code: 'ja0402', label: '북한이탈주민' },
    { code: 'ja0404', label: '1인가구' },
    { code: 'ja0411', label: '다자녀가구' },
    { code: 'ja0412', label: '무주택세대' },
    { code: 'ja0413', label: '신규전입' },
    { code: 'ja0414', label: '확대가족' },
    { code: 'ja0301', label: '예비부모/난임' },
    { code: 'ja0302', label: '임산부' },
    { code: 'ja0303', label: '출산/입양' },
    { code: 'ja0326', label: '근로자/직장인' },
    { code: 'ja0327', label: '구직자/실업자' },
    { code: 'ja0328', label: '장애인' },
    { code: 'ja0329', label: '국가보훈대상자' },
    { code: 'ja0330', label: '질병/질환자' },
];

specialOptions.push(
    { code: 'ja0313', label: '농업인' },
    { code: 'ja0314', label: '어업인' },
    { code: 'ja0315', label: '축산업인' },
    { code: 'ja0316', label: '임업인' }
);

const specialOptionMap = Object.fromEntries(specialOptions.map((option) => [option.code, option.label]));

function buildPageNumbers(currentPage, totalPages) {
    if (totalPages <= 0) {
        return [];
    }

    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages - 1, start + 4);
    const adjustedStart = Math.max(0, end - 4);

    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
}

function parsePage(value) {
    const parsed = Number.parseInt(value ?? '1', 10);
    return Number.isNaN(parsed) || parsed < 1 ? 1 : parsed;
}

function parseSpecial(value) {
    if (!value) {
        return [];
    }

    return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function stringifySpecial(values) {
    return values.join(',');
}

function resolveRegion(mainRegion, subRegion) {
    return subRegion || mainRegion || '';
}

function splitRegion(regionValue) {
    if (!regionValue) {
        return { mainRegion: '', subRegion: '' };
    }

    for (const region of topLevelRegions) {
        const cities = regionHierarchy[region];
        const matchedCity = cities.find((city) => regionValue.includes(city));
        if (matchedCity) {
            return { mainRegion: region, subRegion: matchedCity };
        }
    }

    return { mainRegion: regionValue, subRegion: '' };
}

const PolicyPage = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const urlPage = parsePage(searchParams.get('page'));
    const urlQuery = searchParams.get('query') ?? '';
    const urlType = searchParams.get('type') ?? '';
    const urlAge = searchParams.get('age') ?? '';
    const urlRegion = searchParams.get('region') ?? '';
    const urlSpecialValue = searchParams.get('special') ?? '';
    const urlSpecial = useMemo(() => parseSpecial(urlSpecialValue), [urlSpecialValue]);
    const parsedRegion = useMemo(() => splitRegion(urlRegion), [urlRegion]);
    const [queryInput, setQueryInput] = useState(urlQuery);
    const [selectedType, setSelectedType] = useState(urlType);
    const [ageInput, setAgeInput] = useState(urlAge);
    const [mainRegionInput, setMainRegionInput] = useState(parsedRegion.mainRegion);
    const [subRegionInput, setSubRegionInput] = useState(parsedRegion.subRegion);
    const [selectedSpecials, setSelectedSpecials] = useState(urlSpecial);
    const [specialSelectValue, setSpecialSelectValue] = useState('');
    const [currentPage, setCurrentPage] = useState(urlPage - 1);
    const [pageInput, setPageInput] = useState(String(urlPage));
    const [policies, setPolicies] = useState([]);
    const [typeCounts, setTypeCounts] = useState([]);
    const [pageInfo, setPageInfo] = useState({ page: 0, totalElements: 0, totalPages: 0, hasNext: false });
    const [isLoading, setIsLoading] = useState(true);
    const [isTypeLoading, setIsTypeLoading] = useState(true);
    const [error, setError] = useState('');
    const cityOptions = mainRegionInput ? regionHierarchy[mainRegionInput] ?? [] : [];

    useEffect(() => {
        setQueryInput(urlQuery);
        setSelectedType(urlType);
        setAgeInput(urlAge);
        setMainRegionInput(parsedRegion.mainRegion);
        setSubRegionInput(parsedRegion.subRegion);
        setSelectedSpecials(urlSpecial);
        setSpecialSelectValue('');
        setCurrentPage(urlPage - 1);
        setPageInput(String(urlPage));
    }, [parsedRegion.mainRegion, parsedRegion.subRegion, urlAge, urlPage, urlQuery, urlSpecial, urlType]);

    useEffect(() => {
        let cancelled = false;

        const loadPolicies = async () => {
            setIsLoading(true);
            setError('');

            try {
                const result = await fetchPolicies({
                    query: urlQuery,
                    type: urlType,
                    age: urlAge,
                    region: urlRegion,
                    special: stringifySpecial(urlSpecial),
                    page: currentPage,
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
                setPageInput(String((data?.page ?? 0) + 1));
            } catch (err) {
                if (!cancelled) {
                    setError(err.response?.data?.message || '정책 목록을 불러오지 못했습니다.');
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
    }, [currentPage, urlAge, urlQuery, urlRegion, urlSpecial, urlType]);

    useEffect(() => {
        let cancelled = false;

        const loadTypes = async () => {
            setIsTypeLoading(true);

            try {
                const result = await fetchPolicyTypes({
                    query: urlQuery,
                    age: urlAge,
                    region: urlRegion,
                    special: stringifySpecial(urlSpecial),
                });

                if (!cancelled) {
                    setTypeCounts(result?.data ?? []);
                }
            } catch {
                if (!cancelled) {
                    setTypeCounts([]);
                }
            } finally {
                if (!cancelled) {
                    setIsTypeLoading(false);
                }
            }
        };

        loadTypes();

        return () => {
            cancelled = true;
        };
    }, [urlAge, urlQuery, urlRegion, urlSpecial]);

    const pageNumbers = useMemo(() => buildPageNumbers(pageInfo.page, pageInfo.totalPages), [pageInfo.page, pageInfo.totalPages]);

    const updateUrl = ({ page = 0, query = '', type = '', age = '', region = '', special = [] }) => {
        const nextParams = new URLSearchParams();
        nextParams.set('page', String(page + 1));

        if (query) nextParams.set('query', query);
        if (type) nextParams.set('type', type);
        if (age) nextParams.set('age', age);
        if (region) nextParams.set('region', region);
        if (special.length > 0) nextParams.set('special', stringifySpecial(special));

        setSearchParams(nextParams);
    };

    const applyFilters = (nextPage = 0, nextType = selectedType) => {
        updateUrl({
            page: nextPage,
            query: queryInput.trim(),
            type: nextType,
            age: ageInput.trim(),
            region: resolveRegion(mainRegionInput, subRegionInput),
            special: selectedSpecials,
        });
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        applyFilters(0);
    };

    const handleTypeClick = (type) => {
        const nextType = type === selectedType ? '' : type;
        setSelectedType(nextType);
        updateUrl({
            page: 0,
            query: queryInput.trim(),
            type: nextType,
            age: ageInput.trim(),
            region: resolveRegion(mainRegionInput, subRegionInput),
            special: selectedSpecials,
        });
    };

    const handleMainRegionChange = (event) => {
        setMainRegionInput(event.target.value);
        setSubRegionInput('');
    };

    const handleAddSpecial = () => {
        if (!specialSelectValue || selectedSpecials.includes(specialSelectValue)) {
            return;
        }

        setSelectedSpecials((current) => [...current, specialSelectValue]);
        setSpecialSelectValue('');
    };

    const removeSpecial = (code) => {
        setSelectedSpecials((current) => current.filter((item) => item !== code));
    };

    const resetFilters = () => {
        setQueryInput('');
        setSelectedType('');
        setAgeInput('');
        setMainRegionInput('');
        setSubRegionInput('');
        setSelectedSpecials([]);
        setSpecialSelectValue('');
        updateUrl({ page: 0 });
    };

    const movePage = (nextPage) => {
        const boundedPage = Math.min(Math.max(nextPage, 0), Math.max(pageInfo.totalPages - 1, 0));
        updateUrl({
            page: boundedPage,
            query: urlQuery,
            type: urlType,
            age: urlAge,
            region: urlRegion,
            special: urlSpecial,
        });
    };

    const handlePageInputSubmit = () => {
        const parsedPage = parsePage(pageInput);
        movePage(parsedPage - 1);
    };

    const currentSearch = searchParams.toString();

    return (
        <div className="min-h-screen bg-[linear-gradient(180deg,#f8f9ff_0%,#eef4ff_22%,#f8fafc_100%)]">
            <div className="mx-auto flex max-w-7xl gap-8 px-4 py-10 lg:px-6">
                <aside className="hidden w-64 shrink-0 lg:block">
                    <div className="sticky top-24 overflow-hidden rounded-[28px] border border-white/60 bg-white/80 shadow-[0_18px_60px_rgba(92,118,255,0.12)] backdrop-blur">
                        <div className="bg-[linear-gradient(135deg,#efe8ff_0%,#e8f2ff_100%)] px-6 py-8">
                            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Policy</p>
                            <h1 className="mt-3 text-3xl font-black text-slate-900">지원유형별 정책</h1>
                            <p className="mt-2 text-sm leading-6 text-slate-600">DB 정책을 지역과 대상 조건까지 묶어서 바로 좁혀볼 수 있습니다.</p>
                        </div>
                        <nav className="space-y-1 px-4 py-5">
                            {sidebarMenus.map((menu, index) => (
                                <button
                                    key={menu}
                                    className={`flex w-full items-center justify-between rounded-2xl px-4 py-3 text-left text-sm font-semibold transition ${
                                        index === 0 ? 'bg-slate-900 text-white shadow-lg' : 'text-slate-600 hover:bg-slate-100'
                                    }`}
                                >
                                    <span>{menu}</span>
                                    <ChevronRight size={16} />
                                </button>
                            ))}
                        </nav>
                    </div>
                </aside>

                <main className="min-w-0 flex-1">
                    <section className="rounded-[32px] border border-white/70 bg-white/85 p-6 shadow-[0_20px_80px_rgba(98,124,255,0.12)] backdrop-blur lg:p-8">
                        <div className="flex flex-col gap-6">
                            <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
                                <div>
                                    <p className="text-sm font-semibold text-slate-500">정책 대상 기준 검색</p>
                                    <h2 className="mt-2 text-3xl font-black tracking-tight text-slate-950">조건에 맞는 정책만 빠르게 찾기</h2>
                                    <p className="mt-2 text-sm text-slate-500">나이, 지역, 특수 조건을 함께 적용해서 실제 대상 정책만 남기도록 했습니다.</p>
                                </div>
                                <button
                                    onClick={resetFilters}
                                    className="inline-flex items-center gap-2 self-start rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-white"
                                >
                                    <RefreshCcw size={16} />
                                    초기화
                                </button>
                            </div>

                            <form onSubmit={handleSubmit} className="space-y-5">
                                <div className="flex flex-col gap-3 lg:flex-row">
                                    <div className="relative flex-1">
                                        <Search className="pointer-events-none absolute left-5 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
                                        <input
                                            value={queryInput}
                                            onChange={(event) => setQueryInput(event.target.value)}
                                            placeholder="서비스명이나 기관명으로 검색"
                                            className="h-14 w-full rounded-full border border-[#b7c4ff] bg-white pl-14 pr-5 text-sm text-slate-900 outline-none transition focus:border-[#5f78ff] focus:ring-4 focus:ring-[#dfe6ff]"
                                        />
                                    </div>
                                    <input
                                        value={ageInput}
                                        onChange={(event) => setAgeInput(event.target.value.replace(/[^\d]/g, ''))}
                                        placeholder="나이"
                                        className="h-14 w-full rounded-full border border-slate-200 bg-white px-5 text-sm text-slate-900 outline-none transition focus:border-[#5f78ff] focus:ring-4 focus:ring-[#dfe6ff] lg:w-36"
                                    />
                                    <button
                                        type="submit"
                                        className="h-14 rounded-full bg-[linear-gradient(135deg,#5f78ff_0%,#7e7bff_100%)] px-7 text-sm font-bold text-white shadow-[0_12px_30px_rgba(95,120,255,0.28)] transition hover:translate-y-[-1px]"
                                    >
                                        필터 적용
                                    </button>
                                </div>

                                <div className="rounded-[28px] border border-slate-200 bg-[#f8faff] p-5">
                                    <div className="mb-3 flex items-center justify-between">
                                        <p className="text-sm font-semibold text-slate-700">지원유형 카테고리</p>
                                        {isTypeLoading && <span className="text-xs text-slate-400">집계 불러오는 중</span>}
                                    </div>
                                    <div className="flex flex-wrap gap-3">
                                        <button
                                            type="button"
                                            onClick={() => handleTypeClick('')}
                                            className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
                                                !selectedType
                                                    ? 'border-slate-900 bg-slate-900 text-white'
                                                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                                            }`}
                                        >
                                            전체
                                        </button>
                                        {typeCounts.map((item) => (
                                            <button
                                                key={item.type}
                                                type="button"
                                                onClick={() => handleTypeClick(item.type)}
                                                className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
                                                    selectedType === item.type
                                                        ? 'border-[#5f78ff] bg-[#eef2ff] text-[#4860e6]'
                                                        : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                                                }`}
                                            >
                                                {item.type} {item.count.toLocaleString()}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div className="grid gap-5 lg:grid-cols-2">
                                    <div className="rounded-[28px] border border-slate-200 bg-white p-5">
                                        <div className="flex items-center justify-between">
                                            <p className="text-sm font-semibold text-slate-700">지역 필터</p>
                                            {urlRegion && <span className="text-xs font-semibold text-[#4860e6]">{urlRegion}</span>}
                                        </div>
                                        <div className="mt-4 grid gap-3 md:grid-cols-2">
                                            <select
                                                value={mainRegionInput}
                                                onChange={handleMainRegionChange}
                                                className="h-12 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-[#5f78ff] focus:ring-4 focus:ring-[#dfe6ff]"
                                            >
                                                <option value="">수도권/광역시/도</option>
                                                {topLevelRegions.map((region) => (
                                                    <option key={region} value={region}>
                                                        {region}
                                                    </option>
                                                ))}
                                            </select>
                                            <select
                                                value={subRegionInput}
                                                onChange={(event) => setSubRegionInput(event.target.value)}
                                                disabled={cityOptions.length === 0}
                                                className="h-12 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-[#5f78ff] focus:ring-4 focus:ring-[#dfe6ff] disabled:bg-slate-100 disabled:text-slate-400"
                                            >
                                                <option value="">세부 시 선택</option>
                                                {cityOptions.map((city) => (
                                                    <option key={city} value={city}>
                                                        {city}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                        <p className="mt-3 text-xs leading-5 text-slate-400">서울, 부산 같은 광역시는 첫 번째 바만 선택하면 되고, 경기도 같은 도 단위는 두 번째 바에서 시까지 지정할 수 있습니다.</p>
                                    </div>

                                    <div className="rounded-[28px] border border-slate-200 bg-white p-5">
                                        <div className="flex items-center justify-between">
                                            <p className="text-sm font-semibold text-slate-700">특수 조건</p>
                                            <span className="text-xs text-slate-400">선택 후 추가</span>
                                        </div>
                                        <div className="mt-4 flex gap-3">
                                            <select
                                                value={specialSelectValue}
                                                onChange={(event) => setSpecialSelectValue(event.target.value)}
                                                className="h-12 flex-1 rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-[#5f78ff] focus:ring-4 focus:ring-[#dfe6ff]"
                                            >
                                                <option value="">특수 조건 선택</option>
                                                {specialOptions.map((option) => (
                                                    <option key={option.code} value={option.code}>
                                                        {option.label}
                                                    </option>
                                                ))}
                                            </select>
                                            <button
                                                type="button"
                                                onClick={handleAddSpecial}
                                                className="rounded-2xl bg-slate-900 px-4 text-sm font-bold text-white transition hover:bg-slate-800"
                                            >
                                                추가
                                            </button>
                                        </div>
                                        <div className="mt-4 flex min-h-12 flex-wrap gap-2">
                                            {selectedSpecials.length === 0 && <p className="text-sm text-slate-400">아직 선택한 조건이 없습니다.</p>}
                                            {selectedSpecials.map((code) => (
                                                <span
                                                    key={code}
                                                    className="inline-flex items-center gap-2 rounded-full border border-[#d6defd] bg-[#eef2ff] px-3 py-2 text-xs font-semibold text-[#4860e6]"
                                                >
                                                    {specialOptionMap[code] || code}
                                                    <button type="button" onClick={() => removeSpecial(code)} className="text-[#4860e6]">
                                                        <X size={14} />
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </section>

                    <section className="mt-8">
                        <div className="mb-5 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                            <div>
                                <p className="text-sm font-semibold text-slate-500">검색 결과</p>
                                <h3 className="mt-1 text-2xl font-black text-slate-950">총 {pageInfo.totalElements.toLocaleString()}개의 정책</h3>
                            </div>
                            <div className="flex flex-wrap gap-3 text-sm">
                                {selectedType && <div className="rounded-2xl border border-[#d6defd] bg-[#eef2ff] px-4 py-3 text-[#3550dc]">지원유형 {selectedType}</div>}
                                {urlAge && <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-600">나이 {urlAge}세</div>}
                                {urlRegion && <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-600">지역 {urlRegion}</div>}
                                {urlSpecial.length > 0 && <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-600">특수 조건 {urlSpecial.length}개</div>}
                                <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-600">페이지 {pageInfo.page + 1} / {Math.max(pageInfo.totalPages, 1)}</div>
                            </div>
                        </div>

                        {isLoading && <div className="rounded-[28px] border border-slate-200 bg-white p-10 text-center text-slate-500 shadow-sm">정책 목록을 불러오는 중입니다.</div>}
                        {!isLoading && error && <div className="rounded-[28px] border border-red-100 bg-red-50 p-10 text-center text-red-600 shadow-sm">{error}</div>}
                        {!isLoading && !error && policies.length === 0 && <div className="rounded-[28px] border border-slate-200 bg-white p-10 text-center text-slate-500 shadow-sm">조건에 맞는 정책이 없습니다.</div>}

                        {!isLoading && !error && policies.length > 0 && (
                            <>
                                <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
                                    {policies.map((policy) => (
                                        <article
                                            key={policy.serviceId}
                                            className="group rounded-[28px] border border-white/70 bg-white p-6 shadow-[0_16px_45px_rgba(15,23,42,0.06)] transition hover:-translate-y-1 hover:shadow-[0_24px_60px_rgba(95,120,255,0.18)]"
                                        >
                                            <div className="space-y-2">
                                                <div className="flex flex-wrap gap-2">
                                                    <span className="rounded-full bg-[#eef2ff] px-3 py-1 text-xs font-bold text-[#4860e6]">{policy.serviceType || '정책'}</span>
                                                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">{policy.applicationDeadline || '상시'}</span>
                                                </div>
                                                <h4 className="break-words text-lg font-black leading-7 text-slate-950">{policy.serviceName}</h4>
                                            </div>

                                            <p className="mt-4 h-20 overflow-hidden break-words text-sm leading-6 text-slate-600">{policy.purposeSummary || '정책 설명이 아직 정리되지 않았습니다.'}</p>

                                            <dl className="mt-5 space-y-2 text-sm">
                                                <div className="flex justify-between gap-4">
                                                    <dt className="text-slate-400">기관</dt>
                                                    <dd className="break-words text-right font-semibold text-slate-700">{policy.orgName || '기관 정보 없음'}</dd>
                                                </div>
                                                <div className="flex justify-between gap-4">
                                                    <dt className="text-slate-400">부서</dt>
                                                    <dd className="break-words text-right font-semibold text-slate-700">{policy.departmentName || '부서 정보 없음'}</dd>
                                                </div>
                                            </dl>

                                            <div className="mt-6 flex gap-3">
                                                <Link
                                                    to={`/policies/${policy.serviceId}${currentSearch ? `?${currentSearch}` : ''}`}
                                                    className="flex-1 rounded-2xl border border-[#9fb0ff] bg-white px-4 py-3 text-center text-sm font-bold text-[#4c63e6] transition hover:bg-[#eef2ff]"
                                                >
                                                    자세히 보기
                                                </Link>
                                            </div>
                                        </article>
                                    ))}
                                </div>
                                <div className="mt-8 flex flex-col items-center justify-center gap-4">
                                    <div className="flex items-center justify-center gap-2">
                                        <button
                                            onClick={() => movePage(pageInfo.page - 1)}
                                            disabled={pageInfo.page === 0}
                                            className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                                        >
                                            <ChevronLeft size={18} />
                                        </button>
                                        {pageNumbers.map((page) => (
                                            <button
                                                key={page}
                                                onClick={() => movePage(page)}
                                                className={`h-11 min-w-11 rounded-full px-4 text-sm font-bold transition ${
                                                    page === pageInfo.page
                                                        ? 'bg-slate-900 text-white shadow-lg'
                                                        : 'border border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50'
                                                }`}
                                            >
                                                {page + 1}
                                            </button>
                                        ))}
                                        <button
                                            onClick={() => movePage(pageInfo.page + 1)}
                                            disabled={!pageInfo.hasNext}
                                            className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                                        >
                                            <ChevronRight size={18} />
                                        </button>
                                    </div>

                                    <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2">
                                        <span className="text-sm text-slate-500">페이지 이동</span>
                                        <input
                                            value={pageInput}
                                            onChange={(event) => setPageInput(event.target.value.replace(/[^\d]/g, ''))}
                                            onKeyDown={(event) => {
                                                if (event.key === 'Enter') {
                                                    event.preventDefault();
                                                    handlePageInputSubmit();
                                                }
                                            }}
                                            className="h-9 w-20 rounded-full border border-slate-200 px-3 text-center text-sm font-semibold text-slate-700 outline-none focus:border-[#5f78ff]"
                                        />
                                        <span className="text-sm text-slate-500">/ {Math.max(pageInfo.totalPages, 1)}</span>
                                        <button
                                            onClick={handlePageInputSubmit}
                                            className="rounded-full bg-slate-900 px-4 py-2 text-sm font-bold text-white transition hover:bg-slate-800"
                                        >
                                            이동
                                        </button>
                                    </div>
                                </div>
                            </>
                        )}
                    </section>
                </main>
            </div>
        </div>
    );
};

export default PolicyPage;
