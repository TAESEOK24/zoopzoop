import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, Building2, CalendarDays, ExternalLink, FileText, Phone, User } from 'lucide-react';
import { fetchPolicyDetail } from '../../api/policies';

const sectionBaseClass = 'rounded-[28px] border border-white/70 bg-white/90 p-6 shadow-[0_14px_45px_rgba(15,23,42,0.06)]';

function formatDisplayText(value) {
    if (!value) {
        return '정보 없음';
    }

    return String(value)
        .replace(/;\s*/g, ';\n')
        .replace(/\|/g, '\n')
        .replace(/\s*○\s*/g, '\n○ ')
        .replace(/\s+-\s+/g, '\n- ')
        .trim();
}

function formatContactText(value) {
    if (!value) {
        return '정보 없음';
    }

    const normalized = String(value).trim();

    if (/^.+\s*\/\s*\d{2,4}-\d{3,4}-\d{4}$/.test(normalized)) {
        return normalized.replace(/\s*\/\s*/, ' / ');
    }

    const parts = normalized
        .split(/[;|,]/)
        .map((part) => part.trim())
        .filter(Boolean);

    const lines = [];
    let pendingLabel = null;

    for (const part of parts) {
        const isPhone = /(\d{2,4}-\d{3,4}-\d{4}|\d{3,4}-\d{4}|\d{8,})/.test(part);

        if (isPhone) {
            if (pendingLabel) {
                lines.push(`${pendingLabel} / ${part}`);
                pendingLabel = null;
            } else {
                lines.push(part);
            }
            continue;
        }

        if (pendingLabel) {
            lines.push(pendingLabel);
        }
        pendingLabel = part;
    }

    if (pendingLabel) {
        lines.push(pendingLabel);
    }

    return lines.join('\n');
}

function joinValues(values) {
    return values.filter((value) => value && String(value).trim()).join('\n');
}

function ageText(minAge, maxAge) {
    if (minAge == null && maxAge == null) {
        return '연령 조건 정보 없음';
    }
    if (minAge != null && maxAge != null) {
        return `만 ${minAge}세 ~ 만 ${maxAge}세`;
    }
    if (minAge != null) {
        return `만 ${minAge}세 이상`;
    }
    return `만 ${maxAge}세 이하`;
}

function conditionSections(conditions) {
    if (!conditions) {
        return [];
    }

    return [
        { title: '성별', values: conditions.gender || [] },
        { title: '소득 기준', values: conditions.income || [] },
        { title: '대상 조건', values: conditions.lifeStage || [] },
        { title: '가구 특성', values: conditions.household || [] },
        { title: '직업 및 사업', values: conditions.business || [] },
        { title: '기관 유형', values: conditions.organization || [] },
        { title: '특수 대상', values: conditions.specialStatus || [] },
    ].filter((section) => section.values.length > 0);
}

const PolicyDetailPage = () => {
    const { serviceId } = useParams();
    const location = useLocation();
    const [policy, setPolicy] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let cancelled = false;

        const loadDetail = async () => {
            setIsLoading(true);
            setError('');

            try {
                const result = await fetchPolicyDetail(serviceId);
                if (!cancelled) {
                    setPolicy(result?.data ?? null);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.response?.data?.message || '정책 상세 정보를 불러오지 못했습니다.');
                }
            } finally {
                if (!cancelled) {
                    setIsLoading(false);
                }
            }
        };

        loadDetail();

        return () => {
            cancelled = true;
        };
    }, [serviceId]);

    const requirementText = useMemo(
        () => joinValues([policy?.requiredDocuments, policy?.officialRequiredDocs, policy?.userRequiredDocs]),
        [policy]
    );
    const conditions = useMemo(() => conditionSections(policy?.conditions), [policy]);
    const backLink = `/policies${location.search || '?page=1'}`;

    if (isLoading) {
        return <div className="min-h-screen bg-slate-50 p-10 text-center text-slate-500">정책 상세 정보를 불러오는 중입니다.</div>;
    }

    if (error || !policy) {
        return (
            <div className="min-h-screen bg-slate-50 p-10">
                <div className="mx-auto max-w-4xl rounded-[28px] border border-red-100 bg-red-50 p-10 text-center text-red-600 shadow-sm">
                    {error || '정책 정보를 찾을 수 없습니다.'}
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[linear-gradient(180deg,#f8f9ff_0%,#eef4ff_22%,#f8fafc_100%)]">
            <div className="mx-auto max-w-6xl px-4 py-10 lg:px-6">
                <Link to={backLink} className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-slate-900">
                    <ArrowLeft size={18} />
                    정책 목록으로 돌아가기
                </Link>

                <section className="overflow-hidden rounded-[32px] border border-white/70 bg-white/90 shadow-[0_20px_80px_rgba(98,124,255,0.12)]">
                    <div className="bg-[linear-gradient(135deg,#eef2ff_0%,#f6f0ff_100%)] p-8 lg:p-10">
                        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
                            <div className="max-w-3xl">
                                <div className="flex flex-wrap gap-2">
                                    <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-bold text-[#4860e6]">{policy.serviceType || '정책'}</span>
                                    <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold text-slate-600 whitespace-pre-line break-words">
                                        {formatDisplayText(policy.applicationDeadline || '상시')}
                                    </span>
                                </div>
                                <h1 className="mt-4 break-words text-3xl font-black leading-tight text-slate-950 lg:text-4xl">{policy.serviceName}</h1>
                                <p className="mt-4 whitespace-pre-line break-words text-sm leading-7 text-slate-600">
                                    {formatDisplayText(policy.purpose || policy.purposeSummary || '정책 소개 정보가 아직 정리되지 않았습니다.')}
                                </p>
                            </div>

                            <div className="grid shrink-0 grid-cols-1 gap-3 sm:grid-cols-2">
                                <div className="rounded-2xl border border-white/70 bg-white/70 px-4 py-4">
                                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">조회수</p>
                                    <p className="mt-2 text-xl font-black text-slate-900">{(policy.viewCount || 0).toLocaleString()}</p>
                                </div>
                                <div className="rounded-2xl border border-white/70 bg-white/70 px-4 py-4">
                                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">연령 조건</p>
                                    <p className="mt-2 text-xl font-black text-slate-900">{ageText(policy.conditions?.minAge, policy.conditions?.maxAge)}</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="grid gap-6 p-6 lg:grid-cols-[1.5fr_0.9fr] lg:p-8">
                        <div className="space-y-6">
                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">정책 개요</h2>
                                <div className="mt-5 grid gap-4 md:grid-cols-2">
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">지원 대상</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.target)}</p>
                                    </div>
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">선정 기준</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.selectionCriteria)}</p>
                                    </div>
                                    <div className="rounded-2xl bg-slate-50 p-4 md:col-span-2">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">지원 내용</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.supportContent)}</p>
                                    </div>
                                </div>
                            </section>

                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">지원 조건</h2>
                                <div className="mt-5 grid gap-4 md:grid-cols-2">
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">연령</p>
                                        <p className="mt-2 text-sm leading-6 text-slate-700">{ageText(policy.conditions?.minAge, policy.conditions?.maxAge)}</p>
                                    </div>
                                    {conditions.map((section) => (
                                        <div key={section.title} className="rounded-2xl bg-slate-50 p-4">
                                            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{section.title}</p>
                                            <div className="mt-3 flex flex-wrap gap-2">
                                                {section.values.map((value) => (
                                                    <span key={value} className="rounded-full border border-[#d5ddff] bg-white px-3 py-1 text-xs font-semibold text-[#4860e6]">
                                                        {value}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                    {conditions.length === 0 && (
                                        <div className="rounded-2xl bg-slate-50 p-4 md:col-span-2">
                                            <p className="text-sm leading-6 text-slate-700">조건 정보가 별도로 정리되어 있지 않습니다.</p>
                                        </div>
                                    )}
                                </div>
                            </section>

                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">신청 방법</h2>
                                <div className="mt-5 space-y-4">
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">신청 절차</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.applicationMethod)}</p>
                                    </div>
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">제출 서류</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(requirementText)}</p>
                                    </div>
                                </div>
                            </section>

                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">관련 규정</h2>
                                <div className="mt-5 space-y-4">
                                    <div className="rounded-2xl bg-slate-50 p-4">
                                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">법령</p>
                                        <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.law)}</p>
                                    </div>
                                    <div className="grid gap-4 md:grid-cols-2">
                                        <div className="rounded-2xl bg-slate-50 p-4">
                                            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">행정 규칙</p>
                                            <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.adminRule)}</p>
                                        </div>
                                        <div className="rounded-2xl bg-slate-50 p-4">
                                            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">자치 법규</p>
                                            <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">{formatDisplayText(policy.localRule)}</p>
                                        </div>
                                    </div>
                                </div>
                            </section>
                        </div>

                        <aside className="space-y-6">
                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">기본 정보</h2>
                                <div className="mt-5 space-y-4 text-sm">
                                    <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4">
                                        <Building2 className="mt-0.5 text-[#4860e6]" size={18} />
                                        <div className="min-w-0">
                                            <p className="font-semibold text-slate-900">소관 기관</p>
                                            <p className="mt-1 whitespace-pre-line break-words text-slate-600">{formatDisplayText(policy.orgName)}</p>
                                            <p className="mt-1 whitespace-pre-line break-words text-slate-500">{formatDisplayText(policy.departmentName || '부서 정보 없음')}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4">
                                        <CalendarDays className="mt-0.5 text-[#4860e6]" size={18} />
                                        <div className="min-w-0">
                                            <p className="font-semibold text-slate-900">신청 기한</p>
                                            <p className="mt-1 whitespace-pre-line break-words text-slate-600">{formatDisplayText(policy.applicationDeadline || '상시')}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4">
                                        <Phone className="mt-0.5 text-[#4860e6]" size={18} />
                                        <div className="min-w-0">
                                            <p className="font-semibold text-slate-900">문의처</p>
                                            <p className="mt-1 whitespace-pre-line break-words text-slate-600">{formatContactText(policy.contactInfo || policy.contactNumber)}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-start gap-3 rounded-2xl bg-slate-50 p-4">
                                        <User className="mt-0.5 text-[#4860e6]" size={18} />
                                        <div className="min-w-0">
                                            <p className="font-semibold text-slate-900">접수 기관</p>
                                            <p className="mt-1 whitespace-pre-line break-words text-slate-600">{formatContactText(policy.receivingOrgName || policy.receivingOrg)}</p>
                                        </div>
                                    </div>
                                </div>
                            </section>

                            <section className={sectionBaseClass}>
                                <h2 className="text-xl font-black text-slate-950">바로가기</h2>
                                <div className="mt-5 space-y-3">
                                    {policy.onlineUrl && (
                                        <a
                                            href={policy.onlineUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="flex items-center justify-between rounded-2xl border border-[#cfd8ff] bg-[#eef2ff] px-4 py-4 text-sm font-semibold text-[#4860e6]"
                                        >
                                            온라인 신청 페이지
                                            <ExternalLink size={16} />
                                        </a>
                                    )}
                                    {policy.detailUrl && (
                                        <a
                                            href={policy.detailUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-semibold text-slate-700"
                                        >
                                            원본 상세 정보
                                            <ExternalLink size={16} />
                                        </a>
                                    )}
                                    {!policy.onlineUrl && !policy.detailUrl && (
                                        <div className="rounded-2xl bg-slate-50 px-4 py-4 text-sm text-slate-500">외부 연결 정보가 없습니다.</div>
                                    )}
                                </div>
                            </section>

                            <section className={sectionBaseClass}>
                                <div className="flex items-start gap-3">
                                    <FileText className="mt-0.5 text-[#4860e6]" size={18} />
                                    <div>
                                        <h2 className="text-xl font-black text-slate-950">안내</h2>
                                        <p className="mt-2 text-sm leading-6 text-slate-600">
                                            현재 화면은 우리 DB에 적재된 정책 정보를 기반으로 표시합니다. 외부 사이트로 바로 이동시키지 않고, 필요한 경우에만 추가 링크를 제공합니다.
                                        </p>
                                    </div>
                                </div>
                            </section>
                        </aside>
                    </div>
                </section>
            </div>
        </div>
    );
};

export default PolicyDetailPage;
